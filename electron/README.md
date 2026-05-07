# Ditto Electron Quickstart App 🚀

This directory contains Ditto's quickstart app for [Electron](https://www.electronjs.org/) desktop applications. The Ditto Node SDK runs in Electron's **main process**, providing full peer-to-peer sync. The UI is a React + TypeScript app in the **renderer process**, communicating with Ditto over Electron's IPC.

## Documentation

- [Javascript Install Guide](https://docs.ditto.com/sdk/latest/install-guides/js)
- [Javascript API Reference](https://software.ditto.live/js/Ditto/5.0.0/api-reference/)
- [Javascript Release Notes](https://docs.ditto.com/sdk/latest/release-notes/js)

## Prerequisites

- [Node.js](https://nodejs.org/) v22.12 or later (matches the floor of the bundled Electron runtime)

## Supported platforms

- macOS (Apple Silicon)
- Linux (x64, arm64)
- Windows (x64)

## Architecture

Electron applications run code in two processes that share no memory: a Node.js **main** process (full OS access) and a Chromium **renderer** process (a sandboxed browser window). Because the Ditto Node SDK is a native addon, it must run in main. This quickstart wires it up like this:

```
main process              preload (context bridge)         renderer (React)
─────────────             ────────────────────────         ────────────────
Ditto.open(config)        contextBridge.exposeInMainWorld  window.ditto.*
auth.login(...)             ('ditto', api)                 window.ditto.onTasksUpdated(cb)
sync.start()              ipcRenderer.invoke / .on
registerSubscription
registerObserver  ────►  webContents.send('tasks:updated', tasks)  ────►  setTasks(tasks)
```

The renderer never imports `@dittolive/ditto`; it only calls `window.ditto.*` methods exposed by the preload script via `contextBridge`. Context isolation is enabled and Node integration is disabled in the renderer — the standard secure Electron defaults.

### Peer-to-peer transports

This quickstart enables LAN sync (TCP + mDNS + multicast) and disables Bluetooth LE and AWDL. BLE and AWDL on macOS require entitlements that only signed app bundles get; a plain `npm run dev` Electron process can't use them. LAN P2P provides peer-to-peer sync between devices on the same network, and the `DITTO_WEBSOCKET_URL` provides cloud sync to Ditto's Big Peer.

### Local storage

Ditto's local store is persisted at `app.getPath('userData')`, which resolves to:

- macOS: `~/Library/Application Support/Ditto Tasks/`
- Linux: `~/.config/Ditto Tasks/`
- Windows: `%APPDATA%/Ditto Tasks/`

Tasks survive restarts because of this. To reset, quit the app and delete that directory.

## Getting Started

To get started, you'll first need to create an app in the [Ditto Portal][0] with the "Online Playground" authentication type. You'll need your AppID, Playground Token, Auth URL, and WebSocket URL.

[0]: https://portal.ditto.live

From the repo root, copy the `.env.sample` file to `.env`, and fill in the fields:

```
cp .env.sample .env
```

The `.env` file should look like this (with your fields filled in):

```bash
DITTO_APP_ID=""
DITTO_PLAYGROUND_TOKEN=""
DITTO_AUTH_URL=""
DITTO_WEBSOCKET_URL=""
```

Then run the app:

```
cd electron
npm install
npm run dev
```
