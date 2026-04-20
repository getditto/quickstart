#!/usr/bin/env node
import React from 'react';
import {render} from 'ink-testing-library';
import dotenv from 'dotenv';
import {Ditto, DittoConfig, Authenticator} from '@dittolive/ditto';
import {temporaryDirectory} from 'tempy';
import App from '../dist/app.js';

dotenv.config({path: '../.env'});

// Silence Ditto verbose logging for tests
process.env.RUST_LOG = 'off';

const MAX_WAIT_ITERATIONS = 10;
const POLL_INTERVAL_MS = 2000;

async function createDittoInstance() {
	const tempdir = temporaryDirectory();
	const appID = process.env.DITTO_APP_ID;
	const token = process.env.DITTO_PLAYGROUND_TOKEN;
	const authURL = process.env.DITTO_AUTH_URL;
	const websocketURL = process.env.DITTO_WEBSOCKET_URL;

	const connectConfig = {
		mode: 'server',
		url: authURL,
	};

	const config = new DittoConfig(appID, connectConfig, tempdir);
	const ditto = await Ditto.open(config);

	ditto.updateTransportConfig(config => {
		config.connect.websocketURLs = [websocketURL];
	});

	if (connectConfig.mode === 'server') {
		await ditto.auth.setExpirationHandler(
			async (dittoInstance, timeUntilExpiration) => {
				if (dittoInstance.auth.loginSupported) {
					const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
					await dittoInstance.auth.login(token, devProvider);
				}
			},
		);

		if (ditto.auth.loginSupported) {
			const devProvider = Authenticator.DEVELOPMENT_PROVIDER;
			const loginResult = await ditto.auth.login(token, devProvider);
			if (loginResult.error) {
				throw new Error(`Login failed: ${loginResult.error}`);
			}
		}
	}

	ditto.sync.start();
	return ditto;
}

async function runIntegrationTest() {
	let ditto;
	try {
		const expectedTitle = process.env.DITTO_CLOUD_TASK_TITLE;

		if (!expectedTitle || expectedTitle.trim() === '') {
			throw new Error('Missing DITTO_CLOUD_TASK_TITLE environment variable');
		}

		ditto = await createDittoInstance();
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

		console.error('FAILURE: Integration test conditions not met');
		await ditto.close();
		process.exit(1);
	} catch (error) {
		console.error('Integration test error:', error);
		if (ditto) {
			await ditto.close();
		}
		process.exit(1);
	}
}

runIntegrationTest();
