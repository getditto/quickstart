# KMP iOS Appium Tests

Appium-based UI tests for the Kotlin Multiplatform iOS app using BrowserStack.

## Prerequisites

- Xcode installed (for building .ipa)
- Java 17+
- Gradle
- iOS app built and archived as .ipa

## Local Testing Setup

### 1. Install Appium

```bash
npm install -g appium
appium driver install xcuitest
```

### 2. Build iOS App

Build the app in Xcode or use:

```bash
cd ../iosApp
xcodebuild -project iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator -derivedDataPath ./build
```

### 3. Seed a Test Document

Use the Ditto API to insert a test document:

```bash
# Set your Ditto credentials
export DITTO_API_KEY="your_api_key"
export DITTO_API_URL="your_app_id.cloud.ditto.live"

# Create test task
TEST_TASK_TITLE="test_task_$(date +%s)"
curl -X POST \
  -H 'Content-type: application/json' \
  -H "Authorization: Bearer $DITTO_API_KEY" \
  -d "{
    \"statement\": \"INSERT INTO tasks DOCUMENTS (:newTask) ON ID CONFLICT DO UPDATE\",
    \"args\": {
      \"newTask\": {
        \"_id\": \"${TEST_TASK_TITLE}\",
        \"title\": \"${TEST_TASK_TITLE}\",
        \"done\": false,
        \"deleted\": false
      }
    }
  }" \
"https://${DITTO_API_URL}/api/v4/store/execute"

echo "Created test task: $TEST_TASK_TITLE"
```

### 4. Run Appium Server

```bash
appium
```

### 5. Run Tests

```bash
export GITHUB_TEST_DOC_ID="test_task_1234567890"
../gradlew test
```

## BrowserStack Testing

The test is configured for BrowserStack with the following capabilities:

- **Platform**: iOS 17.0
- **Device**: iPhone 15
- **Automation**: XCUITest via Appium

### BrowserStack Environment Variables

Set these in your CI/GitHub Actions:

```bash
export BROWSERSTACK_USERNAME="your_username"
export BROWSERSTACK_ACCESS_KEY="your_access_key"
export BROWSERSTACK_APP_URL="bs://app_id_from_upload"
export GITHUB_TEST_DOC_ID="test_task_name_from_seeding"
export SYNC_MAX_WAIT_SECONDS="60"  # Optional, defaults to 30
```

## Test Strategy

The test uses multiple XPath strategies to find synced tasks in the Compose Multiplatform iOS UI:

1. Exact text match on `XCUIElementTypeStaticText` by `@name`
2. Label match on `XCUIElementTypeStaticText` by `@label`
3. Partial match using `contains()` on both `@name` and `@label`
4. Broad search across all element types

This compensates for Compose Multiplatform's limited accessibility API exposure on iOS.

## Known Issues & Important Notes

### iOS Accessibility Synchronization

**Critical**: Compose Multiplatform iOS accessibility tree is **only synchronized when iOS Accessibility Services are running**. This means:

- ✅ **BrowserStack/Real Devices**: Works correctly - Appium activates Accessibility Services automatically
- ⚠️ **Local Simulator Testing**: May show `accessible="false"` on all elements unless you:
  - Enable VoiceOver on the simulator during testing
  - Use Accessibility Inspector while running tests
  - Connect to a real iOS device instead of simulator

This is **documented behavior** in Compose Multiplatform 1.8.0+:
> "The iOS accessibility tree is synchronized with the UI only when Accessibility Services are running."

Reference: [Compose Multiplatform iOS Accessibility Documentation](https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-ios-accessibility.html)

### Technical Details

- Compose Multiplatform 1.6.0+ includes iOS accessibility support
- `testTag()` correctly maps to `accessibilityIdentifier` when Accessibility Services are active
- Version 1.8.0+ handles accessibility sync automatically (no manual configuration needed)
- The test includes multiple search strategies (accessibility ID, text match, XPath) to handle different scenarios

### Other Issues

- Build memory issues may require increasing Gradle heap: `org.gradle.jvmargs=-Xmx4g` in `gradle.properties`