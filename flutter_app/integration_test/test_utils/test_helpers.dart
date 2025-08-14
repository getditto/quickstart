import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'page_objects.dart';

/// Common test setup and utility functions
class TestHelpers {
  static const Duration shortWait = Duration(seconds: 2);
  static const Duration mediumWait = Duration(seconds: 5);
  static const Duration longWait = Duration(seconds: 10);

  /// Wait for Ditto to initialize with proper timeout
  static Future<void> waitForDittoInitialization(WidgetTester tester) async {
    await tester.pumpAndSettle(longWait);
  }

  /// Wait for Ditto operations to complete
  static Future<void> waitForDittoOperation(WidgetTester tester) async {
    await tester.pumpAndSettle(mediumWait);
  }

  /// Create a test task with the given text
  static Future<void> createTask(
    WidgetTester tester, 
    String taskText, {
    bool waitForCreation = true,
  }) async {
    final tasksPage = TasksPage(tester);
    final taskDialog = TaskDialog(tester);

    await tasksPage.tapAddTask();
    taskDialog.verifyDialogShown();
    
    await taskDialog.enterText(taskText);
    await taskDialog.save();
    
    if (waitForCreation) {
      await waitForDittoOperation(tester);
      tasksPage.verifyTaskExists(taskText);
    }
  }

  /// Create multiple test tasks
  static Future<void> createMultipleTasks(
    WidgetTester tester, 
    List<String> taskTexts,
  ) async {
    for (final taskText in taskTexts) {
      await createTask(tester, taskText);
    }
  }

  /// Verify app is fully loaded and ready
  static void verifyAppReady(WidgetTester tester) {
    final tasksPage = TasksPage(tester);
    tasksPage.verifyAppInitialized();
  }

  /// Setup test environment - ensures app is ready for testing
  static Future<void> setupTest(WidgetTester tester) async {
    await waitForDittoInitialization(tester);
    verifyAppReady(tester);
  }

  /// Clean up test data by clearing all tasks
  static Future<void> cleanupTasks(WidgetTester tester) async {
    final tasksPage = TasksPage(tester);
    await tasksPage.tapClearTasks();
    await waitForDittoOperation(tester);
  }

  /// Generate unique task text for testing
  static String generateTaskText([String prefix = 'Test Task']) {
    final timestamp = DateTime.now().millisecondsSinceEpoch;
    return '$prefix $timestamp';
  }

  /// Wait for snackbar to appear and verify its content
  static Future<void> verifySnackbar(
    WidgetTester tester,
    String expectedText, {
    bool shouldContain = true,
  }) async {
    await tester.pumpAndSettle();
    
    if (shouldContain) {
      expect(find.byType(SnackBar), findsOneWidget);
      expect(find.textContaining(expectedText), findsOneWidget);
    } else {
      expect(find.textContaining(expectedText), findsNothing);
    }
  }

  /// Retry an operation with exponential backoff
  static Future<T> retryOperation<T>(
    Future<T> Function() operation, {
    int maxRetries = 3,
    Duration initialDelay = const Duration(milliseconds: 500),
  }) async {
    int attempts = 0;
    Duration delay = initialDelay;

    while (attempts < maxRetries) {
      try {
        return await operation();
      } catch (e) {
        attempts++;
        if (attempts >= maxRetries) {
          rethrow;
        }
        await Future.delayed(delay);
        delay *= 2; // Exponential backoff
      }
    }

    throw Exception('Operation failed after $maxRetries attempts');
  }

  /// Verify task operations work correctly
  static Future<void> verifyTaskOperations(WidgetTester tester) async {
    final tasksPage = TasksPage(tester);
    final taskText = generateTaskText('Operations Test');

    // Test task creation
    await createTask(tester, taskText);
    tasksPage.verifyTaskExists(taskText);

    // Test task completion toggle
    await tasksPage.toggleTaskCompletion(taskText);
    await waitForDittoOperation(tester);
    tasksPage.verifyTaskCompleted(taskText, true);

    // Test task uncompleting
    await tasksPage.toggleTaskCompletion(taskText);
    await waitForDittoOperation(tester);
    tasksPage.verifyTaskCompleted(taskText, false);

    // Test task deletion
    await tasksPage.deleteTaskBySwipe(taskText);
    await waitForDittoOperation(tester);
    tasksPage.verifyTaskNotExists(taskText);
  }

  /// Test sync functionality
  static Future<void> verifySyncOperations(WidgetTester tester) async {
    final tasksPage = TasksPage(tester);

    // Verify initial sync state
    tasksPage.verifySyncStatus(true);

    // Test disabling sync
    await tasksPage.toggleSync();
    await waitForDittoOperation(tester);
    tasksPage.verifySyncStatus(false);

    // Test enabling sync
    await tasksPage.toggleSync();
    await waitForDittoOperation(tester);
    tasksPage.verifySyncStatus(true);
  }

  /// Create comprehensive test data
  static Future<void> createTestData(WidgetTester tester) async {
    final testTasks = [
      'Personal Task 1',
      'Work Task 1',
      'Shopping List Item',
      'Important Reminder',
      'Meeting Notes',
    ];

    await createMultipleTasks(tester, testTasks);
  }
}

/// Custom matchers for Ditto-specific assertions
class DittoMatchers {
  /// Matcher to verify a task exists with specific properties
  static Matcher hasTaskWithText(String text) {
    return _TaskTextMatcher(text);
  }

  /// Matcher to verify task completion status
  static Matcher hasTaskCompleted(String text, bool isCompleted) {
    return _TaskCompletionMatcher(text, isCompleted);
  }
}

class _TaskTextMatcher extends Matcher {
  const _TaskTextMatcher(this.expectedText);
  
  final String expectedText;

  @override
  bool matches(dynamic item, Map matchState) {
    return item is WidgetTester && 
           find.text(expectedText).evaluate().isNotEmpty;
  }

  @override
  Description describe(Description description) {
    return description.add('has task with text "$expectedText"');
  }
}

class _TaskCompletionMatcher extends Matcher {
  const _TaskCompletionMatcher(this.taskText, this.expectedCompletion);
  
  final String taskText;
  final bool expectedCompletion;

  @override
  bool matches(dynamic item, Map matchState) {
    if (item is! WidgetTester) return false;
    
    try {
      final tasksPage = TasksPage(item);
      tasksPage.verifyTaskCompleted(taskText, expectedCompletion);
      return true;
    } catch (e) {
      return false;
    }
  }

  @override
  Description describe(Description description) {
    return description.add(
      'has task "$taskText" with completion status: $expectedCompletion'
    );
  }
}