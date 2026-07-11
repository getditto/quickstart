# Ditto Python Quickstart App 🚀

This directory contains Ditto's quickstart app for the Python SDK
(`dittolive-ditto`). It is a console app that manages a todo list which syncs
between multiple peers, sharing the same `tasks` collection as every other
quickstart app in this repo.

It runs on **Linux (including Raspberry Pi / aarch64), macOS, and Windows**.

## Prerequisites

- **Python 3.10 or newer**
- [`uv`](https://docs.astral.sh/uv/) or `pip` for managing a virtual environment

## Getting Started

To get started, you'll first need to create an app in the [Ditto Portal][0]
with the "Online Playground" authentication type. You'll need your AppID,
Online Playground Token, Auth URL, and Websocket URL to use this quickstart.

[0]: https://portal.ditto.live

From the repo root, copy `.env.sample` to `.env` and fill in your credentials:

```bash
cp .env.sample .env
```

```bash
DITTO_APP_ID=""
DITTO_PLAYGROUND_TOKEN=""
DITTO_AUTH_URL=""
DITTO_WEBSOCKET_URL=""
```

Alternatively, export them as environment variables.

## Install

`dittolive-ditto` is **not yet published to PyPI**, so install the SDK editable
from your Ditto monorepo checkout **first**, then this app. From this directory
(`python-tui`):

```bash
# Using pip (python -m venv includes pip):
python3 -m venv .venv
.venv/bin/pip install -e /path/to/ditto/sdks/python   # the SDK (not on PyPI yet)
.venv/bin/pip install -e .                             # this app (+ python-dotenv)
```

```bash
# Using uv (note: `uv venv` does NOT include pip — use `uv pip`, not `python -m pip`):
uv venv --python 3.12 .venv
uv pip install -e /path/to/ditto/sdks/python
uv pip install -e .
```

The editable SDK install lets the loader auto-discover a locally built
`libdittoffi` (build it with `make build-mac` / `make build-linux` from the
monorepo; the loader also honors `DITTOFFI_LIB_PATH`). Once `dittolive-ditto`
is published, `pip install -e .` alone will pull the SDK from PyPI.

## Running

```bash
.venv/bin/python main.py
```

The app reads its configuration from the environment:

- **Cloud (Big Peer) mode** — used automatically when `DITTO_APP_ID` is set.
  Requires `DITTO_APP_ID`, `DITTO_PLAYGROUND_TOKEN`, and `DITTO_AUTH_URL`.
  Peers sync through your Big Peer.
- **Offline (peer-to-peer) mode** — used when `DITTO_APP_ID` is unset and
  `DITTO_OFFLINE_TOKEN` (an offline license token) is set. No cloud is
  required; peers on the same network discover and sync over LAN/mDNS and the
  other peer-to-peer transports.

```bash
# Offline P2P, no cloud account needed
DITTO_OFFLINE_TOKEN="your-offline-license" .venv/bin/python main.py
```

Unlike the full-screen TUI quickstarts, this is a line-oriented console app, so
there is no need to redirect stderr. Ditto's log level is set to `ERROR` to keep
the output clean.

## Controls

At the `>` prompt:

- `add <title>` — create a task
- `done <n>` — toggle task number `n` complete/incomplete
- `edit <n> <title>` — rename task number `n`
- `del <n>` — delete task number `n`
- `list` (or press Enter) — refresh the list (shows changes synced from peers)
- `help` — show commands
- `quit` — exit

## Data Model

All quickstart apps share the same `tasks` collection so they interoperate:

```json
{
  "_id": "unique-id",
  "title": "Task description",
  "done": false,
  "deleted": false
}
```

## Smoke test

A non-interactive check opens Ditto offline, inserts a task, observes it, and
exits. It reads the offline license from `DITTO_OFFLINE_TOKEN`:

```bash
DITTO_OFFLINE_TOKEN="your-offline-license" .venv/bin/python main.py --smoke
```

## Troubleshooting

### libdittoffi library not found

The Python SDK loads `libdittoffi` at first use. If you see a loader error, set
`DITTOFFI_LIB_PATH` to the built library (or its directory), or ensure it is on
the system library path. See the [Python SDK install guide][1].

[1]: https://docs.ditto.live/sdk/latest/install-guides/python

### Environment variables not found

The app loads a `.env` file from the current directory or a parent directory.
Ensure it exists with the required variables, or export them directly.
