# Go-idiomatic Improvements for go-tui

## Overview
The go-tui code was translated from Rust and contains several patterns that, while functional, could be more idiomatic Go. These improvements maintain all existing behavior and Ditto API usage.

## 1. Simplify Mutex Usage Patterns

### Current (Rust-like)
```go
// Lines 264-271: Complex lock/unlock dance
a.mu.RLock()
if a.selectedIdx < len(a.tasks) {
    task := a.tasks[a.selectedIdx]
    a.mu.RUnlock()
    go a.toggleTask(task.ID, !task.Done)
} else {
    a.mu.RUnlock()
}
```

### Idiomatic Go
```go
func (a *App) getSelectedTask() (Task, bool) {
    a.mu.RLock()
    defer a.mu.RUnlock()
    if a.selectedIdx < len(a.tasks) {
        return a.tasks[a.selectedIdx], true
    }
    return Task{}, false
}

// Then use it:
if task, ok := a.getSelectedTask(); ok {
    go a.toggleTask(task.ID, !task.Done)
}
```

## 2. Simplify Error Message Handling

### Current (Rust-like with goroutine cleanup)
```go
// Lines 389-397: Spawning goroutine to clear error
if a.errorMsg != "" {
    // ... render error ...
    go func() {
        time.Sleep(3 * time.Second)
        a.mu.Lock()
        a.errorMsg = ""
        a.mu.Unlock()
        a.render()
    }()
}
```

### Idiomatic Go
```go
// Use time.AfterFunc for cleaner scheduling
func (a *App) setError(msg string) {
    a.mu.Lock()
    a.errorMsg = msg
    a.mu.Unlock()
    
    time.AfterFunc(3*time.Second, func() {
        a.mu.Lock()
        a.errorMsg = ""
        a.mu.Unlock()
        a.render()
    })
}
```

## 3. Remove Forced Initial Query Sleep

### Current (Workaround pattern)
```go
// Lines 150-157: Sleep before initial query
go func() {
    time.Sleep(200 * time.Millisecond)
    result, err := d.Store().Execute("SELECT * FROM tasks WHERE deleted = false ORDER BY _id")
    if err == nil && result != nil {
        tasks := parseTasks(result)
        app.tasksChan <- tasks
    }
}()
```

### Idiomatic Go
```go
// Execute synchronously before starting the event loop
result, err := d.Store().Execute("SELECT * FROM tasks WHERE deleted = false ORDER BY _id")
if err == nil && result != nil {
    app.tasks = parseTasks(result)
}
// Then start the observer and event loop
```

## 4. Simplify Type Assertion Helpers

### Current (Rust Option-like)
```go
// Lines 545-557: Separate helper functions
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
```

### Idiomatic Go (inline or generic)
```go
// Option 1: Inline the simple assertions
task := Task{
    ID:      item["_id"].(string),  // Assuming we know the types
    Title:   item["title"].(string),
    Done:    item["done"].(bool),
    Deleted: item["deleted"].(bool),
}

// Option 2: If defensive, use a generic helper (Go 1.18+)
func getOrDefault[T any](m map[string]interface{}, key string, defaultVal T) T {
    if v, ok := m[key].(T); ok {
        return v
    }
    return defaultVal
}
```

## 5. Simplify InputMode Enum

### Current (Rust-style enum)
```go
type InputMode int
const (
    NormalMode InputMode = iota
    CreateMode
    EditMode
)
```

### Idiomatic Go
```go
// Consider using a struct with state instead
type InputState struct {
    editing   bool
    creating  bool
    buffer    string
    editingID string
}

// Or use simple booleans if states are mutually exclusive
type App struct {
    // ...
    isCreating bool
    isEditing  bool
    // ...
}
```

## 6. Use Context for Cancellation

### Current
```go
// No context-based cancellation
func (a *App) Run() {
    for {
        select {
        case e := <-uiEvents:
            // ...
        }
    }
}
```

### Idiomatic Go
```go
func (a *App) Run(ctx context.Context) {
    for {
        select {
        case <-ctx.Done():
            return
        case e := <-uiEvents:
            // ...
        }
    }
}
```

