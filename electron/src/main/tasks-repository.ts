import { Ditto, StoreObserver, SyncSubscription } from '@dittolive/ditto';
import type { Task } from '../types';

// Owns the tasks concern against a ready Ditto instance: registers the
// subscription and observer, holds the latest observed task list, and
// exposes CRUD. Calls the Ditto API directly.
export class TasksRepository {
  private subscription: SyncSubscription | null = null;
  private observer: StoreObserver | null = null;
  private currentTasks: Task[] = [];

  constructor(
    private readonly ditto: Ditto,
    private readonly onTasksUpdated: (tasks: Task[]) => void,
  ) {}

  start(): void {
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    this.subscription = this.ditto.sync.registerSubscription(
      'SELECT * FROM tasks',
    );

    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    this.observer = this.ditto.store.registerObserver<Task>(
      'SELECT * FROM tasks WHERE NOT deleted',
      (results) => {
        this.currentTasks = results.items.map((item) => item.value);
        this.onTasksUpdated(this.currentTasks);
      },
    );
  }

  // Returns the latest observed task list. Lets the renderer hydrate its
  // initial state after registering its onTasksUpdated listener, closing
  // the race where the observer may have fired during sync before
  // the BrowserWindow was ready to receive IPC events.
  getTasks(): Task[] {
    return this.currentTasks;
  }

  // https://docs.ditto.live/sdk/latest/crud/create
  async createTask(title: string): Promise<void> {
    await this.ditto.store.execute('INSERT INTO tasks DOCUMENTS (:task)', {
      task: { title, done: false, deleted: false },
    });
  }

  // https://docs.ditto.live/sdk/latest/crud/update
  async editTask(id: string, title: string): Promise<void> {
    await this.ditto.store.execute(
      'UPDATE tasks SET title = :title WHERE _id = :id',
      { id, title },
    );
  }

  async toggleTask(id: string, done: boolean): Promise<void> {
    await this.ditto.store.execute(
      'UPDATE tasks SET done = :done WHERE _id = :id',
      { id, done },
    );
  }

  // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
  async deleteTask(id: string): Promise<void> {
    await this.ditto.store.execute(
      'UPDATE tasks SET deleted = true WHERE _id = :id',
      { id },
    );
  }

  dispose(): void {
    this.observer?.cancel();
    this.subscription?.cancel();
    this.observer = null;
    this.subscription = null;
    this.currentTasks = [];
  }
}
