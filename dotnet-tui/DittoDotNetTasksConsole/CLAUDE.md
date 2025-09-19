# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a .NET 9 Terminal UI (TUI) quickstart application that demonstrates the Ditto SDK for real-time task synchronization. It provides a console-based interface for managing todo tasks with automatic peer-to-peer sync capabilities using Terminal.Gui. The application showcases offline-first data synchronization and mesh networking capabilities across Windows, macOS, and Linux platforms.

## Key Components and Architecture

### Core Classes
- **Program.cs**: Entry point that loads environment variables from embedded .env resource, initializes Ditto, and launches the Terminal.Gui interface
- **TasksPeer.cs**: Encapsulates all Ditto SDK operations including:
  - Ditto initialization with Online Playground identity
  - DQL (Ditto Query Language) operations for CRUD operations
  - Subscription and observation management for real-time sync
  - Soft delete implementation using `deleted` field
- **TasksWindow.cs**: Terminal.Gui interface implementation providing keyboard-driven UI for task management
- **ToDoTask.cs**: Data model representing tasks with `Id`, `Title`, `Done`, and `Deleted` properties

### Data Model
Tasks use the following JSON structure:
```json
{
  "_id": "GUID-string",
  "title": "Task description",
  "done": false,
  "deleted": false
}
```

### Ditto Integration Pattern
1. Environment variables loaded from embedded `.env` resource
2. Temporary database directory created per instance for concurrent testing
3. DQL strict mode disabled for flexible queries
4. Subscription registered with `SELECT * FROM tasks WHERE NOT deleted`
5. Observer registered for real-time updates to the UI

## Development Commands

```bash
# Build the project
dotnet build

# Run the application
dotnet run

# Build and run in one command
dotnet build && dotnet run

# Build in Release mode
dotnet build -c Release

# Run tests (if test project exists)
dotnet test

# Run specific test
dotnet test --filter "FullyQualifiedName~TestMethodName"

# Clean build artifacts
dotnet clean
```

## Environment Configuration

The application requires a `.env` file at `../../.env` (repository root) with:
- `DITTO_APP_ID`
- `DITTO_PLAYGROUND_TOKEN`
- `DITTO_AUTH_URL`
- `DITTO_WEBSOCKET_URL`

The `.env` file is embedded as a resource during build and loaded at runtime.

## UI Controls

The Terminal.Gui interface supports:
- Arrow keys: Navigate tasks
- Enter: Edit task title
- Space: Toggle task completion
- Insert/+: Add new task
- Delete/-: Delete selected task
- Tab: Switch between list and buttons
- Escape: Exit application

## Key Implementation Details

- **Temporary Database**: Each instance uses a unique temp directory to allow concurrent testing
- **DQL Usage**: All data operations use DQL (Ditto Query Language) via `ExecuteAsync`
- **Soft Delete**: Tasks are marked as `deleted = true` rather than being removed
- **Initial Data**: Four sample tasks are inserted on first run if not already present
- **Logging**: Ditto logging disabled to prevent interference with Terminal.Gui output
- **Idle CPU Optimization**: MainLoop includes sleep to reduce CPU usage when idle

## Dependencies

- **Ditto SDK** (4.11.1): Real-time sync and data management
- **Terminal.Gui** (1.17.1): Cross-platform terminal UI framework
- **.NET 9.0**: Target framework

## Testing Considerations

- Application supports multiple concurrent instances on same machine due to unique temp directories
- Terminal.Gui may not run properly in some terminal emulators (noted: may not work in Warp)
- For production, use persistent database directory instead of temp directory
- Online Playground identity is for development only, not production use

## Offline Sync Testing

To demonstrate offline synchronization:
1. Launch the app on multiple devices or instances
2. Disconnect from WiFi while keeping WiFi enabled
3. Add, edit, and delete tasks to see offline collaboration
4. Reconnect to see automatic sync convergence

## Cross-Platform Support

- Runs on Windows, macOS, and Linux
- Can sync with other Ditto quickstart apps in different languages
- Demonstrates mesh networking capabilities for peer-to-peer sync

## Documentation References

