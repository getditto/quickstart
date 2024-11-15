#!/usr/bin/env node
import React from 'react';
import { render } from 'ink';
import meow from 'meow';
import App from './app.js';
import dotenv from 'dotenv';
import { Ditto } from '@dittolive/ditto';

const config = dotenv.config();
const cli = meow(
	`
		Usage
		  $ javascript

		Options
			--app-id Your Ditto AppID
			--playground-token An OnlinePlayground token

		Examples
		  $ javascript --name=Jane
		  Hello, Jane
	`,
	{
		importMeta: import.meta,
		flags: {
			appId: {
				type: "string"
			},
			playgroundToken: {
				type: "string"
			}
		}
	},
);

console.log("Flags:", cli.flags);

// Grab appID and token from CLI or .env in that order
const appID = cli.flags.appId ?? process.env.DITTO_APP_ID;
const token = cli.flags.playgroundToken ?? process.env.DITTO_PLAYGROUND_TOKEN;
console.log("Using appId", appID, " and token ", token);
const ditto = new Ditto({
	type: "onlinePlayground",
	appID,
	token,
});
await ditto.disableSyncWithV3();
ditto.startSync();

process.on('uncaughtException', (err) => {
	console.error('Uncaught Exception:', err);
});

process.on('unhandledRejection', (reason) => {
	console.error('Unhandled Rejection:', reason);
});

render(<App ditto={ditto} />);
