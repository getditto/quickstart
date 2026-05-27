// Unit tests for the [Task] domain model. Earlier this file lived at
// `test/widget_test.dart` and held a gutted smoke test that instantiated
// `DittoExample` directly; the layered refactor removed `DittoExample`, so
// the file's coverage moved to round-tripping `Task` JSON.
//
// Integration coverage lives in `integration_test/app_test.dart`, which
// drives the real app against the cloud.

import 'package:flutter_quickstart/domain/models/task.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('Task domain model', () {
    test('toJson omits null id', () {
      const task = Task(title: 'Buy milk', done: false, deleted: false);
      final json = task.toJson();
      expect(json.containsKey('_id'), isFalse);
      expect(json['title'], 'Buy milk');
      expect(json['done'], false);
      expect(json['deleted'], false);
    });

    test('toJson includes id when present', () {
      const task = Task(
        id: 'task-123',
        title: 'Buy milk',
        done: true,
        deleted: false,
      );
      final json = task.toJson();
      expect(json['_id'], 'task-123');
      expect(json['done'], true);
    });

    test('fromJson round-trips', () {
      const original = Task(
        id: 'task-123',
        title: 'Buy milk',
        done: true,
        deleted: false,
      );
      final roundTripped = Task.fromJson(original.toJson());
      expect(roundTripped.id, original.id);
      expect(roundTripped.title, original.title);
      expect(roundTripped.done, original.done);
      expect(roundTripped.deleted, original.deleted);
    });
  });
}
