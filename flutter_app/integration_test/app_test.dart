import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_quickstart/main.dart' as app;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Ditto Tasks App Integration Tests', () {
    testWidgets('App loads and initializes Ditto successfully', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization with longer timeout
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Verify that the app title is displayed
      expect(find.text('Ditto Tasks'), findsOneWidget);
      
      // Verify that the sync toggle is present (indicates Ditto is initialized)
      expect(find.text('Sync Active'), findsOneWidget);
      
      // Verify the FloatingActionButton is present
      expect(find.byType(FloatingActionButton), findsOneWidget);
    });

    testWidgets('Can add a new task', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Find and tap the add task button
      final addButton = find.byType(FloatingActionButton);
      expect(addButton, findsOneWidget);
      await tester.tap(addButton);
      await tester.pumpAndSettle();

      // Verify dialog is shown
      expect(find.byType(AlertDialog), findsOneWidget);
      
      // Find the text field and enter a task
      const taskText = 'Test Integration Task';
      final textField = find.byType(TextField);
      expect(textField, findsOneWidget);
      await tester.enterText(textField, taskText);
      await tester.pumpAndSettle();

      // Find and tap the save button
      final saveButton = find.text('Save');
      expect(saveButton, findsOneWidget);
      await tester.tap(saveButton);
      await tester.pumpAndSettle();

      // Wait for the task to be saved to Ditto and UI to update
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Verify the task appears in the list
      expect(find.text(taskText), findsOneWidget);
      
      // Verify it's displayed as a CheckboxListTile
      expect(find.byType(CheckboxListTile), findsAtLeastNWidgets(1));
    });

    testWidgets('Can toggle task completion status', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Add a task first
      const taskText = 'Task to Toggle';
      await _addTask(tester, taskText);

      // Find the checkbox for the task and tap it
      final checkbox = find.byType(Checkbox).first;
      expect(checkbox, findsOneWidget);
      
      // Verify initial state (should be unchecked)
      Checkbox checkboxWidget = tester.widget(checkbox);
      expect(checkboxWidget.value, false);
      
      // Tap to toggle
      await tester.tap(checkbox);
      await tester.pumpAndSettle();
      
      // Wait for Ditto update
      await tester.pumpAndSettle(const Duration(seconds: 3));
      
      // Verify the checkbox is now checked
      checkboxWidget = tester.widget(checkbox);
      expect(checkboxWidget.value, true);
    });

    testWidgets('Can edit an existing task', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Add a task first
      const originalText = 'Original Task';
      await _addTask(tester, originalText);

      // Find and tap the edit button
      final editButton = find.byIcon(Icons.edit);
      expect(editButton, findsAtLeastNWidgets(1));
      await tester.tap(editButton.first);
      await tester.pumpAndSettle();

      // Verify edit dialog is shown
      expect(find.byType(AlertDialog), findsOneWidget);
      
      // Find the text field and change the text
      const newText = 'Updated Task Text';
      final textField = find.byType(TextField);
      await tester.enterText(textField, newText);
      await tester.pumpAndSettle();

      // Find and tap the save button
      final saveButton = find.text('Save');
      await tester.tap(saveButton);
      await tester.pumpAndSettle();

      // Wait for the update to propagate
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Verify the task text has been updated
      expect(find.text(newText), findsOneWidget);
      expect(find.text(originalText), findsNothing);
    });

    testWidgets('Can delete a task by swiping', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Add a task first
      const taskText = 'Task to Delete';
      await _addTask(tester, taskText);

      // Find the dismissible item and swipe to delete
      final dismissible = find.byType(Dismissible);
      expect(dismissible, findsAtLeastNWidgets(1));
      
      await tester.drag(dismissible.first, const Offset(-500, 0));
      await tester.pumpAndSettle();
      
      // Wait for the deletion to propagate
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Verify the task is no longer visible
      expect(find.text(taskText), findsNothing);
      
      // Verify snackbar is shown
      expect(find.byType(SnackBar), findsOneWidget);
      expect(find.textContaining('Deleted Task'), findsOneWidget);
    });

    testWidgets('Can clear all tasks', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Add multiple tasks
      await _addTask(tester, 'Task 1');
      await _addTask(tester, 'Task 2');
      await _addTask(tester, 'Task 3');

      // Verify tasks are present
      expect(find.text('Task 1'), findsOneWidget);
      expect(find.text('Task 2'), findsOneWidget);
      expect(find.text('Task 3'), findsOneWidget);

      // Find and tap the clear button
      final clearButton = find.byIcon(Icons.clear);
      expect(clearButton, findsOneWidget);
      await tester.tap(clearButton);
      await tester.pumpAndSettle();

      // Wait for the clear operation to complete
      await tester.pumpAndSettle(const Duration(seconds: 5));

      // Verify all tasks are cleared
      expect(find.text('Task 1'), findsNothing);
      expect(find.text('Task 2'), findsNothing);
      expect(find.text('Task 3'), findsNothing);
    });

    testWidgets('Sync toggle works correctly', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Find the sync toggle
      final syncToggle = find.byType(Switch);
      expect(syncToggle, findsOneWidget);

      // Verify initial state (should be enabled)
      Switch switchWidget = tester.widget(syncToggle);
      expect(switchWidget.value, true);

      // Toggle sync off
      await tester.tap(syncToggle);
      await tester.pumpAndSettle();

      // Verify sync is now disabled
      switchWidget = tester.widget(syncToggle);
      expect(switchWidget.value, false);

      // Toggle sync back on
      await tester.tap(syncToggle);
      await tester.pumpAndSettle();

      // Verify sync is enabled again
      switchWidget = tester.widget(syncToggle);
      expect(switchWidget.value, true);
    });

    testWidgets('App info is displayed correctly', (WidgetTester tester) async {
      // Start the app
      app.main();
      await tester.pumpAndSettle();

      // Wait for Ditto initialization
      await tester.pumpAndSettle(const Duration(seconds: 10));

      // Verify App ID and Token are displayed (should be configured in environment)
      expect(find.textContaining('AppID:'), findsOneWidget);
      expect(find.textContaining('Token:'), findsOneWidget);
    });
  });
}

/// Helper function to add a task
Future<void> _addTask(WidgetTester tester, String taskText) async {
  // Tap the add button
  final addButton = find.byType(FloatingActionButton);
  await tester.tap(addButton);
  await tester.pumpAndSettle();

  // Enter task text
  final textField = find.byType(TextField);
  await tester.enterText(textField, taskText);
  await tester.pumpAndSettle();

  // Save the task
  final saveButton = find.text('Save');
  await tester.tap(saveButton);
  await tester.pumpAndSettle();

  // Wait for the task to be saved
  await tester.pumpAndSettle(const Duration(seconds: 3));
}