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

	await ditto.disableSyncWithV3();

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

	try {
		await ditto.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');
	} catch (error) {
		console.error('Integration test DQL setup failed:', error);
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
		console.error('Integration test error:', error);
		process.exit(1);
	}
}

runIntegrationTest();
