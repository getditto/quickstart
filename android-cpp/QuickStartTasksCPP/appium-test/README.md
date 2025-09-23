# Appium E2E Tests for Android CPP Tasks App

This directory contains **true Appium E2E tests** that launch the app externally (like a user would) and are fully compatible with BrowserStack.

## What This Test Does

- 🚀 **Launches the app externally** using Appium WebDriver (not as an instrumentation test)
- 🔍 **Searches for a task name** provided via environment variable
- 📱 **Works on real devices** and emulators
- ☁️ **BrowserStack compatible** for cloud testing
- 🎥 **Video recording** and debug logs supported

## Local Testing

### Prerequisites

1. Install Appium:
```bash
npm install -g appium@next
appium driver install uiautomator2
```

2. Start Appium server:
```bash
appium --port 4723
```

3. Build and install the app on your device/emulator:
```bash
cd ../
./gradlew installDebug
```

### Run the test locally:

```bash
cd appium-test
export GITHUB_TEST_DOC_ID="My Test Task"
./gradlew test
```

## BrowserStack Testing

### Prerequisites

1. Upload your APK to BrowserStack:
```bash
curl -u "YOUR_USERNAME:YOUR_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
  -F "file=@../app/build/outputs/apk/debug/app-debug.apk"
```

2. Update `browserstack.yml` with the returned app ID

3. Set BrowserStack credentials:
```bash
export BROWSERSTACK_USERNAME="your_username"
export BROWSERSTACK_ACCESS_KEY="your_access_key"
```

### Run on BrowserStack:

```bash
# Using BrowserStack CLI
npx browserstack-cypress run

# Or using gradle with BrowserStack Java SDK
./gradlew test -Dappium.server.url="https://your_username:your_access_key@hub-cloud.browserstack.com/wd/hub"
```

## Environment Variables

- `GITHUB_TEST_DOC_ID` - The task name to search for in the app
- `BROWSERSTACK_USERNAME` - Your BrowserStack username
- `BROWSERSTACK_ACCESS_KEY` - Your BrowserStack access key
- `appium.server.url` - Appium server URL (default: http://127.0.0.1:4723)

## Test Strategy

The test uses multiple strategies to find the task:
1. Exact text match: `@text='Task Name'`
2. Contains text match: `contains(@text, 'Task Name')`
3. Waits up to 12 seconds with 1-second retries
4. Dumps page source for debugging if task not found

## Files

- `AppiumE2ETest.kt` - Main Appium test class
- `build.gradle.kts` - Dependencies and test configuration
- `browserstack.yml` - BrowserStack configuration
- `README.md` - This documentation

## Key Differences from Instrumentation Tests

| Aspect | This Appium Test | Instrumentation Test |
|--------|------------------|---------------------|
| App Launch | External (like user) | Internal (test runner) |
| Dependencies | Appium WebDriver | Android Test APIs |
| BrowserStack | Native support | Requires adaptation |
| Real E2E | ✅ True E2E | ❌ Component testing |
| Cross-platform | ✅ Works anywhere | ❌ Android only |