# Ditto Javascript Quickstart App

This app is a TUI built using [Ink](https://github.com/vadimdemedes/ink) with React that demonstrates how to create a peer-to-peer tasks app using the [Ditto v5 SDK](https://docs.ditto.live).

## Documentation

- [Javascript Install Guide](https://docs.ditto.live/sdk/latest/install-guides/js)
- [Javascript API Reference](https://docs.ditto.live/sdk/latest/api-reference/js)
- [Javascript Release Notes](https://docs.ditto.live/sdk/latest/release-notes/js)

## Prerequisites

- Node.js >= 20
- npm

## Getting Started

First, in the root of this repository, copy the `.env.sample` file to `.env`,
then fill out the variables with your Ditto Database ID, Server URL, and
Development Token. If you don't have those yet, visit https://portal.ditto.live

```bash
cp .env.sample .env
```

```
DITTO_DATABASE_ID=""
DITTO_DEVELOPMENT_TOKEN=""
DITTO_SERVER_URL=""
```

Next, install dependencies and run:

**MacOS/Linux**

```bash
npm install
npm start 2>/dev/null
```

**Windows**

```bash
npm install
npm start 2>NUL
```

> NOTE: the `2>/dev/null` silences log output on stderr, because the logs
> interfere with the TUI rendering

## Keyboard Controls

| Key     | Action            |
| ------- | ----------------- |
| `?`     | Toggle help panel |
| `↑`/`k` | Scroll up         |
| `↓`/`j` | Scroll down       |
| `c`     | Create task       |
| `d`     | Delete task       |
| `e`     | Edit task         |
| `s`     | Toggle sync       |
| `q`     | Quit              |
| `Enter` | Toggle done       |
| `Esc`   | Cancel/back       |

## Architecture

This app uses the Ditto v5 SDK in **server mode** with the Development
identity (development only). It connects to the Ditto server and syncs a
`tasks` collection.

Each task document has the following structure:

```json
{
  "_id": "unique-id",
  "title": "Task description",
  "done": false,
  "deleted": false
}
```

Soft deletes are used — tasks are marked `deleted: true` rather than removed
from the collection, so the deletion syncs to other peers.

## Development

```bash
npm run dev      # Watch mode (rebuilds on file changes)
npm run format   # Format code with Prettier
npm test         # Run format check and integration tests
```

## Offline-only mode (optional)

Set `DITTO_OFFLINE_LICENSE_TOKEN` in the repo-root `.env` to run this
app in offline-only mode (peer-to-peer only, no cloud sync). When the
token is non-empty, the playground/auth/websocket vars are not used.
Request a token from <support@ditto.com>. See the top-level
[README](../README.md#offline-only-mode-optional) for full details.
