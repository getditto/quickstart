// Owns the tasks concern against a ready Ditto instance: registers the tasks
// subscription and observer, streams the observed task list to a callback, and
// exposes CRUD. Calls the Ditto API directly.
export class TasksRepository {
  constructor(ditto, onTasksUpdated) {
    this.ditto = ditto;
    this.onTasksUpdated = onTasksUpdated;
    this.subscription = null;
    this.observer = null;
  }

  // Registers the subscription (what syncs to this peer) and the observer
  // (what is read from the local database).
  start() {
    // Register a subscription, which determines what data syncs to this peer
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    this.subscription = this.ditto.sync.registerSubscription(
      'SELECT * FROM tasks',
    );

    // Register observer, which runs against the local database on this peer
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    this.observer = this.ditto.store.registerObserver(
      'SELECT * FROM tasks WHERE NOT deleted',
      (result) => {
        const tasks = result.items.map((item) => item.value);
        this.onTasksUpdated(tasks);
      },
    );
  }

  // https://docs.ditto.live/sdk/latest/crud/create
  async createTask(title) {
    await this.ditto.store.execute('INSERT INTO tasks DOCUMENTS (:task)', {
      task: {
        title,
        done: false,
        deleted: false,
      },
    });
  }

  // https://docs.ditto.live/sdk/latest/crud/update
  async editTask(id, title) {
    await this.ditto.store.execute(
      'UPDATE tasks SET title = :title WHERE _id = :id',
      {
        id,
        title,
      },
    );
  }

  // https://docs.ditto.live/sdk/latest/crud/update
  async toggleTask(task) {
    await this.ditto.store.execute(
      'UPDATE tasks SET done = :done WHERE _id = :id',
      {
        id: task._id,
        done: !task.done,
      },
    );
  }

  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  async deleteTask(task) {
    await this.ditto.store.execute(
      'UPDATE tasks SET deleted = true WHERE _id = :id',
      {
        id: task._id,
      },
    );
  }

  // Cancel the subscription and observer on teardown.
  dispose() {
    this.subscription?.cancel();
    this.observer?.cancel();
    this.subscription = null;
    this.observer = null;
  }
}
