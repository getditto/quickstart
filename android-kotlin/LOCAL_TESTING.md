# Local Integration Testing for Android Kotlin

This document explains how to test Ditto Cloud synchronization locally using real Android devices or emulators.

## Overview

The local integration testing framework allows you to:
- 🌱 **Seed test documents** directly into Ditto Cloud using your `.env` credentials
- 🧪 **Run integration tests** on local Android devices/emulators
- ✅ **Verify sync functionality** by checking that seeded documents appear in the mobile app UI
- 🚀 **Replicate CI/CD testing** locally for debugging and development

## Prerequisites

### 1. Environment Setup

#### 🚨 Security Notice
**NEVER commit the `.env` file to git!** It contains sensitive credentials and is already in `.gitignore`.

Create a `.env` file in the repository root with your Ditto credentials:

```bash
# Required for app functionality
DITTO_APP_ID=your_app_id
DITTO_PLAYGROUND_TOKEN=your_token  
DITTO_AUTH_URL=your_auth_url
DITTO_WEBSOCKET_URL=your_websocket_url

# Required for seeding test documents (usually GitHub secrets)
DITTO_API_KEY=your_api_key
DITTO_API_URL=your_api_url
```

#### Security Best Practices:
- ✅ `.env` file is in `.gitignore` and should never be committed
- ✅ CI/CD uses GitHub secrets directly, not `.env` files  
- ✅ Local development uses `.env` for convenience
- ❌ Never share `.env` files in chat, email, or documentation
- ❌ Never hardcode credentials in source code

> **Note**: `DITTO_API_KEY` and `DITTO_API_URL` are typically stored as GitHub secrets for CI/CD. Contact your team to get these credentials for local testing.

### 2. System Requirements

- **Python 3** with `requests` library
- **Android Studio** or Android SDK
- **Connected Android device** or **running emulator**
- **ADB** (Android Debug Bridge) in PATH

### 3. Check Device Connection

```bash
adb devices
```

You should see your device listed as `device` (not `unauthorized`).

## Testing Methods

### Method 1: One-Command Full Test (Recommended)

The easiest way to run a complete integration test:

```bash
# From android-kotlin/QuickStartTasks/
./gradlew testLocalIntegration
```

This will:
1. Seed a test document in Ditto Cloud
2. Build the Android test APKs
3. Run the integration test on your connected device
4. Verify the seeded document appears in the app

### Method 2: Step-by-Step Testing

For more control over the testing process:

```bash
# 1. Seed a test document
./gradlew seedTestDocument

# 2. Run the integration test  
./gradlew runSyncIntegrationTest

# 3. Or run a quick test with existing document
./gradlew testLocalQuick
```

### Method 3: Manual Script Usage

For advanced usage and custom parameters:

```bash
# From android-kotlin/ directory

# Full test with custom document
scripts/test-local.sh --doc-id my_test_123 --title "My Custom Test"

# Only seed a document
scripts/test-local.sh --seed-only --verify

# Only run tests (using previously seeded document)
scripts/test-local.sh --test-only

# Clean build and full test
scripts/test-local.sh --clean
```

## Understanding the Test Flow

### 1. Document Seeding Phase
- Creates a test document in Ditto Cloud with structure:
  ```json
  {
    "_id": "local_test_1693839245",
    "title": "Local Test Task - 2023-09-04 15:20:45",
    "done": false,
    "deleted": false
  }
  ```
- Verifies document was created successfully
- Provides document ID for testing

### 2. Integration Test Phase
- Launches the Android Kotlin Tasks app
- Waits for Ditto SDK to establish connection and sync
- Searches the UI for the seeded test document
- Verifies document appears in the task list within 30 seconds
- Runs additional stability and UI functionality tests

### 3. Test Verification
- **Success**: Document found in UI within timeout period
- **Failure**: Detailed logging shows what was found in the UI for debugging

## Troubleshooting

### Common Issues

#### 1. "No Android device detected"
```bash
# Check connected devices
adb devices

# Start an emulator or connect a physical device
# Make sure USB debugging is enabled on physical devices
```

#### 2. "DITTO_API_KEY not found"
```bash
# Add API credentials to your .env file
echo "DITTO_API_KEY=your_key_here" >> .env
echo "DITTO_API_URL=your_url_here" >> .env
```

#### 3. "Python requests library not found"
```bash
# Install Python requests
pip3 install requests
```

#### 4. "Document not found after 30 seconds"
- Check if the app has internet connectivity
- Verify Ditto credentials are correct
- Check if sync is enabled in the app UI (toggle in top right)
- Look at the test output for debugging information

### Debug Information

The integration test provides extensive debugging output:

```
🔍 Looking for GitHub test document: local_test_1693839245
🔍 Looking for GitHub Run ID: 1693839245
⏳ Attempt 15: Document not found yet...
✅ Found GitHub test task containing run ID: Local Test Task - 2023-09-04 15:20:45
🎉 Successfully verified GitHub test document synced from Ditto Cloud!
```

### Viewing Test Reports

After running tests, detailed reports are available:
- **HTML Reports**: `app/build/reports/androidTests/connected/`
- **XML Results**: `app/build/outputs/androidTest-results/`
- **Logcat**: Recent app logs are shown on test failure

## Advanced Usage

### Custom Test Documents

```bash
# Create document with specific ID
scripts/test-local.sh --doc-id "my_integration_test_$(date +%s)"

# Create document with custom title
scripts/test-local.sh --title "Integration Test $(date)"

# Combine both
scripts/test-local.sh --doc-id custom_123 --title "Custom Test Task"
```

### Running Specific Test Classes

```bash
# Run only the sync integration test
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=live.ditto.quickstart.tasks.TasksSyncIntegrationTest

# Run a specific test method
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=live.ditto.quickstart.tasks.TasksSyncIntegrationTest \
  -Pandroid.testInstrumentationRunnerArguments.method=testGitHubDocumentSyncFromCloud
```

### Environment Variables

You can override test behavior with environment variables:

```bash
# Set custom document ID
export GITHUB_TEST_DOC_ID=my_custom_test_123
./gradlew runSyncIntegrationTest

# Use different .env file
python3 scripts/seed-test-document.py --env-file .env.local
```

## Integration with Development Workflow

### 1. Feature Development
```bash
# Test your changes locally before CI
scripts/test-local.sh --clean
```

### 2. Debugging Sync Issues  
```bash
# Seed document and check manually in app
scripts/test-local.sh --seed-only
# Then launch app manually to inspect sync behavior
```

### 3. CI/CD Validation
```bash
# Replicate CI conditions locally
scripts/test-local.sh --verify --clean
```

## Available Gradle Tasks

| Task | Description |
|------|-------------|
| `seedTestDocument` | Seed a test document in Ditto Cloud |
| `runSyncIntegrationTest` | Run integration test with connected device |
| `testLocalIntegration` | Complete test: seed + run integration test |
| `testLocalQuick` | Quick test using existing seeded document |

View all custom tasks:
```bash
./gradlew tasks --group testing
```

## Next Steps

- **BrowserStack Testing**: Use the same seeding approach for BrowserStack CI/CD
- **Custom Test Scenarios**: Modify the seed script for different test cases
- **Automated Testing**: Integrate with your development scripts
- **Team Sharing**: Share `.env` template with required API credentials

For questions about credentials or setup, contact your development team.