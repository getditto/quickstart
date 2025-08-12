# Ditto Go Quickstart App 🚀

This directory contains Ditto's quickstart app for the Go SDK.
This app is a Terminal User Interface (TUI) that allows for creating
a todo list that syncs between multiple peers.

## Getting Started

To get started, you'll first need to create an app in the [Ditto Portal][0]
with the "Online Playground" authentication type. You'll need to find your
AppID and Online Playground Token, Auth URL, and Websocket URL in order to use this quickstart.

[0]: https://portal.ditto.live

From the repo root, copy the `.env.sample` file to `.env`, and fill in the
fields with your AppID, Online Playground Token, Auth URL, and Websocket URL:

```
cp ../../.env.sample ../../.env
```

The `.env` file should look like this (with your fields filled in):

```bash
#!/usr/bin/env bash

# Copy this file from ".env.sample" to ".env", then fill in these values
# A Ditto AppID, Online Playground Token, Auth URL, and Websocket URL can be obtained from https://portal.ditto.live
export DITTO_APP_ID=""
export DITTO_PLAYGROUND_TOKEN=""
export DITTO_AUTH_URL=""
export DITTO_WEBSOCKET_URL=""
```

## Building

First, build the FFI library (required for Ditto SDK):
```bash
(cd ../../ditto/sdks/go && make build)
```

Then build the application:
```bash
go build -o ditto-tasks-termui
```

## Running

Run the quickstart app with the following command:

```bash
./ditto-tasks-termui 2>/dev/null
```

Or run directly with Go:
```bash
go run main.go 2>/dev/null
```

> NOTE: The `2>/dev/null` is a workaround to silence output on `stderr`, since
> that would interfere with the TUI application. Without it, the screen will
> quickly become garbled due to Ditto's internal logging.

## Controls

- **j/↓**: Move down
- **k/↑**: Move up
- **Enter/Space**: Toggle task completion
- **c**: Create new task
- **e**: Edit selected task
- **d**: Delete selected task
- **q**: Quit application
- **Esc**: Cancel input mode

## Features

- ✅ Create, edit, and delete tasks
- ✅ Mark tasks as complete/incomplete  
- ✅ Real-time synchronization across devices
- ✅ Terminal-based interface using termui
- ✅ Cross-platform compatibility with other Ditto quickstart apps

## Data Model

Tasks are stored in a `tasks` collection with the following structure:
```json
{
  "_id": "unique-task-id",
  "title": "Task description",
  "done": false,
  "deleted": false
}
```

This matches the data model used by other quickstart apps (Rust, C++, etc.) for cross-platform sync compatibility.

## UI Features

The TUI displays tasks in a table format with:
- Selection indicator (❯❯) for the currently selected task
- Checkboxes showing task status (✅ for done, ☐ for not done)
- Modal overlay for creating and editing tasks
- Keyboard shortcut hints in the status bar

## Troubleshooting

### Library not found
If you get a library loading error, ensure the FFI library is built and available:
```bash
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:../../ditto/sdks/go/build
```

On macOS, you may need to use `DYLD_LIBRARY_PATH` instead:
```bash
export DYLD_LIBRARY_PATH=$DYLD_LIBRARY_PATH:../../ditto/sdks/go/build
```

### Environment variables not found
The app looks for `.env` file in parent directories. Ensure it exists in the repository root with all required variables set.

### Garbled screen output
Always run the application with `2>/dev/null` to suppress stderr output that can interfere with the TUI display:
```bash
./ditto-tasks-termui 2>/dev/null
```

## Development

The application uses:
- [termui v3](https://github.com/gizak/termui) for the TUI framework (similar to Rust's ratatui)
- [Ditto Go SDK](https://docs.ditto.live) for real-time sync
- Channels for async communication between Ditto observers and the UI

## Architecture

The app follows an event-driven architecture similar to the Rust TUI implementation:
- Direct event loop handling keyboard input
- Table widget for displaying tasks
- Manual text input handling for create/edit modes
- Async updates from Ditto observers via Go channels

## License

MIT