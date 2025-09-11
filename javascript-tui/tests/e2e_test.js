#!/usr/bin/env node
import {test, expect} from '@microsoft/tui-test';
import dotenv from 'dotenv';

dotenv.config({path: '../.env'});

/**
 * TUI End-to-End test using @microsoft/tui-test
 * Tests the actual TUI app with proper PTY support in CI
 * Usage: GITHUB_TEST_DOC_TITLE="Task Name" npx @microsoft/tui-test
 */

// Configure test to run the built CLI directly (avoids JSX parsing issues)
test.use({
	program: {
		command: 'node',
		args: ['dist/cli.js'],
		env: process.env,
	},
});

test('TUI displays seeded task from Ditto Cloud', async ({terminal}) => {
	// Get the expected task title from environment
	const expectedTitle = process.env.GITHUB_TEST_DOC_TITLE;

	if (!expectedTitle || expectedTitle.trim() === '') {
		throw new Error('Missing GITHUB_TEST_DOC_TITLE environment variable');
	}

	console.log(`🧪 Testing TUI for task: '${expectedTitle}'`);

	// Wait for the TUI to initialize and connect to Ditto
	// Look for the app initialization or connection indicator
	await expect(terminal.getByText('🟢 Sync Active')).toBeVisible({
		timeout: 30000, // 30 second timeout for Ditto connection
	});

	// Now check if our seeded task appears in the TUI
	await expect(terminal.getByText(expectedTitle)).toBeVisible({
		timeout: 10000, // 10 second timeout for task sync
	});

	console.log(`✅ SUCCESS: Task '${expectedTitle}' found in TUI`);
});
