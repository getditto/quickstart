#!/usr/bin/env node
import React from 'react';
import {render} from 'ink-testing-library';
import dotenv from 'dotenv';
import {Ditto} from '@dittolive/ditto';
import {temporaryDirectory} from 'tempy';
import App from '../dist/app.js';

dotenv.config({path: '../.env'});


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
		console.log('DQL strict mode setup:', error.message);
	}
	
	ditto.startSync();
	return ditto;
}

async function runE2ETest() {
	try {
		const expectedTitle = process.env.GITHUB_TEST_DOC_TITLE;

		if (!expectedTitle || expectedTitle.trim() === '') {
			throw new Error('Missing GITHUB_TEST_DOC_TITLE environment variable');
		}

		const ditto = await createDittoInstance();
		const {stdout} = render(React.createElement(App, {ditto}));

	for (let i = 1; i <= 10; i++) {
			await new Promise(resolve => setTimeout(resolve, 2000));
			
			const frame = stdout.lastFrame();
			const hasSyncActive = frame.includes('🟢 Sync Active');
			const hasTask = frame.includes(expectedTitle);
			
			if (hasSyncActive && hasTask) {
				console.log('SUCCESS: E2E test passed!');
				await ditto.close();
				process.exit(0);
			}
		}

	const finalFrame = stdout.lastFrame();
		const hasSyncActive = finalFrame.includes('🟢 Sync Active');
		const hasTask = finalFrame.includes(expectedTitle);
		
		await ditto.close();
		
		if (hasSyncActive && hasTask) {
			console.log('SUCCESS: All E2E checks passed!');
			process.exit(0);
		} else {
			console.log('FAILURE: E2E test conditions not met');
			process.exit(1);
		}

	} catch (error) {
		console.error('E2E test error:', error.message);
		process.exit(1);
	}
}

runE2ETest();