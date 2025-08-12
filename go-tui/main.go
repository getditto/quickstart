package main

import (
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	"github.com/getditto/ditto-go-sdk/ditto"
	ui "github.com/gizak/termui/v3"
	"github.com/gizak/termui/v3/widgets"
	"github.com/google/uuid"
	"github.com/joho/godotenv"
)

type Task struct {
	ID      string `json:"_id"`
	Title   string `json:"title"`
	Done    bool   `json:"done"`
	Deleted bool   `json:"deleted"`
}

type InputMode int

const (
	NormalMode InputMode = iota
	CreateMode
	EditMode
)

type App struct {
	ditto        *ditto.Ditto
	observer     *ditto.StoreObserver
	subscription *ditto.SyncSubscription
	tasks        []Task
	selectedIdx  int
	inputMode    InputMode
	inputBuffer  string
	editingID    string
	tasksChan    chan []Task
	errorMsg     string
	mu           sync.RWMutex

	// UI widgets
	taskTable *widgets.Table
	inputBox  *widgets.Paragraph
	statusBar *widgets.Paragraph
	errorBar  *widgets.Paragraph
}

func main() {
	// Suppress Ditto logs
	os.Setenv("RUST_LOG", "warn")
	os.Setenv("RUST_BACKTRACE", "0")

	// Load environment variables
	if err := loadEnv(); err != nil {
		log.Printf("Warning: Could not load .env file: %v", err)
	}

	// Get config from environment
	appID := os.Getenv("DITTO_APP_ID")
	token := os.Getenv("DITTO_PLAYGROUND_TOKEN")
	authURL := os.Getenv("DITTO_AUTH_URL")
	websocketURL := os.Getenv("DITTO_WEBSOCKET_URL")

	if appID == "" || token == "" || authURL == "" || websocketURL == "" {
		log.Fatal("Missing required environment variables. Please set DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN, DITTO_AUTH_URL, and DITTO_WEBSOCKET_URL")
	}

	// Create temp directory for persistence
	tempDir, err := os.MkdirTemp("", "ditto-quickstart-*")
	if err != nil {
		log.Fatal(err)
	}
	defer os.RemoveAll(tempDir)

	// Initialize Ditto with config-based API
	config := &ditto.Config{
		DatabaseID:           appID,
		PersistenceDirectory: tempDir,
		Connect: &ditto.OnlinePlaygroundConnect{
			AppID:         appID,
			Token:         token,
			CustomAuthURL: authURL,
		},
	}

	d, err := ditto.Open(config)
	if err != nil {
		log.Fatal("Failed to open Ditto:", err)
	}
	defer d.Close()

	// Configure transport
	err = d.UpdateTransportConfig(func(tc *ditto.TransportConfig) {
		tc.PeerToPeer.BluetoothLE.Enabled = true
		tc.PeerToPeer.LAN.Enabled = true
		tc.PeerToPeer.LAN.MDNSEnabled = true
		tc.PeerToPeer.LAN.MulticastEnabled = true
		tc.SetWebsocketURLs([]string{websocketURL})
	})
	if err != nil {
		log.Fatal("Failed to configure transport:", err)
	}

	// Disable sync with v3 peers (required for DQL)
	if err := d.DisableSyncWithV3(); err != nil {
		log.Printf("Warning: Failed to disable sync with v3: %v", err)
	}

	// Start sync
	if err := d.StartSync(); err != nil {
		log.Fatal("Failed to start sync:", err)
	}

	// Initialize termui
	if err := ui.Init(); err != nil {
		log.Fatalf("failed to initialize termui: %v", err)
	}
	defer ui.Close()

	// Create app
	app := NewApp(d)

	// Create subscription for syncing
	subscription, err := d.Sync().RegisterSubscription("SELECT * FROM tasks")
	if err != nil {
		log.Fatal("Failed to register subscription:", err)
	}
	app.subscription = subscription

	// Create observer for local changes
	observer, err := d.Store().RegisterObserver(
		"SELECT * FROM tasks WHERE deleted = false ORDER BY _id",
		func(result *ditto.QueryResult) {
			tasks := parseTasks(result)
			app.tasksChan <- tasks
		},
	)
	if err != nil {
		log.Fatal("Failed to register observer:", err)
	}
	app.observer = observer

	// Force an initial query
	go func() {
		time.Sleep(200 * time.Millisecond)
		result, err := d.Store().Execute("SELECT * FROM tasks WHERE deleted = false ORDER BY _id")
		if err == nil && result != nil {
			tasks := parseTasks(result)
			app.tasksChan <- tasks
		}
	}()

	// Run the app
	app.Run()
}

