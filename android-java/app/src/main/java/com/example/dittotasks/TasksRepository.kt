package com.example.dittotasks

import android.util.Log
import com.ditto.kotlin.DittoStoreObserver
import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

/**
 * Owns the "tasks" data concern: the subscription that determines which tasks sync
 * to this device, the observer that streams the visible task list, and CRUD.
 *
 * It calls the real Ditto API directly through [DittoManager.ditto] — there is no
 * pass-through/wrapper layer over Ditto.
 */
class TasksRepository(
    private val dittoManager: DittoManager,
) {
    private var subscription: DittoSyncSubscription? = null
    private var observer: DittoStoreObserver? = null

    // Register a subscription, which determines what data syncs to this device.
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    fun registerSubscription() {
        if (subscription == null) {
            subscription = dittoManager.ditto.sync.registerSubscription(SUBSCRIPTION_QUERY)
        }
    }

    // Passes the visible task list (soft-deleted tasks excluded) to [onTasksChanged]
    // on every change to the local store. The returned observer handle is retained so
    // it keeps firing.
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    fun observeTasks(
        onTasksChanged: Consumer<@JvmSuppressWildcards List<Task>>
    ): DittoStoreObserver =
        dittoManager.ditto.store.registerObserver(OBSERVER_QUERY) { result ->
            Log.d(TAG, "Observer callback triggered with ${result.items.size} items")
            val tasks = result.items.map { Task.fromQueryItem(it) }
            onTasksChanged.accept(tasks)
        }.also { observer = it }

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
                dittoManager.ditto.store.execute(
                    "INSERT INTO tasks DOCUMENTS (:task)",
                    mapOf("task" to task)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun editTaskTitle(task: Task, newTitle: String) {
        try {
            // Update tasks in the ditto collection using a DQL UPDATE statement.
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            runBlocking {
                dittoManager.ditto.store.execute(
                    "UPDATE tasks SET title = :title WHERE _id = :id",
                    mapOf("id" to task.id, "title" to newTitle)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun toggleTask(task: Task) {
        try {
            // Update tasks in the ditto collection using a DQL UPDATE statement.
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            runBlocking {
                dittoManager.ditto.store.execute(
                    "UPDATE tasks SET done = :done WHERE _id = :id",
                    mapOf("id" to task.id, "done" to !task.isDone)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deleteTask(task: Task) {
        try {
            // UPDATE DQL statement using the soft-delete pattern.
            // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
            runBlocking {
                dittoManager.ditto.store.execute(
                    "UPDATE tasks SET deleted = true WHERE _id = :id",
                    mapOf("id" to task.id)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "TasksRepository"

        // The subscription controls what syncs to this device; the observer reacts to
        // changes in the local database, hiding soft-deleted tasks from the list.
        private const val SUBSCRIPTION_QUERY = "SELECT * FROM tasks"
        private const val OBSERVER_QUERY =
            "SELECT * FROM tasks WHERE NOT deleted"
    }
}
