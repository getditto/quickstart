#!/usr/bin/env node
import React from 'react';
import {render} from 'ink-testing-library';
import dotenv from 'dotenv';
import {Ditto} from '@dittolive/ditto';
import {temporaryDirectory} from 'tempy';
import App from '../dist/app.js';

dotenv.config({path: '../.env'});

// Silence Ditto verbose logging for tests
process.env.RUST_LOG = 'error';

const MAX_WAIT_ITERATIONS = 10;
const POLL_INTERVAL_MS = 2000;

async function createDittoInstance() {
	const tempdir = temporaryDirectory();
	const appID = process.env.DITTO_APP_ID;
	const token = process.env.DITTO_PLAYGROUND_TOKEN;
	const authURL = process.env.DITTO_AUTH_URL;
	const websocketURL = process.env.DITTO_WEBSOCKET_URL;

	const ditto = new Ditto(
		{
			type: 'onlinePlayground',
			appID,
			token,
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
		console.error('Integration test DQL setup failed:', error.message);
		throw error;
	}

	ditto.startSync();
	return ditto;
}

async function runIntegrationTest() {
	try {
		const expectedTitle = process.env.GITHUB_TEST_DOC_TITLE;

		if (!expectedTitle || expectedTitle.trim() === '') {
			throw new Error('Missing GITHUB_TEST_DOC_TITLE environment variable');
		}

		const ditto = await createDittoInstance();
		const {stdout} = render(React.createElement(App, {ditto}));

		for (let i = 1; i <= MAX_WAIT_ITERATIONS; i++) {
			await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL_MS));

			const frame = stdout.lastFrame();
			const hasSyncActive = frame.includes('🟢 Sync Active');
			const hasTask = frame.includes(expectedTitle);

			if (hasSyncActive && hasTask) {
				console.log('SUCCESS: Integration test passed!');
				await ditto.close();
				process.exit(0);
			}
		}

		await ditto.close();
		console.error('FAILURE: Integration test conditions not met');
		process.exit(1);
	} catch (error) {
		console.error('Integration test error:', error.message);
		process.exit(1);
	}
}

runIntegrationTest();
