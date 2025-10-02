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

✅ **iOS app builds successfully** with increased heap memory (4GB)
```
gradle.properties: org.gradle.jvmargs = -Xmx4096M
BUILD SUCCESSFUL
```

## Critical iOS Accessibility Limitation

⚠️ **Compose Multiplatform 1.8.0 iOS accessibility is fundamentally broken for automated testing**

### The Problem

Compose Multiplatform iOS **does not populate the accessibility tree** unless iOS Accessibility Services (VoiceOver) are actively running. This makes Appium/XCUITest testing impossible in most environments.

**Symptoms:**
- Accessibility tree shows only generic `XCUIElementTypeOther` elements
- All elements have `accessible="false"` with no names or labels
- Only 1 accessible element: the app container itself
- Appium cannot find any Compose UI elements

**Example output:**
```
📋 Found 13 total elements
📋 Found 1 accessible/named elements
  ✓ XCUIElementTypeApplication: name=QuickStartTasks (accessible=false)
⚠️ No UI elements are accessible
```

### Root Cause

In **Compose Multiplatform 1.8.0**, the `AccessibilitySyncOptions.Always` API was removed:
- **Versions 1.6.0-1.7.x**: Had `AccessibilitySyncOptions.Always(debugLogger = null)` to force accessibility tree synchronization
- **Version 1.8.0+**: Removed this API; accessibility tree is now "lazy loaded" only when iOS Accessibility Services detect activity

From official release notes:
> "AccessibilitySyncOptions removed. The accessibility tree is built on demand"

**This creates a catch-22:**
1. Appium needs the accessibility tree to find elements
2. The tree only populates when accessibility services are active
3. iOS simulators don't activate accessibility services for Appium
4. Result: Empty accessibility tree, tests fail

### What We Tried

❌ **Attempted Solutions (all failed):**
1. Downgrading to Compose 1.6.11, 1.7.0, 1.7.3 - `AccessibilitySyncOptions` API doesn't exist in any version
2. Enabling VoiceOver programmatically in simulator - Didn't populate the tree
3. Using UIAccessibility APIs from Kotlin/Native - Compose controls the tree, not UIKit
4. Testing on BrowserStack real devices - **App crashes immediately** (unsigned Release build)
5. Adding explicit `semantics { contentDescription = ... }` - No effect on accessibility tree

✅ **What Works:**
- App builds and runs correctly
- UI renders perfectly
- Manual testing works fine

❌ **What Doesn't Work:**
- Automated testing with Appium on iOS simulator (empty accessibility tree)
- Automated testing on real devices (app crashes)
- Any XCUITest-based automation

### Technical Details

**iOS Simulator Test Results:**
```xml
<XCUIElementTypeApplication accessible="false">
  <XCUIElementTypeWindow accessible="false">
    <XCUIElementTypeOther accessible="false">
      <XCUIElementTypeOther accessible="false">
        <!-- 10 more generic XCUIElementTypeOther elements -->
        <!-- NO Text elements, NO Buttons, NO accessible names -->
      </XCUIElementTypeOther>
    </XCUIElementTypeOther>
  </XCUIElementTypeWindow>
</XCUIElementTypeApplication>
```

**Expected with working accessibility:**
```xml
<XCUIElementTypeApplication accessible="true">
  <XCUIElementTypeWindow>
    <XCUIElementTypeStaticText name="Task Title" accessible="true"/>
    <XCUIElementTypeButton name="Add Task" accessible="true"/>
    <!-- Actual UI elements with names/labels -->
  </XCUIElementTypeWindow>
</XCUIElementTypeApplication>
```

### Conclusion

**iOS automated testing is currently impossible with Compose Multiplatform 1.8.0** due to:
1. Removed `AccessibilitySyncOptions.Always` API
2. Lazy accessibility tree loading that requires active iOS Accessibility Services
3. Real device testing blocked by app crashes (code signing issues)

**This appears to be a regression in Compose Multiplatform 1.8.0.** Related JetBrains issues:
- CMP-7200: "Accessibility Inspection Tools Not Identifying Elements in iOS App"
- CMP-5635: "Compose Multiplatform - iOS accessibility tree"
- GitHub #4401: "VoiceOver only works with AccessibilitySyncOptions.Always"

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