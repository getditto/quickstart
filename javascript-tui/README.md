# Ditto Javascript Quickstart App 🚀

This app is a TUI built using Ink with React that demonstrates how to
create a peer-to-peer tasks app using Ditto.

## Getting Started

First, in the root of this repository, copy the `.env.sample` file to `.env`,
then fill out the variables with your Ditto AppID and Playground Token. If
you don't have those yet, visit https://portal.ditto.live

```
cp .env.sample .env
```

```
#!/usr/bin/env bash

# Copy this file from ".env.sample" to ".env", then fill in these values
# A Ditto AppID, Playground token, Auth URL, and Websocket URL can be obtained from https://portal.ditto.live
DITTO_APP_ID=""
DITTO_PLAYGROUND_TOKEN=""
DITTO_AUTH_URL=""
DITTO_WEBSOCKET_URL=""
```

Next, make sure you have `npm` installed, then run the following:

```
npm install
npm start 2>/dev/null
```

> NOTE: the `2>/dev/null` silences log output on stderr, because the logs
  interfere with the TUI rendering