func NewApp(d *ditto.Ditto) *App {
	app := &App{
		ditto:       d,
		tasks:       []Task{},
		selectedIdx: 0,
		inputMode:   NormalMode,
		tasksChan:   make(chan []Task, 10),
	}

	// Create widgets
	app.taskTable = widgets.NewTable()
	app.taskTable.Title = " Tasks (j↓, k↑, ⏎ toggle done) "
	app.taskTable.BorderStyle = ui.NewStyle(ui.ColorCyan)
	app.taskTable.RowSeparator = false
	app.taskTable.FillRow = true
	app.taskTable.RowStyles[0] = ui.NewStyle(ui.ColorWhite, ui.ColorClear, ui.ModifierBold)

	app.inputBox = widgets.NewParagraph()
	app.inputBox.Title = " New Task "
	app.inputBox.BorderStyle = ui.NewStyle(ui.ColorMagenta)

	app.statusBar = widgets.NewParagraph()
	app.statusBar.Border = false
	app.statusBar.Text = "[c](fg:yellow): create  [e](fg:yellow): edit  [d](fg:yellow): delete  [q](fg:yellow): quit  [s](fg:yellow): toggle sync"

	app.errorBar = widgets.NewParagraph()
	app.errorBar.Border = false
	app.errorBar.TextStyle = ui.NewStyle(ui.ColorRed)

	return app
}

func (a *App) Run() {
	// Initial render
	a.render()

	// Create event polling channel
	uiEvents := ui.PollEvents()

	// Main event loop
	for {
		select {
		case e := <-uiEvents:
			switch e.ID {
			case "q", "<C-c>":
				if a.inputMode == NormalMode {
					return
				}
			case "<Escape>":
				if a.inputMode != NormalMode {
					a.inputMode = NormalMode
					a.inputBuffer = ""
					a.editingID = ""
					a.render()
				}
			default:
				a.handleEvent(e)
			}

		case tasks := <-a.tasksChan:
			a.mu.Lock()
			a.tasks = tasks
			if a.selectedIdx >= len(a.tasks) && len(a.tasks) > 0 {
				a.selectedIdx = len(a.tasks) - 1
			}
			a.mu.Unlock()
			a.render()
		}
	}
}

func (a *App) handleEvent(e ui.Event) {
	switch a.inputMode {
	case NormalMode:
		a.handleNormalMode(e)
	case CreateMode:
		a.handleInputMode(e, false)
	case EditMode:
		a.handleInputMode(e, true)
	}
}

func (a *App) handleNormalMode(e ui.Event) {
	switch e.ID {
	case "j", "<Down>":
		a.mu.Lock()
		if a.selectedIdx < len(a.tasks)-1 {
			a.selectedIdx++
		}
		a.mu.Unlock()
		a.render()

	case "k", "<Up>":
		a.mu.Lock()
		if a.selectedIdx > 0 {
			a.selectedIdx--
		}
		a.mu.Unlock()
		a.render()

	case "<Enter>", " ":
		a.mu.RLock()
		if a.selectedIdx < len(a.tasks) {
			task := a.tasks[a.selectedIdx]
			a.mu.RUnlock()
			go a.toggleTask(task.ID, !task.Done)
		} else {
			a.mu.RUnlock()
		}

	case "c":
		a.inputMode = CreateMode
		a.inputBuffer = ""
		a.render()

	case "e":
		a.mu.RLock()
		if a.selectedIdx < len(a.tasks) {
			task := a.tasks[a.selectedIdx]
			a.inputMode = EditMode
			a.inputBuffer = task.Title
			a.editingID = task.ID
		}
		a.mu.RUnlock()
		a.render()

	case "d":
		a.mu.RLock()
		if a.selectedIdx < len(a.tasks) {
			task := a.tasks[a.selectedIdx]
			a.mu.RUnlock()
			go a.deleteTask(task.ID)
		} else {
			a.mu.RUnlock()
		}

	case "s":
		// Toggle sync (placeholder for now - could implement sync toggle)
		a.setError("Sync toggle not yet implemented")
		a.render()
	}
}

func (a *App) handleInputMode(e ui.Event, isEdit bool) {
	switch e.ID {
	case "<Enter>":
		if strings.TrimSpace(a.inputBuffer) != "" {
			if isEdit {
				go a.updateTask(a.editingID, a.inputBuffer)
			} else {
				go a.createTask(a.inputBuffer)
			}
			a.inputMode = NormalMode
			a.inputBuffer = ""
			a.editingID = ""
			a.render()
		}

	case "<Backspace>":
		if len(a.inputBuffer) > 0 {
			a.inputBuffer = a.inputBuffer[:len(a.inputBuffer)-1]
			a.render()
		}

	case "<Space>":
		a.inputBuffer += " "
		a.render()

	default:
		// Handle regular character input
		if len(e.ID) == 1 {
			a.inputBuffer += e.ID
			a.render()
		}
	}
}

