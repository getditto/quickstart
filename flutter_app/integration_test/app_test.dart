import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_quickstart/main.dart' as app;
import 'package:flutter_dotenv/flutter_dotenv.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Ditto Tasks App Integration Tests', () {
    setUpAll(() async {
      // Load .env file for tests
      await dotenv.load(fileName: ".env");
    });

    testWidgets('App loads and syncs with Ditto Cloud',
        (WidgetTester tester) async {
      // Initialize app
      await app.main();
      // Allow up to 10 seconds for Ditto to initialise and the first sync
      // exchange to complete. pumpAndSettle returns as soon as the UI is
      // idle, so on fast devices this resolves much sooner.
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // NOTE: iOS system permission dialogs (Bluetooth, Local Network) are
      // native UIAlertControllers and cannot be found or tapped via Flutter's
      // widget-tree finders.  They are handled at the XCTest layer in
      // ios/RunnerTests/RunnerTests.m via addUIInterruptionMonitor.

      // Verify app title is present
      expect(find.text('Ditto Tasks'), findsOneWidget);

      // Verify sync toggle is present
      expect(find.text('Sync Active'), findsOneWidget);

      // Verify add task FAB is present
      expect(find.byIcon(Icons.add_task), findsOneWidget);

      // Verify clear button is present
      expect(find.byIcon(Icons.clear), findsOneWidget);

      // Look for the test document that should be synced from Ditto cloud.
      // The playground can accumulate many documents from previous CI runs,
      // so we poll rather than waiting a fixed amount of time.
      const testTitle = String.fromEnvironment('TASK_TO_FIND');

      if (testTitle.isEmpty) {
        throw Exception('TASK_TO_FIND environment variable must be set. '
            'Build with: --dart-define=TASK_TO_FIND=<task_title>');
      }

      // Poll every 500 ms for up to 45 seconds to give the cloud sync
      // enough time to deliver and write all documents to the local store.
      const syncTimeout = Duration(seconds: 45);
      final deadline = DateTime.now().add(syncTimeout);
      bool taskFound = false;

      while (DateTime.now().isBefore(deadline)) {
        await tester.pump(const Duration(milliseconds: 500));
        if (find.text(testTitle).evaluate().isNotEmpty) {
          taskFound = true;
          break;
        }
      }

      expect(taskFound, isTrue,
          reason:
              'Should find test document with title: $testTitle synced from Ditto cloud within ${syncTimeout.inSeconds}s');
    });
  });
}
