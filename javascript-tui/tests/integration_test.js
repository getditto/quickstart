#!/usr/bin/env node
import { spawn } from 'child_process';
import chalk from 'chalk';

/**
 * TUI Integration test
 * Tests the actual TUI app like a customer would use it
 * Usage: GITHUB_TEST_DOC_TITLE="Task Name" node tests/integration_test.js
 */
async function main() {
	try {
		console.log(chalk.blue('🧪 JavaScript TUI Integration Test'));

		// Get the expected task title from environment
		const expectedTitle = process.env.GITHUB_TEST_DOC_TITLE;
		
		if (!expectedTitle || expectedTitle.trim() === '') {
			console.log(chalk.red('❌ FAIL: Missing GITHUB_TEST_DOC_TITLE environment variable'));
			process.exit(1);
		}

		console.log(chalk.yellow(`🔍 Looking for task in TUI: '${expectedTitle}'`));

		console.log(chalk.green('🚀 Starting actual TUI App...'));

		// Spawn the TUI app in a pseudo-terminal
		const app = spawn('npm', ['start'], {
			stdio: ['pipe', 'pipe', 'pipe'],
			env: process.env
		});

		let output = '';
		let found = false;
		const startTime = Date.now();
		const maxWaitSeconds = 20;

		// Monitor progress and handle timeout
		const checkInterval = setInterval(() => {
			const elapsed = Math.floor((Date.now() - startTime) / 1000);
			console.log(chalk.gray(`📺 Checking at ${elapsed}s...`));

			if (elapsed >= maxWaitSeconds) {
				console.log(chalk.red(`❌ FAIL: Task '${expectedTitle}' not found after ${elapsed}s`));
				console.log(chalk.yellow('💡 Output received:'));
				console.log(chalk.gray(output.slice(0, 500)));
				clearInterval(checkInterval);
				app.kill();
				process.exit(1);
			}
		}, 1000);

		// Capture all output from the TUI
		app.stdout.on('data', (data) => {
			const text = data.toString();
			output += text;
			
			// Check if we found the task
			if (!found && text.includes(expectedTitle)) {
				found = true;
				const elapsed = Math.floor((Date.now() - startTime) / 1000);
				console.log(chalk.green(`✅ SUCCESS: Task '${expectedTitle}' found in TUI output`));
				console.log(chalk.green(`🎉 PASS: TUI test completed in ${elapsed}s`));
				clearInterval(checkInterval);
				app.kill();
				process.exit(0);
			}
		});

		app.stderr.on('data', (data) => {
			const text = data.toString();
			// Ignore Ditto logs and debug output, but still capture relevant TUI output
			if (!text.includes('ditto') && !text.includes('INFO') && !text.includes('WARN') && !text.includes('node_modules')) {
				output += text;
			}
		});

		app.on('close', (code) => {
			clearInterval(checkInterval);
			const elapsed = Math.floor((Date.now() - startTime) / 1000);
			
			if (found) {
				console.log(chalk.green(`🎉 PASS: TUI test completed in ${elapsed}s`));
				process.exit(0);
			} else {
				console.log(chalk.red(`❌ FAIL: App exited without finding task (code: ${code})`));
				process.exit(1);
			}
		});

	} catch (error) {
		console.log(chalk.red(`💥 FAIL: ${error.message}`));
		process.exit(1);
	}
}

main();