func (a *App) render() {
	termWidth, termHeight := ui.TerminalDimensions()

	// Clear screen
	ui.Clear()

	// Update table data
	a.updateTable()

	// Layout calculations
	tableHeight := termHeight - 3 // Leave room for status bar
	if a.inputMode != NormalMode {
		tableHeight = termHeight - 8 // Make room for input box
	}

	// Set widget positions
	a.taskTable.SetRect(0, 0, termWidth, tableHeight)
	a.statusBar.SetRect(0, termHeight-2, termWidth, termHeight)

	// Render main widgets
	ui.Render(a.taskTable, a.statusBar)

	// Render input box if in input mode
	if a.inputMode != NormalMode {
		title := " New Task "
		if a.inputMode == EditMode {
			title = " Edit Task "
		}
		a.inputBox.Title = title
		a.inputBox.Text = a.inputBuffer + "█" // Add cursor

		// Center the input box
		boxWidth := termWidth - 10
		if boxWidth > 60 {
			boxWidth = 60
		}
		boxHeight := 3
		boxX := (termWidth - boxWidth) / 2
		boxY := (termHeight - boxHeight) / 2

		a.inputBox.SetRect(boxX, boxY, boxX+boxWidth, boxY+boxHeight)
		ui.Render(a.inputBox)
	}

	// Render error if present
	if a.errorMsg != "" {
		a.errorBar.Text = fmt.Sprintf("Error: %s", a.errorMsg)
		a.errorBar.SetRect(0, termHeight-3, termWidth, termHeight-2)
		ui.Render(a.errorBar)

		// Clear error after 3 seconds
		go func() {
			time.Sleep(3 * time.Second)
			a.mu.Lock()
			a.errorMsg = ""
			a.mu.Unlock()
			a.render()
		}()
	}
}

func (a *App) updateTable() {
	a.mu.RLock()
	defer a.mu.RUnlock()

	// Headers
	headers := []string{"", "Done", "Title"}

	// Rows
	rows := [][]string{headers}
	for i, task := range a.tasks {
		selector := "  "
		if i == a.selectedIdx {
			selector = "❯❯"
		}

		done := "☐"
		if task.Done {
			done = "✅"
		}

		rows = append(rows, []string{selector, done, task.Title})
	}

	if len(rows) == 1 {
		rows = append(rows, []string{"", "", "No tasks yet. Press 'c' to create one!"})
	}

	a.taskTable.Rows = rows

	// Highlight selected row
	if a.selectedIdx >= 0 && a.selectedIdx < len(a.tasks) {
		a.taskTable.RowStyles[a.selectedIdx+1] = ui.NewStyle(ui.ColorBlue, ui.ColorClear, ui.ModifierBold)
	}
}

func (a *App) createTask(title string) {
	task := map[string]interface{}{
		"_id":     uuid.New().String(),
		"title":   title,
		"done":    false,
		"deleted": false,
	}

	_, err := a.ditto.Store().Execute(
		"INSERT INTO tasks VALUES (:task)",
		map[string]interface{}{"task": task},
	)
	if err != nil {
		a.setError(err.Error())
	}
}

func (a *App) updateTask(id, title string) {
	_, err := a.ditto.Store().Execute(
		"UPDATE tasks SET title = :title WHERE _id = :id",
		map[string]interface{}{
			"title": title,
			"id":    id,
		},
	)
	if err != nil {
		a.setError(err.Error())
	}
}

func (a *App) toggleTask(id string, done bool) {
	_, err := a.ditto.Store().Execute(
		"UPDATE tasks SET done = :done WHERE _id = :id",
		map[string]interface{}{
			"done": done,
			"id":   id,
		},
	)
	if err != nil {
		a.setError(err.Error())
	}
}

func (a *App) deleteTask(id string) {
	_, err := a.ditto.Store().Execute(
		"UPDATE tasks SET deleted = true WHERE _id = :id",
		map[string]interface{}{"id": id},
	)
	if err != nil {
		a.setError(err.Error())
	}
}

func (a *App) setError(msg string) {
	a.mu.Lock()
	a.errorMsg = msg
	a.mu.Unlock()
}

func loadEnv() error {
	// Try to find .env file in parent directories
	dir, _ := os.Getwd()
	for {
		envPath := filepath.Join(dir, ".env")
		if _, err := os.Stat(envPath); err == nil {
			return godotenv.Load(envPath)
		}
		parent := filepath.Dir(dir)
		if parent == dir {
			break
		}
		dir = parent
	}
	return fmt.Errorf(".env file not found")
}

func parseTasks(result *ditto.QueryResult) []Task {
	if result == nil {
		return []Task{}
	}

	tasks := make([]Task, 0, result.ItemCount())
	for i := 0; i < result.ItemCount(); i++ {
		queryItem, err := result.GetItem(i)
		if err != nil {
			continue
		}

		// Get the value as a map
		// Value is already typed as map[string]interface{} in the struct
		if queryItem == nil || queryItem.Value == nil {
			continue
		}
		item := queryItem.Value

		// Parse the task from the document
		task := Task{
			ID:      getString(item, "_id"),
			Title:   getString(item, "title"),
			Done:    getBool(item, "done"),
			Deleted: getBool(item, "deleted"),
		}
		if !task.Deleted {
			tasks = append(tasks, task)
		}
	}
	return tasks
}

func getString(m map[string]interface{}, key string) string {
	if v, ok := m[key].(string); ok {
		return v
	}
	return ""
}

func getBool(m map[string]interface{}, key string) bool {
	if v, ok := m[key].(bool); ok {
		return v
	}
	return false
}
