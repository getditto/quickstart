#!/usr/bin/env node
import React from 'react';
import {render} from 'ink';
import meow from 'meow';
import App from './app.js';
import dotenv from 'dotenv';
import {Ditto, TransportConfig} from '@dittolive/ditto';
import {temporaryDirectory} from 'tempy';

const config = dotenv.config({path: '../.env'});
const cli = meow(
	`
    Usage
      $ npm start -- 2>/dev/null

    Options
      --app-id [env: DITTO_APP_ID] Your Ditto AppID
      --playground-token [env: DITTO_PLAYGROUND_TOKEN] An OnlinePlayground token
      --auth-url [env: DITTO_AUTH_URL] The auth URL
      --websocket-url [env: DITTO_WEBSOCKET_URL] The websocket URL
  `,
	{
		importMeta: import.meta,
		flags: {
			appId: {
				type: 'string',
			},
			playgroundToken: {
				type: 'string',
			},
			authURL: {
				type: 'string',
			},
			websocketURL: {
				type: 'string',
			},
		},
	},
);

console.log('Flags:', cli.flags);

// We use a temporary directory to store Ditto's local database.  This
// means that data will not be persistent between runs of the
// application, but it allows us to run multiple instances of the
// application concurrently on the same machine.  For a production
// application, we would want to store the database in a more permanent
// location, and if multiple instances are needed, ensure that each
// instance has its own persistence directory.
const tempdir = temporaryDirectory();

// Grab appID and token from CLI or .env in that order
const appID = cli.flags.appId ?? process.env.DITTO_APP_ID;
const token = cli.flags.playgroundToken ?? process.env.DITTO_PLAYGROUND_TOKEN;
const authURL = cli.flags.authURL ?? process.env.DITTO_AUTH_URL;
const websocketURL = cli.flags.websocketURL ?? process.env.DITTO_WEBSOCKET_URL;

console.log(
	'Using appId',
	appID,
	' and token ',
	token,
	' and authURL ',
	authURL,
	' and websocketURL ',
	websocketURL,
);

// Create a new Ditto instance with the identity
// https://docs.ditto.live/sdk/latest/install-guides/nodejs#installing-the-demo-task-app
const ditto = new Ditto(
	{
		type: 'onlinePlayground',
		appID: appID,
		token: token,
		customAuthURL: authURL,
		enableDittoCloudSync: false,
	},
	tempdir,
);

// Initialize transport config
ditto.updateTransportConfig(config => {
	config.connect.websocketURLs = [websocketURL];
});

// disable sync with v3 peers, required for DQL
await ditto.disableSyncWithV3();

await ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");
ditto.startSync();

process.on('uncaughtException', err => {
	console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', reason => {
	console.error('Unhandled Rejection:', reason);
});

render(<App ditto={ditto} />);
