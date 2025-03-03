# Ditto Rust Quickstart App 🚀

This directory contains Ditto's quickstart app for the Rust SDK.
This app is a Terminal User Interface (TUI) that allows for creating
a todo list that syncs between multiple peers.

## Getting Started

To get started, you'll first need to create an app in the [Ditto Portal][0]
with the "Online Playground" authentication type. You'll need to find your
AppID and Playground Token in order to use this quickstart.

[0]: https://portal.ditto.live

From the repo root, copy the `.env.sample` file to `.env`, and fill in the
fields with your AppID and Playground Token:

```
cp .sample.env .env
```

The `.env` file should look like this (with your fields filled in):

```bash
#!/usr/bin/env bash

# Copy this file from ".env.sample" to ".env", then fill in these values
# A Ditto AppID and Playground token can be obtained from https://portal.ditto.live
export DITTO_APP_ID=""
export DITTO_PLAYGROUND_TOKEN=""
export DITTO_CUSTOM_AUTH_URL = "";
export DITTO_WEBSOCKET_URL = "";
```

Next, run the quickstart app with the following command:

```
cargo run 2>/dev/null
```

> NOTE: The `2>/dev/null` is a workaround to silence output on `stderr`, since
> that would interfere with the TUI application. Without it, the screen will
> quickly become garbled.

