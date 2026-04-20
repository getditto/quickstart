import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:flutter_quickstart/task.dart';
import 'package:flutter_quickstart/dialog.dart';

void main() {
  group('Task model', () {
    test('fromJson creates a Task with correct fields', () {
      final json = {
        '_id': '123',
        'title': 'Buy groceries',
        'done': false,
        'deleted': false,
      };

      final task = Task.fromJson(json);

      expect(task.id, '123');
      expect(task.title, 'Buy groceries');
      expect(task.done, false);
      expect(task.deleted, false);
    });

    test('toJson produces correct map', () {
      const task = Task(
        id: 'abc',
        title: 'Walk the dog',
        done: true,
        deleted: false,
      );

      final json = task.toJson();

      expect(json['_id'], 'abc');
      expect(json['title'], 'Walk the dog');
      expect(json['done'], true);
      expect(json['deleted'], false);
    });

    test('toJson omits null id', () {
      const task = Task(
        title: 'New task',
        done: false,
        deleted: false,
      );

      final json = task.toJson();

      expect(json.containsKey('_id'), false);
    });

    test('fromJson and toJson roundtrip', () {
      final original = {
        '_id': 'rt-1',
        'title': 'Roundtrip test',
        'done': true,
        'deleted': true,
      };

      final task = Task.fromJson(original);
      final result = task.toJson();

      expect(result['_id'], original['_id']);
      expect(result['title'], original['title']);
      expect(result['done'], original['done']);
      expect(result['deleted'], original['deleted']);
    });
  });

  group('Add task dialog', () {
    testWidgets('shows Add Task title for new task',
        (WidgetTester tester) async {
      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () => showAddTaskDialog(context),
              child: const Text('Open'),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();

      expect(find.text('Add Task'), findsWidgets);
      expect(find.text('Name'), findsOneWidget);
      expect(find.text('Done'), findsOneWidget);
      expect(find.text('Cancel'), findsOneWidget);
    });

    testWidgets('shows Edit Task title when editing existing task',
        (WidgetTester tester) async {
      const existing = Task(
        id: '1',
        title: 'Existing task',
        done: true,
        deleted: false,
      );

      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () => showAddTaskDialog(context, existing),
              child: const Text('Open'),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();

      expect(find.text('Edit Task'), findsWidgets);
      expect(find.text('Existing task'), findsOneWidget);
    });

    testWidgets('cancel returns null', (WidgetTester tester) async {
      Task? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () async {
                result = await showAddTaskDialog(context);
              },
              child: const Text('Open'),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();

      await tester.tap(find.text('Cancel'));
      await tester.pumpAndSettle();

      expect(result, isNull);
    });

    testWidgets('submitting returns a Task', (WidgetTester tester) async {
      Task? result;

      await tester.pumpWidget(
        MaterialApp(
          home: Builder(
            builder: (context) => ElevatedButton(
              onPressed: () async {
                result = await showAddTaskDialog(context);
              },
              child: const Text('Open'),
            ),
          ),
        ),
      );

      await tester.tap(find.text('Open'));
      await tester.pumpAndSettle();

      await tester.enterText(find.byType(TextField), 'My new task');
      await tester.tap(find.text('Add Task').last);
      await tester.pumpAndSettle();

      expect(result, isNotNull);
      expect(result!.title, 'My new task');
      expect(result!.done, false);
      expect(result!.deleted, false);
    });
  });
}
