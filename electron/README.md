# Ditto Electron Quickstart App 🚀

This directory contains Ditto's quickstart app for Electron desktop applications.
This app demonstrates how to integrate the Ditto SDK into a cross-platform desktop application built with Electron, React, and TypeScript.

![Electron Ditto Screenshot](../.github/assets/electron-ditto-screenshot.png)

## Getting Started

### Prerequisites

- Node.js (v18 or later)
- npm or yarn
- A Ditto app configured in the [Ditto Portal][0]

[0]: https://portal.ditto.live

### Ditto Configuration

To get started, you'll first need to create an app in the [Ditto Portal][0] with the "Online Playground" authentication type. You'll need to find your AppID and Playground Token in order to use this quickstart.

From the repo root, copy the `.env.sample` file to `.env`, and fill in the fields with your AppID and Playground Token:

```bash
cp .env.sample .env
```

The `.env` file should look like this (with your fields filled in):

```bash
#!/usr/bin/env bash

# Copy this file from ".env.sample" to ".env", then fill in these values
# A Ditto AppID, Playground token, Auth URL, and Websocket URL can be obtained from https://portal.ditto.live
DITTO_APP_ID=""
DITTO_PLAYGROUND_TOKEN=""
DITTO_AUTH_URL=""
DITTO_WEBSOCKET_URL=""
```

### Installation

1. Navigate to the electron directory:
```bash
cd electron
```

2. Install dependencies:
```bash
npm install
```

This will install both the main process dependencies and the renderer (React) dependencies.

### Running the App

#### Development Mode

To run the app in development mode with hot reloading:

```bash
npm run dev
```

This will:
- Start the Vite development server for the renderer process
- Launch the Electron app once the renderer is ready
- Enable hot reloading for both main and renderer processes

#### Production Mode

To build and run the production version:

```bash
npm run build
npm start
```

### Building Distribution Packages

To create distributable packages for different platforms:

```bash
# Build for all platforms
npm run dist

# Build for specific platforms
npm run dist:win    # Windows
npm run dist:mac    # macOS
npm run dist:linux  # Linux
```

Built packages will be available in the `dist-electron/` directory.

## Architecture

This Electron app follows a secure architecture pattern:

- **Main Process** (`main.js`): Handles Ditto SDK operations, window management, and IPC communication
- **Renderer Process** (`renderer/`): React-based UI that communicates with the main process via IPC
- **Preload Script** (`preload.js`): Provides secure bridge between main and renderer processes

### Security Features

- Context isolation enabled
- Node.js integration disabled in renderer
- Secure IPC communication through exposed APIs
- No direct access to Node.js APIs from renderer

## Features

- **Task Management**: Create, edit, toggle, and delete tasks
- **Real-time Sync**: Tasks sync automatically across all connected devices
- **Offline Support**: Works offline and syncs when connection is restored
- **Cross-platform**: Runs on Windows, macOS, and Linux
- **Modern UI**: Built with React and Tailwind CSS

## Development

### Project Structure

```
electron/
├── main.js              # Main Electron process
├── preload.js           # Secure IPC bridge
├── package.json         # Main process dependencies
├── renderer/            # React application
│   ├── src/
│   │   ├── components/  # React components
│   │   ├── App.tsx      # Main app component
│   │   └── types.ts     # TypeScript definitions
│   ├── package.json     # Renderer dependencies
│   └── vite.config.ts   # Vite configuration
└── README.md           # This file
```

### Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm start` - Run production build
- `npm run dist` - Create distribution packages

## Documentation

- [Ditto SDK Documentation](https://docs.ditto.live)
- [Electron Documentation](https://www.electronjs.org/docs)
- [React Documentation](https://reactjs.org/docs)

## Support

For support, please contact Ditto Support (<support@ditto.live>).

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.