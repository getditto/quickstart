package com.example.dittotasks

import android.util.Log
import com.ditto.kotlin.DittoStoreObserver
import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

/**
 * Owns the "tasks" data concern: the sync subscription (which tasks sync to this
 * device) and the task CRUD operations. It calls the real Ditto API directly through
 * [DittoManager.ditto] — there is no pass-through/wrapper layer over Ditto.
 *
 * This is a process-global singleton so the subscription is registered once and lives
 * for the whole app, independent of any Activity. The store observer, by contrast,
 * belongs to the UI: [observeTasks] returns the observer handle for the caller to hold
 * and close on view teardown (see [MainActivity.onDestroy]).
 */
object TasksRepository {
    private var subscription: DittoSyncSubscription? = null

    // Register a subscription, which determines what data syncs to this device. Idempotent —
    // registered once for the life of the app.
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    @JvmStatic
    fun registerSubscription() {
        if (subscription == null) {
            subscription = DittoManager.ditto.sync.registerSubscription(SUBSCRIPTION_QUERY)
        }
    }

    // Observe the local store for changes (soft-deleted tasks excluded), delivering the
    // visible task list to [onTasksChanged] on every change. The returned observer handle
    // is owned by the caller (the UI) and must be closed on view teardown.
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    @JvmStatic
    fun observeTasks(
        onTasksChanged: Consumer<@JvmSuppressWildcards List<Task>>
    ): DittoStoreObserver =
        DittoManager.ditto.store.registerObserver(OBSERVER_QUERY) { result ->
            Log.d(TAG, "Observer callback triggered with ${result.items.size} items")
            val tasks = result.items.map { Task.fromQueryItem(it) }
            onTasksChanged.accept(tasks)
        }

    @JvmStatic
    fun createTask(title: String) {
        val task = mapOf(
            "title" to title,
            "done" to false,
            "deleted" to false
        )

        try {
            // Add tasks into the ditto collection using a DQL INSERT statement.
            // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
            runBlocking {
                DittoManager.ditto.store.execute(
                    "INSERT INTO tasks DOCUMENTS (:task)",
                    mapOf("task" to task)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task write failed", e)
        }
    }

    @JvmStatic
    fun editTaskTitle(task: Task, newTitle: String) {
        try {
            // Update tasks in the ditto collection using a DQL UPDATE statement.
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            runBlocking {
                DittoManager.ditto.store.execute(
                    "UPDATE tasks SET title = :title WHERE _id = :id",
                    mapOf("id" to task.id, "title" to newTitle)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task write failed", e)
        }
    }

    @JvmStatic
    fun toggleTask(task: Task) {
        try {
            // Update tasks in the ditto collection using a DQL UPDATE statement.
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            runBlocking {
                DittoManager.ditto.store.execute(
                    "UPDATE tasks SET done = :done WHERE _id = :id",
                    mapOf("id" to task.id, "done" to !task.isDone)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task write failed", e)
        }
    }

    @JvmStatic
    fun deleteTask(task: Task) {
        try {
            // UPDATE DQL statement using the soft-delete pattern.
            // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
            runBlocking {
                DittoManager.ditto.store.execute(
                    "UPDATE tasks SET deleted = true WHERE _id = :id",
                    mapOf("id" to task.id)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Task write failed", e)
        }
    }

    private const val TAG = "TasksRepository"

    // The subscription controls what syncs to this device; the observer reacts to
    // changes in the local database, hiding soft-deleted tasks from the list.
    private const val SUBSCRIPTION_QUERY = "SELECT * FROM tasks"
    private const val OBSERVER_QUERY =
        "SELECT * FROM tasks WHERE NOT deleted"
}
