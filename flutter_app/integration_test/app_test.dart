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

    testWidgets('App loads and shows initial UI', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Verify app title is present
      expect(find.text('Ditto Tasks'), findsOneWidget);

      // Verify sync toggle is present
      expect(find.text('Sync Active'), findsOneWidget);

      // Verify add task FAB is present
      expect(find.byIcon(Icons.add_task), findsOneWidget);

      // Verify clear button is present
      expect(find.byIcon(Icons.clear), findsOneWidget);
    });

    testWidgets('Can add a new task', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Tap the add task FAB
      await tester.tap(find.byIcon(Icons.add_task));
      await tester.pumpAndSettle();

      // Verify dialog appears
      expect(find.text('Add Task'), findsOneWidget);
      expect(find.byType(TextField), findsOneWidget);

      // Enter task title
      const testTaskTitle = 'Integration Test Task';
      await tester.enterText(find.byType(TextField), testTaskTitle);
      await tester.pumpAndSettle();

      // Tap Add button in dialog
      await tester.tap(find.text('Add'));
      await tester.pumpAndSettle();

      // Verify task appears in list
      expect(find.text(testTaskTitle), findsOneWidget);
    });

    testWidgets('Can toggle task completion', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Add a task first
      await tester.tap(find.byIcon(Icons.add_task));
      await tester.pumpAndSettle();

      const testTaskTitle = 'Task to Complete';
      await tester.enterText(find.byType(TextField), testTaskTitle);
      await tester.pumpAndSettle();

      await tester.tap(find.text('Add'));
      await tester.pumpAndSettle();

      // Find the checkbox for the task
      final checkbox = find.byType(Checkbox).first;

      // Verify initial state is unchecked
      Checkbox checkboxWidget = tester.widget(checkbox);
      expect(checkboxWidget.value, false);

      // Toggle the checkbox
      await tester.tap(checkbox);
      await tester.pumpAndSettle();

      // Verify checkbox is now checked
      checkboxWidget = tester.widget(checkbox);
      expect(checkboxWidget.value, true);
    });

    testWidgets('Can edit an existing task', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Add a task first
      await tester.tap(find.byIcon(Icons.add_task));
      await tester.pumpAndSettle();

      const originalTitle = 'Original Task';
      await tester.enterText(find.byType(TextField), originalTitle);
      await tester.pumpAndSettle();

      await tester.tap(find.text('Add'));
      await tester.pumpAndSettle();

      // Tap edit button
      await tester.tap(find.byIcon(Icons.edit).first);
      await tester.pumpAndSettle();

      // Clear and enter new text
      await tester.enterText(find.byType(TextField), '');
      await tester.pumpAndSettle();

      const newTitle = 'Edited Task';
      await tester.enterText(find.byType(TextField), newTitle);
      await tester.pumpAndSettle();

      // Save changes
      await tester.tap(find.text('Add'));
      await tester.pumpAndSettle();

      // Verify task is updated
      expect(find.text(newTitle), findsOneWidget);
      expect(find.text(originalTitle), findsNothing);
    });

    testWidgets('Can delete a task by swiping', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Add a task first
      await tester.tap(find.byIcon(Icons.add_task));
      await tester.pumpAndSettle();

      const taskToDelete = 'Task to Delete';
      await tester.enterText(find.byType(TextField), taskToDelete);
      await tester.pumpAndSettle();

      await tester.tap(find.text('Add'));
      await tester.pumpAndSettle();

      // Verify task exists
      expect(find.text(taskToDelete), findsOneWidget);

      // Swipe to dismiss
      await tester.drag(find.text(taskToDelete), const Offset(-500.0, 0.0));
      await tester.pumpAndSettle();

      // Verify task is deleted (soft delete)
      expect(find.text(taskToDelete), findsNothing);

      // Verify snackbar appears
      expect(find.text('Deleted Task $taskToDelete'), findsOneWidget);
    });

    testWidgets('Can toggle sync on and off', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Find the sync switch
      final syncSwitch = find.byType(Switch);
      expect(syncSwitch, findsOneWidget);

      // Get initial state
      Switch switchWidget = tester.widget(syncSwitch);
      final initialState = switchWidget.value;

      // Toggle sync
      await tester.tap(syncSwitch);
      await tester.pumpAndSettle();

      // Verify state changed
      switchWidget = tester.widget(syncSwitch);
      expect(switchWidget.value, !initialState);

      // Toggle back
      await tester.tap(syncSwitch);
      await tester.pumpAndSettle();

      // Verify state restored
      switchWidget = tester.widget(syncSwitch);
      expect(switchWidget.value, initialState);
    });

    testWidgets('Can clear all tasks', (WidgetTester tester) async {
      await app.main();
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Add multiple tasks
      for (int i = 1; i <= 3; i++) {
        await tester.tap(find.byIcon(Icons.add_task));
        await tester.pumpAndSettle();

        await tester.enterText(find.byType(TextField), 'Task $i');
        await tester.pumpAndSettle();

        await tester.tap(find.text('Add'));
        await tester.pumpAndSettle();
      }

      // Verify tasks exist
      expect(find.text('Task 1'), findsOneWidget);
      expect(find.text('Task 2'), findsOneWidget);
      expect(find.text('Task 3'), findsOneWidget);

      // Tap clear button
      await tester.tap(find.byIcon(Icons.clear));
      await tester.pumpAndSettle();

      // Verify all tasks are cleared
      expect(find.text('Task 1'), findsNothing);
      expect(find.text('Task 2'), findsNothing);
      expect(find.text('Task 3'), findsNothing);
    });

    testWidgets('Can find test document from CI/CD', (WidgetTester tester) async {
      // This test is specifically for CI/CD integration
      // It looks for a document with a title set by environment variable
      final testTitle = const String.fromEnvironment('TASK_TO_FIND',
          defaultValue: '');

      if (testTitle.isEmpty) {
        // Skip this test if not in CI environment
        print('Skipping CI test - TASK_TO_FIND not set');
        return;
      }

      await app.main();

      // Wait for app to initialize and sync
      await tester.pumpAndSettle(const Duration(seconds: 5));
      await Future.delayed(const Duration(seconds: 5));
      await tester.pumpAndSettle();

      // Look for the test document
      expect(find.text(testTitle), findsOneWidget,
          reason: 'Should find test document with title: $testTitle');
    });
  });
}