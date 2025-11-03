package main

import (
	"context"
	"fmt"
	"log"
	"os"
	"path/filepath"
	"strings"
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

	tasks       []Task
	tasksChan   chan []Task
	selectedIdx int

	inputMode   InputMode
	inputBuffer string
	editingID   string

	errorMsg struct {
		value string // Owned by the app goroutine
		ch    chan string
		reset <-chan time.Time
	}

	ctx    context.Context
	cancel context.CancelFunc

	// UI widgets
	taskTable *widgets.Table
	inputBox  *widgets.Paragraph
	statusBar *widgets.Paragraph
	errorBar  *widgets.Paragraph
}

func main() {
	// Platform-specific stderr redirection (Unix: /dev/null, Windows: no-op for now)
	redirectStderr()

	// Also redirect Go's log output to a file for debugging
	logPath := filepath.Join(os.TempDir(), "ditto-tasks-termui.log")
	logFile, err := os.OpenFile(logPath, os.O_CREATE|os.O_WRONLY|os.O_APPEND, 0666)
	if err == nil {
		log.SetOutput(logFile)
		defer logFile.Close()
	} else {
		log.Printf("Failed to open log file: %v", err)
	}

	// Set Ditto log level to Error to suppress most logs
	// ditto.SetMinimumLogLevel(ditto.LogLevelError) // Commented out - causing segfault

	// Load environment variables
	if err := loadEnv(); err != nil {
		log.Printf("Warning: Could not load .env file: %v", err)
	}

	// Get config from environment
	appID := os.Getenv("DITTO_APP_ID")
	token := os.Getenv("DITTO_PLAYGROUND_TOKEN")
	authURL := os.Getenv("DITTO_AUTH_URL")

	if appID == "" || token == "" || authURL == "" {
		log.Fatal("Missing required environment variables. Please set DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN, and DITTO_AUTH_URL")
	}

	// Create temp directory for persistence
	tempDir, err := os.MkdirTemp("", "ditto-quickstart-*")
	if err != nil {
		log.Fatal(err)
	}
	defer os.RemoveAll(tempDir)

	// Initialize Ditto with Server connection API
	config := ditto.DefaultDittoConfig().
		WithDatabaseID(appID).
		WithPersistenceDirectory(tempDir).
		WithConnect(&ditto.DittoConfigConnectServer{URL: authURL})

	d, err := ditto.Open(config)
	if err != nil {
		log.Fatal("Failed to open Ditto:", err)
	}
	defer d.Close()

	// Set up authentication handler for development mode
	if auth := d.Auth(); auth != nil {
		auth.SetExpirationHandler(
			func(d *ditto.Ditto, timeUntilExpiration time.Duration) {
				log.Printf("Expiration handler called with time until expiration: %v", timeUntilExpiration)

				// For development mode, login with the playground token
				provider := ditto.DevelopmentAuthenticationProvider()
				clientInfoJSON, err := d.Auth().Login(token, provider)
				if err != nil {
					log.Printf("Failed to login: %v", err)
				} else {
					log.Printf("Login successful")
					if clientInfoJSON != "" {
						log.Printf("Client info: %s", clientInfoJSON)
					}
				}
			})
	}

	// Start sync (authentication handler will be called automatically if needed)
	if err := d.Sync().Start(); err != nil {
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
	defer subscription.Cancel()
	app.subscription = subscription

	// Create observer for local changes
	observer, err := d.Store().RegisterObserver(
		"SELECT * FROM tasks WHERE deleted = false ORDER BY _id",
		nil,
		func(result *ditto.QueryResult) {
			defer result.Close()

			tasks := parseTasks(result)
			select {
			case app.tasksChan <- tasks:
			case <-app.ctx.Done():
			}
		})
	if err != nil {
		log.Fatal("Failed to register observer:", err)
	}
	defer observer.Cancel()
	app.observer = observer

	// Run the app
	app.Run()
}

func NewApp(d *ditto.Ditto) *App {
	ctx, cancel := context.WithCancel(context.Background())
	app := &App{
		ditto:       d,
		tasks:       []Task{},
		selectedIdx: 0,
		inputMode:   NormalMode,
		tasksChan:   make(chan []Task, 1), // Buffer size 1 - latest update wins
		ctx:         ctx,
		cancel:      cancel,
	}

	app.errorMsg.ch = make(chan string, 1)

	// Create widgets
	app.taskTable = widgets.NewTable()
	app.taskTable.Title = " Tasks (j↓, k↑, ⏎ toggle done) "
	app.taskTable.BorderStyle = ui.NewStyle(ui.ColorCyan)
	app.taskTable.RowSeparator = false
	app.taskTable.FillRow = true
	app.taskTable.TextStyle = ui.NewStyle(ui.ColorWhite, ui.ColorClear, ui.ModifierBold)

	app.inputBox = widgets.NewParagraph()
	app.inputBox.Title = " New Task "
	app.inputBox.BorderStyle = ui.NewStyle(ui.ColorMagenta)

	app.statusBar = BorderlessParagraph()
	app.statusBar.Text = "[c](fg:yellow): create  [e](fg:yellow): edit  [d](fg:yellow): delete  [q](fg:yellow): quit  [s](fg:yellow): toggle sync"

	app.errorBar = BorderlessParagraph()
	app.errorBar.TextStyle = ui.NewStyle(ui.ColorRed)

	return app
}

func (a *App) Run() {
	defer a.cancel() // Ensure context is canceled when Run exits

	// Initial render
	a.render()

	// Create event polling channel
	uiEvents := ui.PollEvents()

	// Main event loop
	for {
		select {
		case <-a.ctx.Done():
			return
		case e := <-uiEvents:
			a.handleEvent(e)

		case tasks := <-a.tasksChan:
			a.updateTasks(tasks)
			a.render()
		case msg := <-a.errorMsg.ch:
			a.errorMsg.value = msg
			a.render()
		case <-a.errorMsg.reset:
			a.errorMsg.value = ""
			a.render()
		}
	}
}

func (a *App) handleEvent(e ui.Event) {
	if e.ID == "<Resize>" {
		a.render()
		return
	}
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
	case "q", "<C-c>":
		a.cancel() // signal main event loop to exit
	case "j", "<Down>":
		if a.selectedIdx < len(a.tasks)-1 {
			a.selectedIdx++
		}
		a.render()

	case "k", "<Up>":
		if a.selectedIdx > 0 {
			a.selectedIdx--
		}
		a.render()

	case "<Enter>", " ":
		if task, ok := a.getSelectedTask(); ok {
			go a.toggleTask(task.ID, !task.Done)
		}

	case "c":
		a.inputMode = CreateMode
		a.inputBuffer = ""
		a.render()

	case "e":
		if task, ok := a.getSelectedTask(); ok {
			a.inputMode = EditMode
			a.inputBuffer = task.Title
			a.editingID = task.ID
			a.render()
		}

	case "d":
		if task, ok := a.getSelectedTask(); ok {
			go a.deleteTask(task.ID)
		}

	case "s":
		// Toggle sync (placeholder for now - could implement sync toggle)
		a.setError("Sync toggle not yet implemented")
		a.render()
	}
}

