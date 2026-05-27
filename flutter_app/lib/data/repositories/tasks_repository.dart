import 'package:ditto_live/ditto_live.dart';
import 'package:flutter_quickstart/data/services/ditto_service.dart';
import 'package:flutter_quickstart/domain/models/task.dart';

/// Single source of truth for [Task] data. Transforms raw DQL results into
/// [Task] domain models and exposes parameterized CRUD methods so callers
/// never build DQL strings.
///
/// Lifecycle:
/// - The [SyncSubscription] is registered in the constructor. Construct this
///   *before* calling [DittoService.startSync] so the subscription is included
///   in the first sync round-trip with the cloud.
/// - The [StoreObserver] is registered lazily on the first call to
///   [watchTasks]. Observer registration timing does not affect sync; only
///   the subscription does.
class TasksRepository {
  TasksRepository(this._service)
      : _subscription = _service.ditto.sync.registerSubscription(_tasksQuery);

  static const _tasksQuery =
      'SELECT * FROM tasks WHERE deleted = false ORDER BY title ASC';

  final DittoService _service;
  // Held to keep the subscription alive for the lifetime of this repository
  // and to cancel it cleanly in [dispose].
  final SyncSubscription _subscription;
  StoreObserver? _observer;

  Stream<List<Task>> watchTasks() {
    final observer = _observer ??= _service.ditto.store.registerObserver(
      _tasksQuery,
    );
    return observer.changes.map(
      (result) =>
          result.items.map((item) => Task.fromJson(item.value)).toList(),
    );
  }

  Future<void> addTask(Task task) => _service.ditto.store.execute(
        'INSERT INTO tasks DOCUMENTS (:task)',
        arguments: {'task': task.toJson()},
      );

  Future<void> setDone({required String id, required bool done}) =>
      _service.ditto.store.execute(
        'UPDATE tasks SET done = :done WHERE _id = :id',
        arguments: {'id': id, 'done': done},
      );

  Future<void> updateTitle({required String id, required String title}) =>
      _service.ditto.store.execute(
        'UPDATE tasks SET title = :title WHERE _id = :id',
        arguments: {'id': id, 'title': title},
      );

  Future<void> softDelete(String id) => _service.ditto.store.execute(
        'UPDATE tasks SET deleted = true WHERE _id = :id',
        arguments: {'id': id},
      );

  Future<void> evictAll() =>
      _service.ditto.store.execute('EVICT FROM tasks WHERE true');

  void dispose() {
    _observer?.cancel();
    _subscription.cancel();
    _observer = null;
  }
}
