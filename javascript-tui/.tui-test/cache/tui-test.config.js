//# hash=2ad1a609bf25ab354979367d9f475587
//# sourceMappingURL=tui-test.config.js.map

import {defineConfig} from '@microsoft/tui-test';
export default defineConfig({
	retries: 2,
	trace: true,
	timeout: 60000,
	testDir: 'tests',
	workers: 1,
	testMatch: '**/*_test.js',
});
