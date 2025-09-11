#!/usr/bin/env node
import {Ditto} from '@dittolive/ditto';
import {temporaryDirectory} from 'tempy';
import dotenv from 'dotenv';
import chalk from 'chalk';

dotenv.config({path: '../.env'});

/**
 * Ditto Cloud End-to-End test
 * Tests that seeded task data syncs properly from Ditto Cloud
 * Usage: GITHUB_TEST_DOC_TITLE="Task Name" node tests/e2e_test.js
 */
async function main() {
	try {
		console.log(chalk.blue('🧪 JavaScript TUI End-to-End Test'));

		// Get the expected task title from environment
		const expectedTitle = process.env.GITHUB_TEST_DOC_TITLE;

		if (!expectedTitle || expectedTitle.trim() === '') {
			console.log(
				chalk.red(
					'❌ FAIL: Missing GITHUB_TEST_DOC_TITLE environment variable',
				),
			);
			process.exit(1);
		}

		console.log(chalk.yellow(`🔍 Looking for task: '${expectedTitle}'`));

		// Test the core functionality: Ditto Cloud sync
		const tempdir = temporaryDirectory();
		const appID = process.env.DITTO_APP_ID;
		const token = process.env.DITTO_PLAYGROUND_TOKEN;
		const authURL = process.env.DITTO_AUTH_URL;
		const websocketURL = process.env.DITTO_WEBSOCKET_URL;

		if (!appID || !token || !authURL || !websocketURL) {
			console.log(chalk.red('❌ FAIL: Missing required environment variables'));
			process.exit(1);
		}

		console.log(chalk.cyan('🔗 Connecting to Ditto Cloud...'));

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
		await ditto.store.execute('ALTER SYSTEM SET DQL_STRICT_MODE = false');

		console.log(chalk.green('📊 Querying tasks database...'));

		// Query the tasks that would be displayed by the TUI
		const maxWaitSeconds = 20;
		const startTime = Date.now();
		let found = false;

		const checkInterval = setInterval(async () => {
			const elapsed = Math.floor((Date.now() - startTime) / 1000);
			console.log(chalk.gray(`🔍 Checking at ${elapsed}s...`));

			try {
				// Query tasks using the same query as the TUI app
				const queryResult = await ditto.store.execute(
					'SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC'
				);
				
				// Check if our expected task is in the results
				const tasks = queryResult.items || [];
				const foundTask = tasks.find(task => 
					task.title === expectedTitle || task.text === expectedTitle
				);

				if (foundTask && !found) {
					found = true;
					console.log(chalk.green(`✅ SUCCESS: Task '${expectedTitle}' found in database`));
					console.log(chalk.blue(`📋 Task data: ${JSON.stringify(foundTask)}`));
					console.log(chalk.green(`🎉 PASS: E2E test completed in ${elapsed}s`));
					clearInterval(checkInterval);
					ditto.stopSync();
					process.exit(0);
				}

			} catch (error) {
				console.log(chalk.yellow(`⚠️  Query error at ${elapsed}s: ${error.message}`));
			}

			if (elapsed >= maxWaitSeconds && !found) {
				console.log(
					chalk.red(`❌ FAIL: Task '${expectedTitle}' not found after ${elapsed}s`)
				);
				clearInterval(checkInterval);
				ditto.stopSync();
				process.exit(1);
			}
		}, 1000);
	} catch (error) {
		console.log(chalk.red(`💥 FAIL: ${error.message}`));
		process.exit(1);
	}
}

main();
