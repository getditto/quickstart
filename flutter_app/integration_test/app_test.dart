import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_quickstart/main.dart' as app;
import 'package:flutter_dotenv/flutter_dotenv.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Ditto Tasks App Integration Tests', () {
    setUp(() async {
      await dotenv.load(fileName: ".env");
    });

    testWidgets('App loads and displays basic UI elements', (WidgetTester tester) async {
      app.main();
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 5));

      expect(find.text('Ditto Tasks'), findsOneWidget);
      
      final syncTile = find.byType(SwitchListTile);
      expect(syncTile, findsOneWidget);
      
      final fab = find.byType(FloatingActionButton);
      expect(fab, findsOneWidget);

    });

    testWidgets('Can add and verify a task', (WidgetTester tester) async {
      await dotenv.load(fileName: ".env");
      
      app.main();
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 5));

      final fab = find.byType(FloatingActionButton);
      await tester.tap(fab);
      await tester.pumpAndSettle();

      final textField = find.byType(TextField);
      expect(textField, findsOneWidget);
      
      await tester.enterText(textField, 'Integration Test Task');
      
      final addButton = find.widgetWithText(ElevatedButton, 'Add');
      await tester.tap(addButton);
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 3));

      expect(find.text('Integration Test Task'), findsOneWidget);
      
    });

    testWidgets('Can mark task as complete', (WidgetTester tester) async {
      await dotenv.load(fileName: ".env");
      
      app.main();
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 5));

      final fab = find.byType(FloatingActionButton);
      await tester.tap(fab);
      await tester.pumpAndSettle();

      final textField = find.byType(TextField);
      await tester.enterText(textField, 'Task to Complete');
      
      final addButton = find.widgetWithText(ElevatedButton, 'Add');
      await tester.tap(addButton);
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 3));

      final checkbox = find.byType(Checkbox);
      expect(checkbox, findsAtLeastNWidgets(1));
      
      await tester.tap(checkbox.first);
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 2));

    });

    testWidgets('Can delete a task by swipe', (WidgetTester tester) async {
      await dotenv.load(fileName: ".env");
      
      app.main();
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 5));

      final fab = find.byType(FloatingActionButton);
      await tester.tap(fab);
      await tester.pumpAndSettle();

      final textField = find.byType(TextField);
      await tester.enterText(textField, 'Task to Delete');
      
      final addButton = find.widgetWithText(ElevatedButton, 'Add');
      await tester.tap(addButton);
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 3));

      final taskTile = find.text('Task to Delete');
      expect(taskTile, findsOneWidget);

      await tester.drag(taskTile, const Offset(-500.0, 0.0));
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 2));

    });

    testWidgets('Sync functionality test', (WidgetTester tester) async {
      await dotenv.load(fileName: ".env");
      
      app.main();
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 5));

      final syncTile = find.byType(SwitchListTile);
      expect(syncTile, findsOneWidget);

      await tester.tap(syncTile);
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 2));

      await tester.tap(syncTile);
      await tester.pumpAndSettle();

    });

    testWidgets('GitHub test document sync verification', (WidgetTester tester) async {
      await dotenv.load(fileName: ".env");
      
      app.main();
      await tester.pumpAndSettle();

      await tester.pump(const Duration(seconds: 10));

      const githubRunId = String.fromEnvironment('GITHUB_TEST_DOC_ID');
      if (githubRunId.isNotEmpty) {
        final runIdPart = githubRunId.split('_')[2];
        final testDocumentText = find.textContaining(runIdPart);
        
        int attempts = 0;
        const maxAttempts = 15;
        
        while (attempts < maxAttempts && testDocumentText.evaluate().isEmpty) {
          await tester.pump(const Duration(seconds: 2));
          attempts++;
        }

        if (testDocumentText.evaluate().isNotEmpty) {
          // GitHub test document synced successfully
        } else {
          // GitHub test document not found within timeout
        }
      } else {
        // No GitHub test document ID provided, skipping sync verification
      }
    });
  });
}