## 7. Simplify Task Parsing

### Current (Pre-allocates then filters)
```go
// Lines 517-541: Allocates capacity but then filters
tasks := make([]Task, 0, result.ItemCount())
for i := 0; i < result.ItemCount(); i++ {
    // ... parse task ...
    if !task.Deleted {
        tasks = append(tasks, task)
    }
}
```

### Idiomatic Go
```go
// Don't pre-allocate if filtering
var tasks []Task
for i := 0; i < result.ItemCount(); i++ {
    // ... parse task ...
    if !task.Deleted {
        tasks = append(tasks, task)
    }
}
```

## 8. Use Functional Options for Widget Creation

### Current
```go
// Lines 173-179: Manual property setting
app.taskTable = widgets.NewTable()
app.taskTable.Title = " Tasks (j↓, k↑, ⏎ toggle done) "
app.taskTable.BorderStyle = ui.NewStyle(ui.ColorCyan)
app.taskTable.RowSeparator = false
```

### Idiomatic Go (if we could modify the widget creation)
```go
// Create a helper for cleaner initialization
func newTaskTable() *widgets.Table {
    t := widgets.NewTable()
    t.Title = " Tasks (j↓, k↑, ⏎ toggle done) "
    t.BorderStyle = ui.NewStyle(ui.ColorCyan)
    t.RowSeparator = false
    t.FillRow = true
    t.RowStyles[0] = ui.NewStyle(ui.ColorWhite, ui.ColorClear, ui.ModifierBold)
    return t
}
```

## 9. Simplify Channel Usage

### Current
```go
// Line 169: Arbitrary buffer size
tasksChan: make(chan []Task, 10),
```

### Idiomatic Go
```go
// Use unbuffered for synchronization or size 1 for simple cases
tasksChan: make(chan []Task, 1),  // Latest update wins
```

## 10. Event Handler Map

### Current (Large switch statement)
```go
func (a *App) handleNormalMode(e ui.Event) {
    switch e.ID {
    case "j", "<Down>":
        // ...
    case "k", "<Up>":
        // ...
    }
}
```

### Idiomatic Go
```go
// Define handlers as a map
var normalModeHandlers = map[string]func(*App){
    "j":      (*App).moveDown,
    "<Down>": (*App).moveDown,
    "k":      (*App).moveUp,
    "<Up>":   (*App).moveUp,
    // ...
}

func (a *App) handleNormalMode(e ui.Event) {
    if handler, ok := normalModeHandlers[e.ID]; ok {
        handler(a)
    }
}
```

## 11. Embed Mutex for Cleaner Code

### Current
```go
type App struct {
    // ...
    mu sync.RWMutex
}

// Usage:
a.mu.Lock()
defer a.mu.Unlock()
```

### Idiomatic Go (for some cases)
```go
type App struct {
    // ...
    sync.RWMutex  // Embedded
}

// Usage:
a.Lock()
defer a.Unlock()
```

## 12. Use sync.Once for Initialization

### Current
```go
// Complex initialization spread across main and NewApp
```

### Idiomatic Go
```go
type App struct {
    initOnce sync.Once
    // ...
}

func (a *App) ensureInitialized() {
    a.initOnce.Do(func() {
        // One-time initialization
    })
}
```

## Summary of Key Changes

1. **Reduce mutex complexity** - Use helper methods with defer
2. **Simplify error handling** - Use time.AfterFunc instead of goroutines with sleep
3. **Remove initialization sleeps** - Do synchronous setup before async operations
4. **Inline simple type assertions** - Don't over-abstract
5. **Use context for cancellation** - Standard Go pattern for lifecycle management
6. **Simplify state management** - Consider alternatives to enum-like patterns
7. **Optimize slice allocation** - Don't pre-allocate when filtering
8. **Consider handler maps** - For large switch statements
9. **Use appropriate channel sizes** - Not arbitrary buffers

These changes would make the code more idiomatic Go while maintaining identical functionality and Ditto API usage.