- [Ditto C# .NET SDK Install Guide](https://docs.ditto.live/install-guides/c-sharp)
- [Ditto C# .NET SDK API Reference](https://software.ditto.live/dotnet/Ditto/4.9.1/api-reference/)
- [.NET Console Quickstart Guide](https://docs.ditto.live/sdk/latest/quickstarts/dotnet-console)

## INCIDENT-407: CFA Double-Trigger Investigation

This application is being used to reproduce and investigate a customer incident involving observers and subscriptions firing multiple times for the same data change.

### Issue Description
The "double-trigger" issue occurs when observer callbacks are triggered multiple times for a single data modification, causing:
- Duplicate UI updates
- Performance degradation
- Potential state inconsistencies

### Key Areas of Investigation

#### Observer/Subscription Pattern
The application uses both subscriptions and observers:
```csharp
// Subscription (in RegisterSubscription method)
_ditto.Sync.RegisterSubscription(Query);

// Observer (in ObserveTasksCollection method)
_ditto.Store.RegisterObserver(Query, async (queryResult) => {...});
```

#### Potential Trigger Points
1. **Initial local update**: When a task is modified locally
2. **Sync confirmation**: When the change syncs back from remote peers
3. **Query overlap**: Both subscription and observer use similar queries

### Reproduction Steps
1. Launch multiple instances of the application
2. Monitor observer callback invocations in `TasksPeer.ObserveTasksCollection`
3. Perform actions (add/edit/delete/toggle tasks)
4. Watch for duplicate callbacks for the same logical change
5. Check if UI updates occur multiple times

### Areas to Monitor
- `TasksPeer.ObserveTasksCollection`: Observer callback registration and firing
- `TasksWindow.RefreshTasks`: UI update method that responds to observer callbacks
- All task mutation methods: `AddTask`, `UpdateTaskTitle`, `UpdateTaskDone`, `DeleteTask`
- Sync state changes and their impact on observers

### Testing Scenarios
1. **Single instance**: Baseline behavior with no remote sync
2. **Multiple local instances**: Using temp directories for concurrent testing
3. **Network disconnect/reconnect**: Test offline changes and sync convergence
4. **Rapid updates**: Quick successive changes to the same task
5. **Cross-platform sync**: Test with other quickstart implementations

### Diagnostic Modes

The application now includes specialized diagnostic modes for investigating observer behavior:

#### Observer Mode (`--observe`)
Monitors observer callback patterns and detects duplicate triggers:
```bash
dotnet run --observe
```

Features:
- Logs all observer callbacks with timestamps and data
- Detects and terminates if identical data is received in consecutive callbacks
- Toggles 4 subscriptions on/off every second to test subscription state changes
- Evicts deleted tasks during subscription cancellation
- Reports total callback count and duplicate detection status

#### Generator Mode (`--generate`)
Continuously generates/updates tasks to stress-test observers:
```bash
dotnet run --generate
```

Features:
- Updates 10 tasks per second with IDs "1" through "10"
- Uses `ON ID CONFLICT DO UPDATE` to prevent collection growth
- Sets task titles to current ISO 8601 timestamp
- Useful for testing observer behavior under continuous data changes
- Reports generation statistics on exit

#### Implementation Details

##### Duplicate Detection Algorithm
The observer mode uses the following approach to detect duplicates:
1. Stores JSON representation of all items from each callback
2. Sorts items for order-independent comparison
3. Compares with previous callback data
4. Terminates immediately if identical data detected

##### Subscription Toggling
Observer mode tests the impact of subscription state changes:
- Cancels all 4 subscriptions (setting references to null)
- Evicts deleted tasks during cancellation
- Forces garbage collection
- Re-registers subscriptions with original queries
- Repeats cycle every second

##### Metadata Configuration
Both diagnostic modes set comprehensive peer metadata including:
- Device name (mode-specific)
- Process ID, machine name, user name
- OS version, .NET version
- Start time (ISO 8601)
- Platform (x64/x86)
- Working directory
- Unique session ID

### Known Issues and Observations

1. **Async Task Blocking**: Initial implementation used `ManualResetEvent.WaitOne()` which blocked async tasks. Fixed by using `TaskCompletionSource` for proper async/await pattern.

2. **DQL Syntax**: DQL requires single quotes for string literals in INSERT statements, not double quotes.

3. **Multiple Subscriptions**: Testing with 4 overlapping subscriptions that all match the same data to investigate if multiple subscriptions contribute to duplicate callbacks.
- Ditto credentials are in the file ../../.env