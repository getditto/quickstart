import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';

/// Page Object Model for the main Tasks screen
class TasksPage {
  const TasksPage(this.tester);
  
  final WidgetTester tester;

  // Finders for UI elements
  Finder get appTitle => find.text('Ditto Tasks');
  Finder get addTaskButton => find.byType(FloatingActionButton);
  Finder get clearButton => find.byIcon(Icons.clear);
  Finder get syncToggle => find.byType(Switch);
  Finder get syncLabel => find.text('Sync Active');
  Finder get appIdText => find.textContaining('AppID:');
  Finder get tokenText => find.textContaining('Token:');
  Finder get taskList => find.byType(ListView);

  // Task-related finders
  Finder taskByText(String text) => find.text(text);
  Finder get allTasks => find.byType(CheckboxListTile);
  Finder get allCheckboxes => find.byType(Checkbox);
  Finder get allEditButtons => find.byIcon(Icons.edit);
  Finder get allDismissibles => find.byType(Dismissible);

  // Actions
  Future<void> tapAddTask() async {
    await tester.tap(addTaskButton);
    await tester.pumpAndSettle();
  }

  Future<void> tapClearTasks() async {
    await tester.tap(clearButton);
    await tester.pumpAndSettle();
  }

  Future<void> toggleSync() async {
    await tester.tap(syncToggle);
    await tester.pumpAndSettle();
  }

  Future<void> toggleTaskCompletion(String taskText) async {
    final taskTile = find.ancestor(
      of: find.text(taskText),
      matching: find.byType(CheckboxListTile),
    );
    final checkbox = find.descendant(
      of: taskTile,
      matching: find.byType(Checkbox),
    );
    await tester.tap(checkbox);
    await tester.pumpAndSettle();
  }

  Future<void> editTask(String originalText) async {
    final taskTile = find.ancestor(
      of: find.text(originalText),
      matching: find.byType(CheckboxListTile),
    );
    final editButton = find.descendant(
      of: taskTile,
      matching: find.byIcon(Icons.edit),
    );
    await tester.tap(editButton);
    await tester.pumpAndSettle();
  }

  Future<void> deleteTaskBySwipe(String taskText) async {
    final taskTile = find.ancestor(
      of: find.text(taskText),
      matching: find.byType(Dismissible),
    );
    await tester.drag(taskTile, const Offset(-500, 0));
    await tester.pumpAndSettle();
  }

  // Verifications
  void verifyAppInitialized() {
    expect(appTitle, findsOneWidget);
    expect(syncLabel, findsOneWidget);
    expect(addTaskButton, findsOneWidget);
  }

  void verifyTaskExists(String taskText) {
    expect(taskByText(taskText), findsOneWidget);
  }

  void verifyTaskNotExists(String taskText) {
    expect(taskByText(taskText), findsNothing);
  }

  void verifyTaskCount(int expectedCount) {
    expect(allTasks, findsNWidgets(expectedCount));
  }

  void verifyTaskCompleted(String taskText, bool shouldBeCompleted) {
    final taskTile = find.ancestor(
      of: find.text(taskText),
      matching: find.byType(CheckboxListTile),
    );
    final checkbox = find.descendant(
      of: taskTile,
      matching: find.byType(Checkbox),
    );
    
    final checkboxWidget = tester.widget<Checkbox>(checkbox);
    expect(checkboxWidget.value, shouldBeCompleted);
  }

  bool get isSyncActive {
    final switchWidget = tester.widget<Switch>(syncToggle);
    return switchWidget.value;
  }

  void verifySyncStatus(bool expectedStatus) {
    expect(isSyncActive, expectedStatus);
  }
}

/// Page Object Model for the Add/Edit Task dialog
class TaskDialog {
  const TaskDialog(this.tester);
  
  final WidgetTester tester;

  // Finders
  Finder get dialog => find.byType(AlertDialog);
  Finder get textField => find.byType(TextField);
  Finder get saveButton => find.text('Save');
  Finder get cancelButton => find.text('Cancel');

  // Actions
  Future<void> enterText(String text) async {
    await tester.enterText(textField, text);
    await tester.pumpAndSettle();
  }

  Future<void> clearAndEnterText(String text) async {
    await tester.tap(textField);
    await tester.pumpAndSettle();
    // Select all text and replace
    await tester.sendKeyDownEvent(LogicalKeyboardKey.control);
    await tester.sendKeyEvent(LogicalKeyboardKey.keyA);
    await tester.sendKeyUpEvent(LogicalKeyboardKey.control);
    await tester.enterText(textField, text);
    await tester.pumpAndSettle();
  }

  Future<void> save() async {
    await tester.tap(saveButton);
    await tester.pumpAndSettle();
  }

  Future<void> cancel() async {
    await tester.tap(cancelButton);
    await tester.pumpAndSettle();
  }

  // Verifications
  void verifyDialogShown() {
    expect(dialog, findsOneWidget);
    expect(textField, findsOneWidget);
    expect(saveButton, findsOneWidget);
  }

  void verifyDialogClosed() {
    expect(dialog, findsNothing);
  }

  String get currentText {
    final textFieldWidget = tester.widget<TextField>(textField);
    return textFieldWidget.controller?.text ?? '';
  }

  void verifyTextFieldContains(String expectedText) {
    expect(currentText, expectedText);
  }
}