# iOS BrowserStack Integration Setup Guide

## Current Status ✅
- **iOS app builds successfully** and creates IPA 
- **Android BrowserStack works** with real device testing
- **KMP native tests work** on all platforms via Gradle  
- **iOS UI test code created** at `iosAppUITests/iosAppUITests.swift`

## What's Missing 🔧
The KMP iOS project needs a **UI Testing Bundle target** in Xcode to enable BrowserStack XCUITest integration.

## Quick Setup Steps (5 minutes)

### 1. Open Xcode Project
```bash
cd kotlin-multiplatform/iosApp
open iosApp.xcodeproj
```

### 2. Add iOS UI Testing Bundle Target
1. In Xcode: **File > New > Target...**
2. Select **iOS** tab
3. Choose **iOS UI Testing Bundle**
4. Configure:
   - **Product Name**: `iosAppUITests` (exactly this name)
   - **Target to be Tested**: Select `iosApp`
   - **Language**: Swift
   - Click **Finish**

### 3. Replace Xcode's Generated UI Test File

⚠️ **Important**: After step 2, Xcode will generate a NEW test file (usually `iosAppUITestsUITests.swift`).

**Option A: Automated (Recommended)**
```bash
cd kotlin-multiplatform/iosApp
./replace_ui_test_file.sh
```

**Option B: Manual**  
1. Find the Xcode-generated file (typically `iosAppUITests/iosAppUITestsUITests.swift`)
2. Replace its contents with the code from our prepared file: `iosAppUITests/iosAppUITests.swift`
3. Or simply copy our file and rename it to match what Xcode expects

### 4. Share the Scheme
1. **Product > Scheme > Manage Schemes...**
2. Find **iosAppUITests** scheme
3. Check the **Shared** checkbox ✅
4. Click **Close**

### 5. Verify Setup
Run this to verify the scheme exists:
```bash
xcodebuild -project iosApp.xcodeproj -list
```

You should see:
```
Schemes:
    iosApp
    iosAppUITests  ← This should now appear
```

### 6. Test Locally (Optional)
```bash
# Build UI test bundle (should work without errors)
xcodebuild build-for-testing \
  -project iosApp.xcodeproj \
  -scheme iosAppUITests \
  -configuration Debug \
  -destination 'generic/platform=iOS'
```

## Re-enable CI Workflow ⚡

After completing the Xcode setup, update the workflow:

1. **Edit** `.github/workflows/kotlin-multiplatform-ci.yml`
2. **Find** the `browserstack-ios` job  
3. **Change** `if: false` to `if: needs.build-ios.outputs.ios-build-success == 'true'`

## Expected Result 🎯

After setup, your CI will:

1. ✅ **Build iOS app** successfully  
2. ✅ **Build UI test bundle** with `iosAppUITests` scheme
3. ✅ **Upload to BrowserStack** both app and test bundle
4. ✅ **Run UI tests** on real iOS devices
5. ✅ **Verify document sync** from GitHub Actions → Ditto Cloud → BrowserStack

The UI test will search for the seeded document title (just like the working Swift implementation) and validate the complete end-to-end sync pipeline.

## Files Created 📁

I've already created these files for you:

- `iosAppUITests/iosAppUITests.swift` - Main UI test (follows Swift pattern exactly)  
- `iosAppUITests/Info.plist` - Bundle configuration
- This setup guide

## Troubleshooting 🔍

**If "iosAppUITests" scheme doesn't appear:**
- Make sure you named the target exactly `iosAppUITests`
- Ensure you shared the scheme (step 4 above)
- Try closing and reopening Xcode

**If build-for-testing fails:**
- Verify the UI test target is properly configured 
- Check that the test file compiles without Swift errors
- Ensure iOS deployment target matches between app and test targets

## Alternative: Quick Commit ⚡

If you want to commit the current working state:

```bash
git add .
git commit -m "feat: prepare iOS BrowserStack integration

- Add iOS UI test implementation following Swift pattern
- Create iosAppUITests target setup files  
- Document manual Xcode setup steps

Ready for iOS BrowserStack once UI test target is added in Xcode"
```

The setup is straightforward - just need to add the missing UI test target in Xcode, which takes about 2 minutes through the GUI.