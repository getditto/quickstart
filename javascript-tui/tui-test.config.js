import {defineConfig} from '@microsoft/tui-test';

export default defineConfig({
	retries: 2,
	trace: true,
	timeout: 60000, // 60 second timeout for full test
	testDir: 'tests',
	workers: 1, // Run tests sequentially to avoid conflicts
});