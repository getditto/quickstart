# Ditto Go Quickstart App 🚀

This directory contains Ditto's quickstart app for the Go SDK.
This app is a Terminal User Interface (TUI) that allows for creating
a todo list that syncs between multiple peers.

## Getting Started

To get started, you'll first need to create an app in the [Ditto Portal][0]
with the "Online Playground" authentication type. You'll need to find your
AppID and Online Playground Token, Auth URL, and Websocket URL in order to use this quickstart.

[0]: https://portal.ditto.live

Create a `.env` file in this directory with your Ditto credentials:

```bash
# Create .env file
cat > .env << 'EOF'
DITTO_APP_ID=your-app-id
DITTO_PLAYGROUND_TOKEN=your-playground-token
DITTO_AUTH_URL=https://your-app-id.cloud.ditto.live
EOF
```

Alternatively, you can set these as environment variables:

```bash
export DITTO_APP_ID="your-app-id"
export DITTO_PLAYGROUND_TOKEN="your-playground-token"
export DITTO_AUTH_URL="https://your-app-id.cloud.ditto.live"
```

## Building

From this directory (`go-tui`):

```bash
# Using the Makefile
make build

# Or build directly with Go
go build -o ditto-tasks-termui
```


## Running

**Note:** the Ditto Go SDK `libdittoffi.so` (Linux) or `libdittoffi.dylib`
shared library must be present and in one of the directories searched by the
system's dynamic linker to run the application.  This is handled automatically
by `make run`. If you use one of the other options, you may need to perform
additional steps.  See the [Go SDK Install Guide](https://docs.ditto.live/sdk/latest/install-guides/go)
for details.

Run the quickstart app with the following command:


```bash
# Using the Makefile (which will download the shared library and set shared-library load paths automatically)
make run
```

```bash
# Run the executable that was created via make build
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


## Troubleshooting

### Logs

To find errors and messages that are not printed to the TUI display, check the application logs.
Logs are output to `/tmp/ditto-tasks-termui.log`.

### libdittoffi Library not found

If you get a library loading error, ensure that the `libdittoffi.so` (Linux) or
`libdittoffi.dylib` (macOS) shared library is present and that `LD_LIBRARY_PATH`
(Linux) or `DYLD_LIBRARY_PATH` (macOS) is set appropriately.

### Environment variables not found

The app looks for a `.env` file in the current directory. Ensure it exists with
all required variables set, or export them as environment variables.

### Garbled screen output

Always run the application with `2>/dev/null` to suppress stderr output that can
interfere with the TUI display:

```bash
./ditto-tasks-termui 2>/dev/null
```

## Development

The application uses:
- [termui v3](https://github.com/gizak/termui) for the TUI framework (similar to Rust's ratatui)
- [Ditto Go SDK](https://github.com/getditto/ditto-go-sdk) for edge sync
- Channels for async communication between Ditto observers and the UI

## Architecture

The app follows an event-driven architecture:
- Direct event loop handling keyboard input
- Table widget for displaying tasks (similar to Rust's ratatui)
- Manual text input handling for create/edit modes
- Async updates from Ditto observers via Go channels
- Real-time sync with other Ditto peers running the same app
