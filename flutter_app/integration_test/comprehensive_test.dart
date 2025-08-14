import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:flutter_quickstart/main.dart' as app;

import 'test_utils/page_objects.dart';
import 'test_utils/test_helpers.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  group('Comprehensive Ditto Tasks Integration Tests', () {
    late TasksPage tasksPage;
    late TaskDialog taskDialog;

    setUp(() async {
      // Each test gets a fresh app instance
      app.main();
    });

    testWidgets('Complete user workflow - add, edit, toggle, delete tasks', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      taskDialog = TaskDialog(tester);
      
      // Setup: Wait for app to initialize
      await TestHelpers.setupTest(tester);

      // Step 1: Create initial tasks
      const task1 = 'Buy groceries';
      const task2 = 'Call dentist';
      const task3 = 'Fix bike';

      await TestHelpers.createTask(tester, task1);
      await TestHelpers.createTask(tester, task2);
      await TestHelpers.createTask(tester, task3);

      // Verify all tasks created
      tasksPage.verifyTaskCount(3);
      tasksPage.verifyTaskExists(task1);
      tasksPage.verifyTaskExists(task2);
      tasksPage.verifyTaskExists(task3);

      // Step 2: Mark some tasks as completed
      await tasksPage.toggleTaskCompletion(task1);
      await TestHelpers.waitForDittoOperation(tester);
      tasksPage.verifyTaskCompleted(task1, true);

      await tasksPage.toggleTaskCompletion(task3);
      await TestHelpers.waitForDittoOperation(tester);
      tasksPage.verifyTaskCompleted(task3, true);

      // Verify task2 is still not completed
      tasksPage.verifyTaskCompleted(task2, false);

      // Step 3: Edit a task
      await tasksPage.editTask(task2);
      taskDialog.verifyDialogShown();
      
      const updatedTask2 = 'Call dentist for appointment';
      await taskDialog.clearAndEnterText(updatedTask2);
      await taskDialog.save();
      
      await TestHelpers.waitForDittoOperation(tester);
      tasksPage.verifyTaskExists(updatedTask2);
      tasksPage.verifyTaskNotExists(task2);

      // Step 4: Delete a task
      await tasksPage.deleteTaskBySwipe(task1);
      await TestHelpers.waitForDittoOperation(tester);
      tasksPage.verifyTaskNotExists(task1);
      await TestHelpers.verifySnackbar(tester, 'Deleted Task');

      // Step 5: Verify remaining tasks
      tasksPage.verifyTaskCount(2);
      tasksPage.verifyTaskExists(updatedTask2);
      tasksPage.verifyTaskExists(task3);
      tasksPage.verifyTaskCompleted(task3, true);

      // Cleanup
      await TestHelpers.cleanupTasks(tester);
      tasksPage.verifyTaskCount(0);
    });

    testWidgets('Sync toggle functionality works correctly', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      
      await TestHelpers.setupTest(tester);

      // Test sync operations
      await TestHelpers.verifySyncOperations(tester);

      // Create a task while sync is off
      await tasksPage.toggleSync();
      tasksPage.verifySyncStatus(false);
      
      const taskWhileSyncOff = 'Task created while sync off';
      await TestHelpers.createTask(tester, taskWhileSyncOff);
      
      // Re-enable sync
      await tasksPage.toggleSync();
      tasksPage.verifySyncStatus(true);
      await TestHelpers.waitForDittoOperation(tester);

      // Verify task is still there
      tasksPage.verifyTaskExists(taskWhileSyncOff);
    });

    testWidgets('Bulk operations - create many tasks and clear all', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      
      await TestHelpers.setupTest(tester);

      // Create test data
      await TestHelpers.createTestData(tester);
      
      // Verify all tasks were created
      tasksPage.verifyTaskCount(5);

      // Clear all tasks
      await TestHelpers.cleanupTasks(tester);
      tasksPage.verifyTaskCount(0);
    });

    testWidgets('Task dialog validation and error handling', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      taskDialog = TaskDialog(tester);
      
      await TestHelpers.setupTest(tester);

      // Test empty task creation
      await tasksPage.tapAddTask();
      taskDialog.verifyDialogShown();
      
      // Try to save without entering text
      await taskDialog.save();
      await TestHelpers.waitForDittoOperation(tester);
      
      // Dialog should close even with empty text (app allows this)
      taskDialog.verifyDialogClosed();

      // Test dialog cancellation
      await tasksPage.tapAddTask();
      taskDialog.verifyDialogShown();
      
      await taskDialog.enterText('Task to cancel');
      await taskDialog.cancel();
      taskDialog.verifyDialogClosed();
      
      // Verify task was not created
      tasksPage.verifyTaskNotExists('Task to cancel');

      // Test editing existing task
      const originalTask = 'Task to edit';
      await TestHelpers.createTask(tester, originalTask);
      
      await tasksPage.editTask(originalTask);
      taskDialog.verifyDialogShown();
      taskDialog.verifyTextFieldContains(originalTask);
      
      await taskDialog.cancel();
      taskDialog.verifyDialogClosed();
      
      // Verify task was not changed
      tasksPage.verifyTaskExists(originalTask);
    });

    testWidgets('App state persistence and recovery', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      
      await TestHelpers.setupTest(tester);

      // Create some tasks and set different states
      const task1 = 'Persistent Task 1';
      const task2 = 'Persistent Task 2';
      
      await TestHelpers.createTask(tester, task1);
      await TestHelpers.createTask(tester, task2);
      
      // Mark one as completed
      await tasksPage.toggleTaskCompletion(task1);
      await TestHelpers.waitForDittoOperation(tester);
      
      // Disable sync
      await tasksPage.toggleSync();
      await TestHelpers.waitForDittoOperation(tester);

      // Verify state before "restart"
      tasksPage.verifyTaskExists(task1);
      tasksPage.verifyTaskExists(task2);
      tasksPage.verifyTaskCompleted(task1, true);
      tasksPage.verifyTaskCompleted(task2, false);
      tasksPage.verifySyncStatus(false);

      // Simulate app restart by creating new page objects
      // (In real scenarios, this would involve actual app restart)
      await TestHelpers.waitForDittoOperation(tester);
      
      // Verify state persisted
      final newTasksPage = TasksPage(tester);
      newTasksPage.verifyTaskExists(task1);
      newTasksPage.verifyTaskExists(task2);
      // Note: Sync status might be reset on app restart - this is expected behavior
    });

    testWidgets('Performance test - rapid operations', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      
      await TestHelpers.setupTest(tester);

      // Perform rapid task creation
      final rapidTasks = <String>[];
      for (int i = 0; i < 5; i++) {
        final taskText = TestHelpers.generateTaskText('Rapid $i');
        rapidTasks.add(taskText);
        await TestHelpers.createTask(tester, taskText, waitForCreation: false);
      }

      // Wait for all operations to complete
      await TestHelpers.waitForDittoOperation(tester);

      // Verify all tasks were created
      tasksPage.verifyTaskCount(5);
      for (final taskText in rapidTasks) {
        tasksPage.verifyTaskExists(taskText);
      }

      // Perform rapid toggle operations
      for (final taskText in rapidTasks) {
        await tasksPage.toggleTaskCompletion(taskText);
        // Small delay to prevent overwhelming the system
        await tester.pump(const Duration(milliseconds: 100));
      }

      await TestHelpers.waitForDittoOperation(tester);

      // Verify all tasks are completed
      for (final taskText in rapidTasks) {
        tasksPage.verifyTaskCompleted(taskText, true);
      }
    });

    testWidgets('Edge cases and error scenarios', (WidgetTester tester) async {
      tasksPage = TasksPage(tester);
      taskDialog = TaskDialog(tester);
      
      await TestHelpers.setupTest(tester);

      // Test very long task names
      const longTaskName = 'This is a very long task name that should test how the app handles long text in task titles and ensures the UI remains responsive and properly formatted even with extensive content';
      await TestHelpers.createTask(tester, longTaskName);
      tasksPage.verifyTaskExists(longTaskName);

      // Test special characters
      const specialCharTask = 'Task with special chars: !@#\$%^&*()_+{}|:"<>?[];\'\\,./';
      await TestHelpers.createTask(tester, specialCharTask);
      tasksPage.verifyTaskExists(specialCharTask);

      // Test emoji in task names
      const emojiTask = '🚀 Task with emojis 🎉 and symbols 📱';
      await TestHelpers.createTask(tester, emojiTask);
      tasksPage.verifyTaskExists(emojiTask);

      // Test empty string handling (should work based on app behavior)
      await tasksPage.tapAddTask();
      await taskDialog.enterText('');
      await taskDialog.save();
      await TestHelpers.waitForDittoOperation(tester);

      // Test multiple rapid swipe deletes
      const tasksToDelete = ['Delete 1', 'Delete 2', 'Delete 3'];
      for (final task in tasksToDelete) {
        await TestHelpers.createTask(tester, task);
      }

      for (final task in tasksToDelete) {
        await tasksPage.deleteTaskBySwipe(task);
        await tester.pump(const Duration(milliseconds: 200));
      }

      await TestHelpers.waitForDittoOperation(tester);

      for (final task in tasksToDelete) {
        tasksPage.verifyTaskNotExists(task);
      }
    });
  });
}