func (a *App) handleInputMode(e ui.Event, isEdit bool) {
	switch e.ID {
	case "<Escape>":
		a.inputMode = NormalMode
		a.inputBuffer = ""
		a.editingID = ""
		a.render()
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
	tableHeight := termHeight - 2 // Leave room for status bar

	// Set widget positions
	a.taskTable.SetRect(0, 0, termWidth, tableHeight)
	a.statusBar.SetRect(0, termHeight-1, termWidth, termHeight)

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
		boxHeight := 8
		boxX := (termWidth - boxWidth) / 2
		boxY := (termHeight - boxHeight) / 2

		a.inputBox.SetRect(boxX, boxY, boxX+boxWidth, boxY+boxHeight)
		ui.Render(a.inputBox)
	}

	// Render error if present
	if a.errorMsg.value != "" {
		a.errorBar.Text = fmt.Sprintf("Error: %s", a.errorMsg.value)
		a.errorBar.SetRect(0, termHeight-2, termWidth, termHeight-1)
		ui.Render(a.errorBar)

		// Clear error after 3 seconds using the reset channel
		a.errorMsg.reset = time.After(3 * time.Second)
	}
}

func (a *App) updateTable() {
	// Headers
	headers := []string{"", "Done", "Title"}

	// Rows
	rows := [][]string{headers}
	for i, task := range a.tasks {
		selector := "  "
		if i == a.selectedIdx {
			selector = "❯❯"
		}

		done := " ☐"
		if task.Done {
			done = " ✓"
		}

		rows = append(rows, []string{selector, done, task.Title})
	}

	if len(rows) == 1 {
		rows = append(rows, []string{"", "", "No tasks yet. Press 'c' to create one!"})
	}

	a.taskTable.Rows = rows
	a.taskTable.ColumnWidths = []int{2, 5, a.taskTable.Dx() - 7}

	// Highlight selected row
	a.taskTable.RowStyles = map[int]ui.Style{} // clear existing highlight(s)
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

	result, err := a.ditto.Store().Execute(
		"INSERT INTO tasks VALUES (:task)",
		map[string]interface{}{"task": task},
	)
	if err != nil {
		a.setError(err.Error())
		return
	}
	defer result.Close()
}

