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
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Tap "OK" button if Bluetooth permission dialog appears (iOS)
      final okButton = find.text('OK');
      if (okButton.evaluate().isNotEmpty) {
        await tester.tap(okButton);
        await tester.pumpAndSettle(const Duration(seconds: 2));
      }

      // Verify app title is present
      expect(find.text('Ditto Tasks'), findsOneWidget);

      // Verify sync toggle is present
      expect(find.text('Sync Active'), findsOneWidget);

      // Verify add task FAB is present
      expect(find.byIcon(Icons.add_task), findsOneWidget);

      // Verify clear button is present
      expect(find.byIcon(Icons.clear), findsOneWidget);

      // Wait for sync to complete
      await Future.delayed(const Duration(seconds: 5));
      await tester.pumpAndSettle();

      // Look for the test document that should be synced from Ditto cloud
      const testTitle = String.fromEnvironment('TASK_TO_FIND');

      if (testTitle.isEmpty) {
        throw Exception('TASK_TO_FIND environment variable must be set. '
            'Build with: --dart-define=TASK_TO_FIND=<task_title>');
      }

      expect(find.text('${testTitle}_randomsuffix'), findsOneWidget,
          reason:
              'Should find test document with title: ${testTitle}_randomsuffix synced from Ditto cloud');
    });
  });
}
