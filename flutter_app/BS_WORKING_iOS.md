# BrowserStack Flutter iOS Integration Tests - Working Setup

## Prerequisites
- Xcode installed
- iOS development environment set up
- BrowserStack account with credentials

## iOS Test Setup

### 1. Test Runner File
Already created at: `ios/Runner/RunnerTests/RunnerTests.m`
```objc
@import XCTest;
@import integration_test;

INTEGRATION_TEST_IOS_RUNNER(RunnerTests)
```

### 2. Podfile Configuration
Already configured in `ios/Podfile`:
```ruby
target 'Runner' do
  # ... existing configuration ...
  target 'RunnerTests' do
    inherit! :search_paths
  end
end
```

## Build Process

### Step 1: Build Flutter App for iOS
```bash
flutter build ios integration_test/app_test.dart --release \
  --dart-define=INTEGRATION_TEST_MODE=true \
  --dart-define="TASK_TO_FIND=GitHub Test Task Android CPP 1756824079138"
```

### Step 2: Build iOS Test Package
```bash
cd ios

# Build for testing
xcodebuild -workspace Runner.xcworkspace \
  -scheme Runner \
  -config Flutter/Release.xcconfig \
  -derivedDataPath ../build/ios_integration \
  -sdk iphoneos \
  build-for-testing

cd ..
```

### Step 3: Create Test Package ZIP
```bash
cd build/ios_integration/Build/Products

# Find the xctestrun file (e.g., Runner_iphoneos16.2-arm64.xctestrun)
# Create zip with both Release-iphoneos folder and xctestrun file
zip -r ios_test_package.zip Release-iphoneos *.xctestrun
```

### Step 4: Upload to BrowserStack
```bash
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/ios/test-package" \
  -F "file=@build/ios_integration/Build/Products/ios_test_package.zip"
```

Response example:
```json
{
  "test_package_name": "ios_test_package.zip",
  "test_package_url": "bs://3f8ca850476a7c26d4698225e32b353c83cac7ed",
  "test_package_id": "3f8ca850476a7c26d4698225e32b353c83cac7ed",
  "uploaded_at": "2025-09-26 14:00:00 UTC",
  "expiry": "2025-10-26 14:00:00 UTC",
  "framework": "flutter-integration-tests"
}
```

### Step 5: Execute Tests
```bash
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/ios/build" \
  -d '{"testPackage": "bs://TEST_PACKAGE_ID", "devices": ["iPhone 14-16.0"], "project": "Ditto Flutter"}' \
  -H "Content-Type: application/json"
```

## Environment Variables

The same environment variables work for iOS:
- `INTEGRATION_TEST_MODE=true` - Skips permission dialogs
- `TASK_TO_FIND=<task_name>` - Configurable task to search for (defaults to "GitHub Test Task Android CPP 1756824079138")

These are passed via `--dart-define` in the flutter build command.

## Local Testing (Optional)
```bash
# Get device ID
xcrun simctl list devices

# Run tests locally
xcodebuild test-without-building \
  -xctestrun "build/ios_integration/Build/Products/*.xctestrun" \
  -destination id=<DEVICE_ID>
```

## Supported Devices
Common iOS devices for BrowserStack:
- iPhone 14-16.0
- iPhone 13-15.0
- iPhone 12-14.0
- iPhone 11-13.0
- iPad Pro 12.9-16.0

## Notes
- Unlike Android, iOS requires creating a test package ZIP containing both the test runner and app
- The test package includes the entire Release-iphoneos folder and the xctestrun file
- Environment variables are handled the same way as Android (via --dart-define)
- No need for base64 encoding like Android Gradle