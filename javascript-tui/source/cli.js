#!/usr/bin/env node
import React from 'react';
import {render} from 'ink';
import meow from 'meow';
import App from './app.js';
import dotenv from 'dotenv';
import {Ditto, DittoConfig, Authenticator} from '@dittolive/ditto';
import {temporaryDirectory} from 'tempy';

dotenv.config({path: '../.env'});
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

// Create a new Ditto instance with the DittoConfig
// https://docs.ditto.live/sdk/latest/install-guides/nodejs#installing-the-demo-task-app
const connectConfig = {
	mode: 'server',
	url: authURL,
};

const config = new DittoConfig(appID, connectConfig, tempdir);
const ditto = await Ditto.open(config);

// Initialize transport config
ditto.updateTransportConfig(config => {
	config.connect.websocketURLs = [websocketURL];
});

// disable sync with v3 peers, required for DQL
await ditto.disableSyncWithV3();

// Set up authentication for server mode
if (connectConfig.mode === 'server') {
	await ditto.auth.setExpirationHandler(
		async (dittoInstance, timeUntilExpiration) => {
			console.log(
				'Authentication expiring soon, time until expiration:',
				timeUntilExpiration,
			);

			if (dittoInstance.auth.loginSupported) {
				const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
				const reLoginResult = await dittoInstance.auth.login(
					token,
					devProvider,
				);
				if (reLoginResult.error) {
					console.error('Re-authentication failed:', reLoginResult.error);
				} else {
					console.log(
						'Successfully re-authenticated with info:',
						reLoginResult,
					);
				}
			}
		},
	);

	if (ditto.auth.loginSupported) {
		const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
		const loginResult = await ditto.auth.login(token, devProvider);
		if (loginResult.error) {
			console.error('Login failed:', loginResult.error);
		} else {
			console.log('Successfully logged in with info:', loginResult);
		}
	}
}

// Disable DQL strict mode
// when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
// https://docs.ditto.live/dql/strict-mode
try {
	await ditto.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');
} catch (error) {
	console.error('Failed to disable DQL strict mode:', error);
	process.exit(1); // Exit the application with a non-zero status code
}

ditto.startSync();

process.on('uncaughtException', err => {
	console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', reason => {
	console.error('Unhandled Rejection:', reason);
});

render(<App ditto={ditto} />);
