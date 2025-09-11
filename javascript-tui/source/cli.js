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

const tempdir = temporaryDirectory();
const appID = cli.flags.appId ?? process.env.DITTO_APP_ID;
const token = cli.flags.playgroundToken ?? process.env.DITTO_PLAYGROUND_TOKEN;
const authURL = cli.flags.authURL ?? process.env.DITTO_AUTH_URL;
const websocketURL = cli.flags.websocketURL ?? process.env.DITTO_WEBSOCKET_URL;

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

ditto.updateTransportConfig(config => {
	config.connect.websocketURLs = [websocketURL];
});

await ditto.disableSyncWithV3();

try {
	await ditto.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');
} catch (error) {
	console.error('Failed to disable DQL strict mode:', error);
	process.exit(1);
}

ditto.startSync();

process.on('uncaughtException', err => {
	console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', reason => {
	console.error('Unhandled Rejection:', reason);
});

render(<App ditto={ditto} />);
