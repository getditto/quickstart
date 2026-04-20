# Ditto Javascript Quickstart App

This app is a TUI built using [Ink](https://github.com/vadimdemedes/ink) with React that demonstrates how to
create a peer-to-peer tasks app using the [Ditto v5 SDK](https://docs.ditto.live).

## Prerequisites

- Node.js >= 20
- npm

## Getting Started

First, in the root of this repository, copy the `.env.sample` file to `.env`,
then fill out the variables with your Ditto AppID, Auth URL, Playground Token,
and Websocket URL. If you don't have those yet, visit https://portal.ditto.live

```bash
cp .env.sample .env
```

```
DITTO_APP_ID=""
DITTO_PLAYGROUND_TOKEN=""
DITTO_AUTH_URL=""
DITTO_WEBSOCKET_URL=""
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

This app uses the Ditto v5 SDK in **server mode** with the Online Playground
identity (development only). It connects to the Ditto cloud via WebSocket and
syncs a `tasks` collection.

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
