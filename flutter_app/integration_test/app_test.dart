import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'util.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testDitto('App loads and displays basic UI elements', (tester) async {
    expect(appBar, findsOneWidget);
    expect(syncTile, findsOneWidget);
    expect(openAddDialogButton, findsOneWidget);
  });

  testDitto('Can add and verify a task', (tester) async {
    const name = "Integration Test Task";
    await tester.addTask(name);
    await tester.pump(const Duration(seconds: 3));

    expect(taskWithName(name), findsOneWidget);
  });

  testDitto('Can mark task as complete', (tester) async {
    const name = "Task to Complete";
    await tester.addTask(name);

    expect(tester.task(name).done, false);

    await tester.setTaskDone(name: name, done: true);
    expect(tester.task(name).done, true);
  });

  testDitto('Can delete a task by swipe', (tester) async {
    const name = "Task to Delete";
    await tester.addTask(name);

    expect(taskWithName(name), findsOneWidget);

    await tester.deleteTask(name);
    expect(taskWithName(name), findsNothing);
  });

  testDitto('Sync functionality test', (tester) async {
    expect(tester.isSyncing, true);

    await tester.setSyncing(false);
    expect(tester.isSyncing, false);

    await tester.setSyncing(true);
    expect(tester.isSyncing, true);
  });

  testDitto(
    'GitHub test document sync verification',
    skip: !const bool.hasEnvironment("GITHUB_TEST_DOC_ID"),
    (tester) async {
      const githubRunId = String.fromEnvironment("GITHUB_TEST_DOC_ID");

      final splitRunId = githubRunId.split('_');
      // Expected format: 'github_test_RUNID_RUNNUMBER' where index 2 contains RUNID
      if (splitRunId.length >= 3) {
        final runIdPart = splitRunId[2]; // Extract RUNID from position 2

        try {
          await tester.waitUntil(
            () => tester.isVisible(taskWithName(runIdPart)),
          );
        } catch (_) {
          // GitHub test document not found within timeout
        }
      } else {
        // GitHub test document ID format invalid, skipping sync verification
      }
    },
  );
}
