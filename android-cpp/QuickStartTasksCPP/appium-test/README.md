# Appium E2E Tests for Android CPP Tasks App

End-to-end Appium tests that verify dynamically seeded tasks appear in the Android CPP Tasks app.

## Local Testing

### Prerequisites
```bash
npm install -g appium
appium --port 4723
export GITHUB_TEST_DOC_ID="Your Task Name"
```

### Run Test
```bash
../gradlew test
```

## BrowserStack Testing

### Environment Variables
```bash
export BROWSERSTACK_USERNAME="your-username"
export BROWSERSTACK_ACCESS_KEY="your-access-key"
export BROWSERSTACK_APP_URL="bs://your-app-id"
export GITHUB_TEST_DOC_ID="Task Name to Find"
```

### Run Test
```bash
../gradlew test
```

## CI/CD Integration

The test integrates with `.github/workflows/android-cpp-browserstack.yml`:
1. Seeds dynamic task document to Ditto Cloud
2. Uploads APK to BrowserStack
3. Runs Appium test to verify seeded task appears
4. Reports pass/fail status

## Test Details

- **Device**: Google Pixel 7 (Android 13.0) on BrowserStack
- **Framework**: Appium + TestNG + Kotlin
- **Verification**: Searches for task by exact and partial text match
- **Features**: Permission handling, BrowserStack status reporting