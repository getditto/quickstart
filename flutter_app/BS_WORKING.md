# BrowserStack Flutter Integration Tests - Working Setup

## ✅ Confirmed Working Configuration (2025-09-26)

### Build IDs
- **Build ID**: `572570ea0e83853297ea3484084add8fa0c3134b` (PASSED)
- **App URL**: `bs://a21422836d4b891f6625e357fc98a3e792941495`
- **Test Suite URL**: `bs://eacf6224b2a1938b8de262321c7532645482d904`

### Key Solution: Single Ditto Instance
The critical fix was ensuring only ONE Ditto instance is created across all tests. Multiple instances cause file locking conflicts.

### Test Structure (`integration_test/app_test.dart`)
```dart
void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Ditto Tasks App Integration Tests', () {
    setUpAll(() async {
      await dotenv.load(fileName: ".env");
    });

    testWidgets('App loads and syncs with Ditto Cloud', (WidgetTester tester) async {
      // Initialize app ONLY ONCE
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Verify UI elements
      expect(find.text('Ditto Tasks'), findsOneWidget);
      expect(find.text('Sync Active'), findsOneWidget);
      expect(find.byIcon(Icons.add_task), findsOneWidget);
      expect(find.byIcon(Icons.clear), findsOneWidget);

      // Wait for sync and verify test document
      await Future.delayed(const Duration(seconds: 5));
      await tester.pumpAndSettle();

      // Task name is configurable via TASK_TO_FIND environment variable
      const testTitle = String.fromEnvironment('TASK_TO_FIND',
          defaultValue: 'GitHub Test Task Android CPP 1756824079138');
      expect(find.text(testTitle), findsOneWidget);
    });
  });
}
```

### Build Process

#### 1. Build Test APK Wrapper
```bash
cd android
./gradlew app:assembleAndroidTest
```

#### 2. Build Debug APK with Integration Test Target
```bash
./gradlew app:assembleDebug \
  -Ptarget="/Users/teodorc/Projects/worktrees/teodorciuraru/sdks-1611-flutter_app-add-integration-test-support/flutter_app/integration_test/app_test.dart" \
  -Pdart-defines="SU5URUdSQVRJT05fVEVTVF9NT0RFPXRydWU=,VEFTS19UT19GSU5EPUdpdEh1YiBUZXN0IFRhc2sgQW5kcm9pZCBDUFAgMTc1NjgyNDA3OTEzOA=="
```

**Environment Variable Encoding:**
- `INTEGRATION_TEST_MODE=true` → `SU5URUdSQVRJT05fVEVTVF9NT0RFPXRydWU=`
- `TASK_TO_FIND=GitHub Test Task Android CPP 1756824079138` → `VEFTS19UT19GSU5EPUdpdEh1YiBUZXN0IFRhc2sgQW5kcm9pZCBDUFAgMTc1NjgyNDA3OTEzOA==`

**To encode a custom task name:**
```bash
echo -n "TASK_TO_FIND=Your Task Name Here" | base64
```

### Upload to BrowserStack

#### 1. Upload App APK
```bash
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/app" \
  -F "file=@build/app/outputs/apk/debug/app-debug.apk"
```

#### 2. Upload Test Suite APK
```bash
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/test-suite" \
  -F "file=@build/app/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
```

#### 3. Execute Tests
```bash
curl -u "teodorciuraru_g4ijva:4hJsNzsc2BXiEiu9ktDt" \
  -X POST "https://api-cloud.browserstack.com/app-automate/flutter-integration-tests/v2/android/build" \
  -d '{"app": "bs://APP_ID", "testSuite": "bs://TEST_SUITE_ID", "devices": ["Google Pixel 7-13.0"], "project": "Ditto Flutter"}' \
  -H "Content-Type: application/json"
```

### Main Code Changes

#### `lib/main.dart` - Skip Permissions in Test Mode
```dart
const isTestMode = bool.fromEnvironment('INTEGRATION_TEST_MODE', defaultValue: false);

if (!kIsWeb && !isTestMode) {
  await [
    Permission.bluetoothScan,
    Permission.bluetoothAdvertise,
    Permission.bluetoothConnect,
    Permission.nearbyWifiDevices,
    Permission.locationWhenInUse,
  ].request();
}
```

### Test Requirements
- **MainActivityTest.java**: Must exist in `android/app/src/androidTest/java/com/example/flutter_quickstart/`
- **Single Test File**: Use one test file with one test to avoid multiple Ditto instances
- **Debug Build**: Works with debug builds (no need for release)
- **Environment Variables**: Must be base64 encoded and passed via Gradle

### Success Criteria
✅ No permission dialogs during tests
✅ Single Ditto instance throughout test execution
✅ App syncs with Ditto Cloud
✅ Test finds synced document from cloud
✅ Tests pass both locally and on BrowserStack

### Test Results
- **Status**: PASSED
- **Duration**: 59 seconds
- **Device**: Google Pixel 7 (Android 13.0)
- **Test Count**: 1 test, 1 passed, 0 failed