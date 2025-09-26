# BrowserStack Flutter Integration Tests - WORKING SOLUTION

## ✅ What Actually Works (Build ID: c57470f84078c1c128fe2876c59c44656ed0b339)

### Exact Build Process (MUST follow this order)

```bash
# From flutter_app/android directory
cd android

# Step 1: Build test APK wrapper FIRST
./gradlew app:assembleAndroidTest

# Step 2: Build debug APK with integration test target and environment variables
./gradlew app:assembleDebug \
  -Ptarget="/Users/teodorc/Projects/worktrees/teodorciuraru/sdks-1611-flutter_app-add-integration-test-support/flutter_app/integration_test/app_test.dart" \
  -Pdart-defines="SU5URUdSQVRJT05fVEVTVF9NT0RFPXRydWU=,VEFTS19UT19GSU5EPUJhc2ljIFRlc3QgVGFzaw=="
```

### Environment Variables Base64 Encoding
- `INTEGRATION_TEST_MODE=true` → `SU5URUdSQVRJT05fVEVTVF9NT0RFPXRydWU=`
- `TASK_TO_FIND=Basic Test Task` → `VEFTS19UT19GSU5EPUJhc2ljIFRlc3QgVGFzaw==`
- Combined: `"SU5URUdSQVRJT05fVEVTVF9NT0RFPXRydWU=,VEFTS19UT19GSU5EPUJhc2ljIFRlc3QgVGFzaw=="`

### Upload Process (EXACT ORDER MATTERS)

```bash
# From flutter_app directory
cd ..

# Upload App APK FIRST
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/app" \
  -F "file=@build/app/outputs/apk/debug/app-debug.apk"

# Upload Test Suite APK SECOND
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/test-suite" \
  -F "file=@build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
```

### Execute Tests

```bash
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/build" \
  -d '{"app": "bs://APP_ID", "testSuite": "bs://TEST_SUITE_ID", "devices": ["Google Pixel 7-13.0"], "project": "Ditto Flutter"}' \
  -H "Content-Type: application/json"
```

## Working APK IDs (Reference)
- **App**: `bs://7ac2e0e0ce26c0d005dae0f1223bc208645d4336`
- **Test Suite**: `bs://eacf6224b2a1938b8de262321c7532645482d904`

## Critical Success Factors

1. **Build Order**: Test APK wrapper MUST be built before app APK
2. **Environment Variables**: Must be base64 encoded and comma-separated
3. **Upload Order**: App first, then test suite
4. **Matching Pairs**: Both APKs must be built from same codebase session
5. **No Extra Parameters**: Don't add deviceLogs, networkLogs, etc.

## Test Code Requirements

- **MainActivityTest.java**: Must exist in `android/app/src/androidTest/java/com/example/flutter_quickstart/`
- **build.gradle**: Must have correct test runner and dependencies configured
- **Integration Test**: `integration_test/app_test.dart` must check for `INTEGRATION_TEST_MODE` and `TASK_TO_FIND`

## What This Achieves

✅ No permission dialogs (INTEGRATION_TEST_MODE=true works)
✅ App loads with Ditto sync working
✅ Tasks are visible including seeded test tasks
✅ Flutter integration tests can run on BrowserStack devices

## If Tests Hang or Fail

1. Check APK pair matching (rebuild both together)
2. Verify environment variables are correctly base64 encoded
3. Ensure build order: test wrapper first, then app with target
4. Use exact configuration (no extra parameters)