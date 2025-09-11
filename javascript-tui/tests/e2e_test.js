#!/usr/bin/env node
import {spawn} from 'child_process';
import dotenv from 'dotenv';
import chalk from 'chalk';

dotenv.config({path: '../.env'});

/**
 * TUI End-to-End test using process spawning
 * Tests the actual TUI app by spawning the CLI and capturing output
 * Usage: GITHUB_TEST_DOC_TITLE="Task Name" node tests/e2e_test.js
 */

// Get the expected task title from environment
const expectedTitle = process.env.GITHUB_TEST_DOC_TITLE;

if (!expectedTitle || expectedTitle.trim() === '') {
	console.error(
		chalk.red('❌ Missing GITHUB_TEST_DOC_TITLE environment variable'),
	);
	process.exit(1);
}

console.log(chalk.blue(`🧪 Testing TUI for task: '${expectedTitle}'`));

// Start the TUI process
const tui = spawn('node', ['dist/cli.js'], {
	env: process.env,
	stdio: ['ignore', 'pipe', 'pipe'], // Capture stdout and stderr
});

let output = '';
let foundSyncActive = false;
let foundTask = false;

// Capture and process stdout
tui.stdout.on('data', data => {
	const text = data.toString();
	output += text;

	// Check for sync indicator
	if (text.includes('🟢 Sync Active')) {
		foundSyncActive = true;
		console.log(chalk.green('✅ Found sync indicator: 🟢 Sync Active'));
	}

	// Check for our test task
	if (text.includes(expectedTitle)) {
		foundTask = true;
		console.log(chalk.green(`✅ Found test task: '${expectedTitle}'`));
	}
});

// Capture stderr for debugging
tui.stderr.on('data', data => {
	const text = data.toString();
	console.log(chalk.gray('stderr:', text.slice(0, 200)));
});

// Handle process exit
tui.on('exit', code => {
	console.log(chalk.yellow(`TUI process exited with code: ${code}`));
});

// Run test for maximum 45 seconds
const timeout = setTimeout(() => {
	console.log(chalk.yellow('⏰ Test timeout reached, terminating TUI'));
	tui.kill('SIGTERM');

	// Give results after timeout
	setTimeout(() => {
		console.log(chalk.blue('\n📊 Test Results:'));
		console.log(
			`   Sync Active Found: ${
				foundSyncActive ? chalk.green('✅') : chalk.red('❌')
			}`,
		);
		console.log(
			`   Task Found: ${foundTask ? chalk.green('✅') : chalk.red('❌')}`,
		);

		if (foundSyncActive && foundTask) {
			console.log(chalk.green('\n🎉 SUCCESS: All checks passed!'));
			process.exit(0);
		} else {
			console.log(chalk.red('\n💥 FAILURE: Test conditions not met'));
			console.log(chalk.gray('Last 500 chars of output:'));
			console.log(chalk.gray(output.slice(-500)));
			process.exit(1);
		}
	}, 1000);
}, 45000);

// Check periodically if we found both conditions
const checkInterval = setInterval(() => {
	if (foundSyncActive && foundTask) {
		console.log(chalk.green('\n🎉 SUCCESS: Both conditions met!'));
		clearTimeout(timeout);
		clearInterval(checkInterval);
		tui.kill('SIGTERM');

		setTimeout(() => {
			console.log(chalk.blue('📊 Test Results:'));
			console.log(`   Sync Active Found: ${chalk.green('✅')}`);
			console.log(`   Task Found: ${chalk.green('✅')}`);
			process.exit(0);
		}, 1000);
	}
}, 2000);
