# Local Integration Tests

This directory contains local integration tests for the Kotlin Multiplatform Ditto Tasks app.

## Overview

The tests verify that a seeded Ditto task ID appears in the UI, proving that:
1. The app can receive test parameters
2. Ditto sync is working 
3. The UI displays synced data correctly

## Android Integration Test

**Location**: `src/androidTest/kotlin/com/ditto/quickstart/DittoSeededIdTest.kt`

**Technology**: Compose UI Test framework (not traditional Espresso)

**Setup**: 
1. Ensure Android emulator or device is connected
2. Set `DITTO_TASK_ID` environment variable to an existing task ID
3. Run the test script

**Usage**:
```bash
export DITTO_TASK_ID="existing_task_id_here"
./scripts/run_local_android.sh
```

## iOS Integration Test

**Location**: `iosApp/iosAppUITests/DittoSeededIdUITests.swift`

**Technology**: XCUITest framework

**Setup**:
1. **Manual Xcode setup required first**:
   - Open `iosApp/iosApp.xcodeproj` in Xcode
   - Add a UI Test target named `iosAppUITests`
   - Add `DittoSeededIdUITests.swift` to the test target
2. Ensure iOS Simulator is available
3. Set `DITTO_TASK_ID` environment variable
4. Run the test script

**Usage**:
```bash
export DITTO_TASK_ID="existing_task_id_here"  
./scripts/run_local_ios.sh
```

## Test Input Contract

Both tests use the same pattern:
- **Android**: Reads `DITTO_TASK_ID` from instrumentation arguments
- **iOS**: Reads `DITTO_TASK_ID` from environment variables passed to the app

## Next Steps

These local tests are designed to be the foundation for:
1. CI pipeline integration 
2. BrowserStack device testing
3. Automated test data seeding via Ditto Cloud API

The tests prove the basic sync functionality works before moving to cloud-based testing infrastructure.