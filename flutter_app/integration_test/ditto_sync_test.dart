import 'package:flutter_quickstart/task.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';

import 'util.dart';


void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  testDitto(
    'Ditto initialization and cloud connection test',
    (tester) async {
      expect(
        tester.isSyncing,
        isTrue,
        reason: 'Sync should be active on startup',
      );
    },
  );

  testDitto(
    'Create task and verify cloud sync document insertion',
    (tester) async {
      final time = DateTime.now().millisecondsSinceEpoch;
      final testTaskTitle = 'Cloud Sync Test Task $time';

      await tester.addTask(testTaskTitle);

      await tester.waitUntil(() => tester.isVisible(taskWithName(testTaskTitle)));
      await tester.pump(const Duration(seconds: 3));
    },
  );

  testDitto(
    'Verify task state changes sync to cloud',
    (tester) async {
      final time = DateTime.now().millisecondsSinceEpoch;
      final testTaskTitle = 'State Change Test $time';

      await tester.addTask(testTaskTitle);
      await tester
          .waitUntil(() => tester.isVisible(taskWithName(testTaskTitle)));

      expect(tester.task(testTaskTitle).done, false);

      await tester.setTaskDone(name: testTaskTitle, done: true);
      expect(tester.task(testTaskTitle).done, true);
    },
  );

  testDitto(
    'Sync toggle functionality test',
    (tester) async {
      expect(tester.isSyncing, true);

      await tester.setSyncing(false);
      expect(tester.isSyncing, false);

      await tester.setSyncing(true);
      expect(tester.isSyncing, true);
    },
  );

  testDitto(
    'Documents created via Big Peer are available on SDK',
    (tester) async {
      final title = "flutter_test_bp_${DateTime.now().millisecondsSinceEpoch}";
      final task = Task(title: title, done: false, deleted: false);

      await bigPeerHttpExecute(
        "INSERT INTO tasks DOCUMENTS (:doc)",
        arguments: {"doc": task.toJson()},
      );

      await tester.waitUntil(() => tester.isVisible(taskWithName(title)));
    },
  );

  testDitto(
    'Documents created via SDK are available via Big Peer',
    (tester) async {
      final title = "flutter_test_sdk_${DateTime.now().millisecondsSinceEpoch}";
      await tester.addTask(title);

      Future<Task> taskExistsOnBigPeer() async {
        final {"items": List items} = await bigPeerHttpExecute(
          "SELECT * FROM tasks",
        );

        final item = items.singleWhere((json) => json["title"] == title);
        return Task.fromJson(item);
      }

      late final Task task;
      await tester.waitUntil(() async {
        try {
          task = await taskExistsOnBigPeer();
          return true;
        } catch (e) {
          print(e);
          return false;
        }
      });

      expect(task.title, equals(title));
    },
  );
}
