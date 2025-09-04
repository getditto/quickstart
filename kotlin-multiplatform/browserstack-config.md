# BrowserStack Android Integration Testing

This guide shows how to run Android Espresso integration tests on BrowserStack App Automate with seeded test documents.

## Prerequisites

1. BrowserStack account with App Automate enabled
2. Android APK built with `./gradlew assembleDebug`
3. Android Test APK built with `./gradlew assembleDebugAndroidTest`
4. Environment variables: `BROWSERSTACK_USERNAME` and `BROWSERSTACK_ACCESS_KEY`

## Build Commands

```bash
# Build the main APK
./gradlew assembleDebug

# Build the instrumentation test APK
./gradlew assembleDebugAndroidTest
```

The APKs will be generated at:
- Main APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
- Test APK: `composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk`

## Upload APKs to BrowserStack

### 1. Upload Main APK
```bash
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
  -F "file=@composeApp/build/outputs/apk/debug/composeApp-debug.apk" \
  -F "custom_id=ditto_quickstart_main"
```

### 2. Upload Test APK
```bash
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/test-suite" \
  -F "file=@composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk" \
  -F "custom_id=ditto_quickstart_tests"
```

## Execute Tests with Seeded Document

### Basic Test Execution
```bash
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/build" \
  -H "Content-Type: application/json" \
  -d '{
    "app": "ditto_quickstart_main",
    "testSuite": "ditto_quickstart_tests",
    "devices": [
      "Google Pixel 7-13.0",
      "Samsung Galaxy S22-12.0"
    ],
    "project": "Ditto KMP Integration Tests",
    "buildName": "Android Espresso Tests - Build #1"
  }'
```

### Test Execution with Seeded Document
```bash
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/build" \
  -H "Content-Type: application/json" \
  -d '{
    "app": "ditto_quickstart_main",
    "testSuite": "ditto_quickstart_tests",
    "devices": [
      "Google Pixel 7-13.0",
      "Samsung Galaxy S22-12.0"
    ],
    "project": "Ditto KMP Integration Tests",
    "buildName": "Android Espresso Tests with Seeded Document",
    "instrumentationLogs": true,
    "deviceLogs": true,
    "networkLogs": true,
    "customOptions": {
      "github_test_doc_id": "Clean the kitchen"
    }
  }'
```

### Advanced Configuration with Multiple Test Documents
```bash
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/build" \
  -H "Content-Type: application/json" \
  -d '{
    "app": "ditto_quickstart_main",
    "testSuite": "ditto_quickstart_tests",
    "devices": [
      "Google Pixel 7-13.0",
      "Samsung Galaxy S22-12.0",
      "Samsung Galaxy S21-11.0"
    ],
    "project": "Ditto KMP Integration Tests",
    "buildName": "Multi-Document Test Suite",
    "instrumentationLogs": true,
    "deviceLogs": true,
    "networkLogs": true,
    "customOptions": {
      "github_test_doc_id": "Clean the kitchen",
      "ditto_app_id": "your-app-id-here",
      "ditto_playground_token": "your-token-here"
    },
    "class": [
      "integration.AndroidIntegrationTest#testAppLaunchesSuccessfully",
      "integration.AndroidIntegrationTest#testGitHubTestDocumentSyncs"
    ]
  }'
```

## GitHub Actions Workflow Integration

Create `.github/workflows/browserstack-android.yml`:

