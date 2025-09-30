# Kotlin Multiplatform iOS Testing Strategy

## Summary

XCUITest was **replaced with Appium** for iOS UI testing due to Compose Multiplatform compatibility issues.

## Why Not XCUITest?

Compose Multiplatform's `.testTag()` modifier does not bridge to iOS native accessibility identifiers. XCUITest queries returned **zero elements** even though the UI was rendered correctly.

```kotlin
// This works on Android but NOT on iOS with XCUITest
Text(
    text = task.title,
    modifier = Modifier.testTag("task_title_${task.title}")
)
```

iOS accessibility queries found nothing:
```
📋 Total text elements found: 0
⚠️ No text elements found yet, waiting...
```

## Why Appium?

1. **BrowserStack recommended** - Official support for iOS testing
2. **Cross-platform** - Same test framework as android-cpp
3. **Flexible queries** - XPath can find Compose elements by text/label
4. **Working precedent** - android-cpp already uses Appium successfully

## Implementation

### Test Location
- **Module**: `kotlin-multiplatform/appium-test`
- **Test**: `src/test/kotlin/com/ditto/quickstart/kmp/KMPiOSAppiumTest.kt`
- **Build**: `build.gradle.kts` (TestNG + Appium Java Client 9.0.0)

### Test Strategy

The test uses **multiple XPath strategies** to compensate for Compose's limited iOS accessibility:

```kotlin
// Strategy 1: Exact name match
driver.findElement(By.xpath("//XCUIElementTypeStaticText[@name='$testTaskName']"))

// Strategy 2: Label match
driver.findElement(By.xpath("//XCUIElementTypeStaticText[@label='$testTaskName']"))

// Strategy 3: Partial match
driver.findElement(By.xpath("//XCUIElementTypeStaticText[contains(@name, '$testTaskName')]"))

// Strategy 4: Broad search
driver.findElement(By.xpath("//*[contains(@name, '$testTaskName') or contains(@label, '$testTaskName')]"))
```

### BrowserStack Configuration

```kotlin
XCUITestOptions().apply {
    setPlatformName("iOS")
    setDeviceName("iPhone 15")
    setPlatformVersion("17.0")
    setCapability("app", System.getenv("BROWSERSTACK_APP_URL"))
    setCapability("project", "Ditto SDK Kotlin Multiplatform iOS")
    setCapability("build", "Appium E2E Tests - KMP iOS")
    setCapability("automationName", "XCUITest")
    setCapability("autoAcceptAlerts", true)
}
```

## Running Tests

### Local (with Appium server)

```bash
# Install Appium
npm install -g appium
appium driver install xcuitest

# Start Appium
appium

# Seed test document
export GITHUB_TEST_DOC_ID="test_task_$(date +%s)"
# ... use Ditto API to insert document ...

# Run test
cd kotlin-multiplatform
./gradlew :appium-test:test
```

### BrowserStack (CI/CD)

Required environment variables:
```bash
BROWSERSTACK_USERNAME="your_username"
BROWSERSTACK_ACCESS_KEY="your_access_key"
BROWSERSTACK_APP_URL="bs://app_id"
GITHUB_TEST_DOC_ID="seeded_task_title"
SYNC_MAX_WAIT_SECONDS="60"  # Optional, defaults to 30
```

## Build Status

✅ **Appium test module compiles successfully**
```
BUILD SUCCESSFUL in 10s
```

⚠️ **iOS app has Kotlin compiler memory issues** (unrelated to test framework choice)
```
error: java.lang.OutOfMemoryError: Java heap space
```

This is a known KMP build issue, not a testing framework issue.

## Files Changed

**Removed:**
- `kotlin-multiplatform/iosApp/iosAppUITests/` (entire XCUITest directory)
- XCUITest references in `iosApp.xcscheme`

**Added:**
- `kotlin-multiplatform/appium-test/` (new Appium test module)
- `kotlin-multiplatform/appium-test/build.gradle.kts`
- `kotlin-multiplatform/appium-test/src/test/kotlin/com/ditto/quickstart/kmp/KMPiOSAppiumTest.kt`
- `kotlin-multiplatform/appium-test/README.md`
- Updated `kotlin-multiplatform/settings.gradle.kts` to include `:appium-test`

## Next Steps

1. Fix iOS app build memory issues (increase Gradle heap)
2. Build .ipa for BrowserStack upload
3. Create GitHub Actions workflow matching `android-cpp-ci.yml` pattern
4. Test on BrowserStack iOS devices

## References

- BrowserStack Appium iOS Docs: https://www.browserstack.com/docs/app-automate
- Android CPP Appium Tests: `android-cpp/QuickStartTasksCPP/appium-test/`
- Appium Java Client: https://github.com/appium/java-client