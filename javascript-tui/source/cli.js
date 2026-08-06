#!/usr/bin/env node
import React from 'react';
import { render } from 'ink';
import meow from 'meow';
import App from './app.js';
import dotenv from 'dotenv';
import { temporaryDirectory } from 'tempy';
import { DittoManager } from './ditto-manager.js';

dotenv.config({ path: '../.env' });
const cli = meow(
  `
    Usage
      $ npm start -- 2>/dev/null

    Options
      --database-id [env: DITTO_DATABASE_ID] Your Ditto Database ID
      --development-token [env: DITTO_DEVELOPMENT_TOKEN] A Development token
      --server-url [env: DITTO_SERVER_URL] The server URL
      --offline-license-token [env: DITTO_OFFLINE_LICENSE_TOKEN] An offline-only license token
  `,
  {
    importMeta: import.meta,
    flags: {
      databaseId: {
        type: 'string',
      },
      developmentToken: {
        type: 'string',
      },
      serverURL: {
        type: 'string',
      },
      offlineLicenseToken: {
        type: 'string',
      },
    },
  },
);

// We use a temporary directory to store Ditto's local database.  This
// means that data will not be persistent between runs of the
// application, but it allows us to run multiple instances of the
// application concurrently on the same machine.  For a production
// application, we would want to store the database in a more permanent
// location, and if multiple instances are needed, ensure that each
// instance has its own persistence directory.
const tempdir = temporaryDirectory();

// Grab database ID and token from CLI or .env in that order
const databaseId = cli.flags.databaseId ?? process.env.DITTO_DATABASE_ID;
const token = cli.flags.developmentToken ?? process.env.DITTO_DEVELOPMENT_TOKEN;
const serverURL = cli.flags.serverURL ?? process.env.DITTO_SERVER_URL;
const offlineLicenseToken = (
  cli.flags.offlineLicenseToken ??
  process.env.DITTO_OFFLINE_LICENSE_TOKEN ??
  ''
).trim();

// Open a configured, already-running Ditto instance via the DittoManager,
// which owns instance/identity/transport/sync setup.
// https://docs.ditto.live/sdk/latest/install-guides/nodejs#installing-the-demo-task-app
const dittoManager = new DittoManager({
  databaseId,
  token,
  serverURL,
  offlineLicenseToken,
  persistenceDirectory: tempdir,
});
const ditto = await dittoManager.open();

process.on('uncaughtException', (err) => {
  console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', (reason) => {
  console.error('Unhandled Rejection:', reason);
});

const { waitUntilExit } = render(
  <App ditto={ditto} dittoManager={dittoManager} />,
);
await waitUntilExit();
