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

    test('fromJson with no _id key produces task with null id', () {
      // The store returns docs that may pre-date id assignment — tolerate.
      final task = Task.fromJson(const {
        'title': 'Buy milk',
        'done': false,
        'deleted': false,
      });
      expect(task.id, isNull);
      expect(task.title, 'Buy milk');
      expect(task.done, false);
      expect(task.deleted, false);
    });

    test('fromJson preserves deleted=true (soft-delete tombstone)', () {
      final task = Task.fromJson(const {
        '_id': 'task-456',
        'title': 'Tombstone',
        'done': false,
        'deleted': true,
      });
      expect(task.deleted, isTrue);
      // Tombstone titles still round-trip — the query layer filters by
      // `WHERE deleted = false`, but the model itself is permissive.
      expect(task.title, 'Tombstone');
    });

    test('toJson is shaped for INSERT INTO tasks DOCUMENTS (:task)', () {
      // Asserts the document shape the Repository hands to Ditto. If this
      // contract drifts (e.g. someone renames `title` to `text`), other
      // quickstarts' interop breaks per CLAUDE.md cross-platform note.
      const task = Task(title: 'a', done: false, deleted: false);
      final json = task.toJson();
      expect(json.keys.toSet(), {'title', 'done', 'deleted'});
      expect(json.containsKey('_id'), isFalse);
    });
  });
}
