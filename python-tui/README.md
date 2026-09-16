# Ditto Python Quickstart App 🐍

This directory contains Ditto's quickstart app for the Python SDK.
This app is a console application that manages a todo list that syncs
between multiple peers.

It shares the `tasks` collection schema with every other quickstart app in
this repository, so it syncs with the Swift, Kotlin, Go, Rust, and JavaScript
quickstarts. Python-created tasks include a `created_at` timestamp so this TUI
displays them in creation order.

## Requirements

- **Python 3.10 or later.** The SDK is tested against 3.10–3.13; 3.14 also
  works. See the [Python Compatibility page](https://docs.ditto.live/sdk/latest/compatibility/python).
- A Ditto [Portal][0] account with a Database configured for **Development**
  authentication.

[0]: https://portal.ditto.live

The Ditto Python SDK ships prebuilt wheels that bundle the native library, so
there is no separate shared-library download step.

## Getting Started

Find your Database ID, Development Token, and URL in the
[Ditto Portal][0], then create a `.env` file in this directory:

```bash
cp .env.sample .env
```

```bash
DITTO_DATABASE_ID="your-database-id"
DITTO_DEVELOPMENT_TOKEN="your-development-token"
DITTO_SERVER_URL="your-server-url"
```

Alternatively, set them as environment variables, or use the shared `.env` at
the root of this repository. The app checks this directory first, then the
repository root.

## Running

Using [uv][1] (recommended — it installs Python and dependencies for you):

```bash
uv run --with dittolive-ditto main.py
```

[1]: https://docs.astral.sh/uv/

Or with a virtual environment and pip:

```bash
python3 -m venv .venv
source .venv/bin/activate        # Windows: .venv\Scripts\activate
pip install dittolive-ditto
python main.py
```

### Commands

Once running, the app accepts these commands:

| Command | Description |
| --- | --- |
| `add <title>` | Create a task |
| `toggle <n>` | Toggle a task's completed state |
| `edit <n> <title>` | Rename a task |
| `del <n>` | Delete a task |
| `list` | Refresh the list |
| `help` | Show help |
| `quit` | Exit |

Press Enter (or type `list`) to refresh and pick up changes made by other
peers.

Pass `--verbose` to see the SDK's own logs, which are suppressed by default so
they don't scribble over the console UI.

## Sync Data Offline

1. Launch the application on multiple devices, or alongside another quickstart
   app.
2. Disconnect from your WiFi network while keeping WiFi enabled on the device,
   to allow for LAN connections.
3. Add, edit, and delete tasks and experience offline collaboration!

## Self-test

To verify your installation without any credentials or network access:

```bash
uv run --with dittolive-ditto main.py --smoke
```

This opens a temporary local peer and runs the full insert / update / rename /
delete path, asserting that the store observer reports each change.

Note that `--smoke` does **not** start sync. Starting sync requires an
activated instance. If there is no license token, the SDK will raise
`DittoError <activation>`. The local store, subscriptions, and observers all
work regardless; only replication with other peers needs activation.