```yaml
name: BrowserStack Android Integration Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  browserstack-tests:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v4
    
    - name: Set up JDK 17
      uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    
    - name: Setup Gradle
      uses: gradle/gradle-build-action@v3
    
    - name: Build Android APKs
      run: |
        ./gradlew assembleDebug
        ./gradlew assembleDebugAndroidTest
    
    - name: Upload Main APK to BrowserStack
      run: |
        APP_UPLOAD_RESPONSE=$(curl -s -u "${{ secrets.BROWSERSTACK_USERNAME }}:${{ secrets.BROWSERSTACK_ACCESS_KEY }}" \
          -X POST "https://api-cloud.browserstack.com/app-automate/upload" \
          -F "file=@composeApp/build/outputs/apk/debug/composeApp-debug.apk" \
          -F "custom_id=ditto_quickstart_main_${{ github.run_id }}")
        echo "APP_UPLOAD_RESPONSE=$APP_UPLOAD_RESPONSE" >> $GITHUB_ENV
    
    - name: Upload Test APK to BrowserStack
      run: |
        TEST_UPLOAD_RESPONSE=$(curl -s -u "${{ secrets.BROWSERSTACK_USERNAME }}:${{ secrets.BROWSERSTACK_ACCESS_KEY }}" \
          -X POST "https://api-cloud.browserstack.com/app-automate/espresso/test-suite" \
          -F "file=@composeApp/build/outputs/apk/androidTest/debug/composeApp-debug-androidTest.apk" \
          -F "custom_id=ditto_quickstart_tests_${{ github.run_id }}")
        echo "TEST_UPLOAD_RESPONSE=$TEST_UPLOAD_RESPONSE" >> $GITHUB_ENV
    
    - name: Execute BrowserStack Tests
      run: |
        BUILD_RESPONSE=$(curl -s -u "${{ secrets.BROWSERSTACK_USERNAME }}:${{ secrets.BROWSERSTACK_ACCESS_KEY }}" \
          -X POST "https://api-cloud.browserstack.com/app-automate/espresso/build" \
          -H "Content-Type: application/json" \
          -d '{
            "app": "ditto_quickstart_main_${{ github.run_id }}",
            "testSuite": "ditto_quickstart_tests_${{ github.run_id }}",
            "devices": [
              "Google Pixel 7-13.0",
              "Samsung Galaxy S22-12.0"
            ],
            "project": "Ditto KMP Integration Tests",
            "buildName": "GitHub Actions Build #${{ github.run_number }}",
            "instrumentationLogs": true,
            "deviceLogs": true,
            "networkLogs": true,
            "customOptions": {
              "github_test_doc_id": "Clean the kitchen"
            }
          }')
        
        BUILD_ID=$(echo $BUILD_RESPONSE | jq -r '.build_id')
        echo "BrowserStack Build ID: $BUILD_ID"
        echo "BUILD_ID=$BUILD_ID" >> $GITHUB_ENV
    
    - name: Wait for Test Completion
      run: |
        echo "Waiting for BrowserStack tests to complete..."
        while true; do
          STATUS=$(curl -s -u "${{ secrets.BROWSERSTACK_USERNAME }}:${{ secrets.BROWSERSTACK_ACCESS_KEY }}" \
            "https://api-cloud.browserstack.com/app-automate/espresso/builds/${{ env.BUILD_ID }}" | \
            jq -r '.status')
          
          echo "Current status: $STATUS"
          
          if [ "$STATUS" = "done" ]; then
            echo "Tests completed successfully!"
            break
          elif [ "$STATUS" = "error" ] || [ "$STATUS" = "failed" ]; then
            echo "Tests failed with status: $STATUS"
            exit 1
          fi
          
          sleep 30
        done
    
    - name: Get Test Results
      run: |
        curl -s -u "${{ secrets.BROWSERSTACK_USERNAME }}:${{ secrets.BROWSERSTACK_ACCESS_KEY }}" \
          "https://api-cloud.browserstack.com/app-automate/espresso/builds/${{ env.BUILD_ID }}" | \
          jq '.'
```

## Environment Variables Setup

Add these secrets to your GitHub repository:
- `BROWSERSTACK_USERNAME`: Your BrowserStack username
- `BROWSERSTACK_ACCESS_KEY`: Your BrowserStack access key

## Test Configuration Options

The integration tests support these instrumentation arguments:

1. **github_test_doc_id**: The seeded document title to search for
2. **ditto_app_id**: Ditto application ID (optional override)
3. **ditto_playground_token**: Ditto playground token (optional override)

## Device Selection

Popular Android devices on BrowserStack:
- `Google Pixel 7-13.0`
- `Samsung Galaxy S22-12.0`
- `Samsung Galaxy S21-11.0`
- `OnePlus 9-11.0`
- `Google Pixel 6-12.0`

## Troubleshooting

### Common Issues:
1. **APK Upload Failed**: Check file paths and BrowserStack credentials
2. **Test Failed to Start**: Verify both APKs are compatible
3. **Instrumentation Args Not Passed**: Use `customOptions` in the API call
4. **Timeout Issues**: Increase sync timeout in test code
5. **Permission Issues**: Ensure Android app requests necessary permissions

### Debug Commands:
```bash
# Check uploaded apps
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  "https://api-cloud.browserstack.com/app-automate/recent_apps"

# Check test suites
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  "https://api-cloud.browserstack.com/app-automate/espresso/recent_test_suites"

# Get build details
curl -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
  "https://api-cloud.browserstack.com/app-automate/espresso/builds/BUILD_ID"
```

## Test Results

After execution, you can:
1. View results in BrowserStack dashboard
2. Download device logs and screenshots
3. Access session recordings
4. Get detailed test reports

The tests will validate:
- ✅ App launches successfully on real devices
- ✅ Seeded documents sync and appear in UI
- ✅ Memory usage is within acceptable limits
- ✅ Basic app functionality works across devices