func (a *App) updateTask(id, title string) {
	result, err := a.ditto.Store().Execute(
		"UPDATE tasks SET title = :title WHERE _id = :id",
		map[string]interface{}{
			"title": title,
			"id":    id,
		},
	)
	if err != nil {
		a.setError(err.Error())
		return
	}
	defer result.Close()
}

func (a *App) toggleTask(id string, done bool) {
	result, err := a.ditto.Store().Execute(
		"UPDATE tasks SET done = :done WHERE _id = :id",
		map[string]interface{}{
			"done": done,
			"id":   id,
		},
	)
	if err != nil {
		a.setError(err.Error())
		return
	}
	defer result.Close()
}

func (a *App) deleteTask(id string) {
	result, err := a.ditto.Store().Execute(
		"UPDATE tasks SET deleted = true WHERE _id = :id",
		map[string]interface{}{"id": id},
	)
	if err != nil {
		a.setError(err.Error())
		return
	}
	defer result.Close()
}

// Set the UI error message. May be called by any goroutine.
func (a *App) setError(msg string) {
	a.errorMsg.ch <- msg
}

func loadEnv() error {
	// Try to find .env file in parent directories
	dir, _ := os.Getwd()
	for {
		envPath := filepath.Join(dir, ".env")
		if err := godotenv.Load(envPath); err == nil || !os.IsNotExist(err) {
			return err
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

	// Don't pre-allocate when we're filtering
	var tasks []Task
	items := result.Items()
	for _, queryItem := range items {
		// Get the value as a map
		item := queryItem.Value()

		// Parse the task from the document
		task := Task{
			ID:      getStringValue(item, "_id"),
			Title:   getStringValue(item, "title"),
			Done:    getBoolValue(item, "done"),
			Deleted: getBoolValue(item, "deleted"),
		}
		if !task.Deleted {
			tasks = append(tasks, task)
		}
	}
	return tasks
}

// getSelectedTask returns the currently selected task and whether the selection is valid
func (a *App) getSelectedTask() (Task, bool) {
	if a.selectedIdx < len(a.tasks) {
		return a.tasks[a.selectedIdx], true
	}
	return Task{}, false
}

// updateTasks updates the task list and adjusts selection if needed
func (a *App) updateTasks(tasks []Task) {
	a.tasks = tasks
	if a.selectedIdx >= len(a.tasks) && len(a.tasks) > 0 {
		a.selectedIdx = len(a.tasks) - 1
	}
}

// Generic helper for type assertions
func getValueAs[T any](m map[string]any, key string) (T, bool) {
	if v, ok := m[key].(T); ok {
		return v, true
	}
	var zero T
	return zero, false
}

func getStringValue(m map[string]any, key string) string {
	if v, ok := getValueAs[string](m, key); ok {
		return v
	}
	return ""
}

func getBoolValue(m map[string]any, key string) bool {
	if v, ok := getValueAs[bool](m, key); ok {
		return v
	}
	return false
}
