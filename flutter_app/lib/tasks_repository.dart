import 'package:ditto_live/ditto_live.dart';

/// Owns the tasks concern against a ready Ditto instance: registers the tasks
/// subscription and observer, exposes the observed task results as a stream,
/// and provides CRUD. It calls the real Ditto API directly.
class TasksRepository {
  final Ditto _ditto;

  // https://docs.ditto.live/sdk/latest/crud/observing-data-changes
  StoreObserver? _observer;

  // https://docs.ditto.live/sdk/latest/sync/syncing-data
  SyncSubscription? _subscription;

  TasksRepository(this._ditto);

  /// Registers the subscription (what syncs to this peer) and the observer
  /// (what is read from the local database). The subscription is a superset of
  /// the observer query so it syncs every task, while the observer filters out
  /// soft-deleted tasks for display.
  void start() {
    // Register a subscription, which determines what data syncs to this peer
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    _subscription = _ditto.sync.registerSubscription(
      "SELECT * FROM tasks",
    );

    // Register observer, which runs against the local database on this peer
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    _observer = _ditto.store.registerObserver(
      "SELECT * FROM tasks WHERE NOT deleted",
    );
  }

  /// Stream of query results for the observed tasks. Emits whenever the
  /// matching set of tasks in the local store changes.
  Stream<QueryResult>? get changes => _observer?.changes;

  // https://docs.ditto.live/sdk/latest/crud/create
  Future<void> addTask(Map<String, dynamic> task) async {
    await _ditto.store.execute(
      "INSERT INTO tasks DOCUMENTS (:task)",
      arguments: {"task": task},
    );
  }

  // https://docs.ditto.live/sdk/latest/crud/update
  Future<void> setDone(String id, bool done) async {
    await _ditto.store.execute(
      "UPDATE tasks SET done = :done WHERE _id = :id",
      arguments: {"done": done, "id": id},
    );
  }

  // https://docs.ditto.live/sdk/latest/crud/update
  Future<void> updateTitle(String id, String title) async {
    await _ditto.store.execute(
      "UPDATE tasks SET title = :title WHERE _id = :id",
      arguments: {"title": title, "id": id},
    );
  }

  // Use the Soft-Delete pattern
  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  Future<void> deleteTask(String id) async {
    await _ditto.store.execute(
      "UPDATE tasks SET deleted = true WHERE _id = :id",
      arguments: {"id": id},
    );
  }

  /// Cancel the subscription and observer on teardown.
  void dispose() {
    _observer?.cancel();
    _subscription?.cancel();
    _observer = null;
    _subscription = null;
  }
}
