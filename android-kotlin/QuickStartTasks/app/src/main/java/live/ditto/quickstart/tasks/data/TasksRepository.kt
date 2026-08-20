package live.ditto.quickstart.tasks.data

import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.flow.Flow
import live.ditto.quickstart.tasks.DittoManager

/**
 * Owns everything specific to the tasks data: the sync subscription, the store
 * observer that streams the current task list, and the task CRUD operations. It
 * talks to Ditto directly through the instance vended by [DittoManager].
 */
object TasksRepository {

    // The subscription controls what syncs to this device and is written to the
    // local database; the observer reacts to changes in that local database,
    // hiding soft-deleted tasks from the displayed list.
    private const val SUBSCRIPTION_QUERY = "SELECT * FROM tasks"
    private const val OBSERVER_QUERY =
        "SELECT * FROM tasks WHERE NOT deleted"

    private var syncSubscription: DittoSyncSubscription? = null

    // Observe the local store for changes, hiding soft-deleted tasks.
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    fun observeTasks(): Flow<List<Task>> =
        DittoManager.ditto.store.observe(OBSERVER_QUERY) { result ->
            result.items.map { item -> Task.fromJson(item.jsonString()) }
        }

    // Register a subscription, which determines what data syncs to this peer.
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    fun registerSubscription() {
        if (syncSubscription == null) {
            syncSubscription = DittoManager.ditto.sync.registerSubscription(SUBSCRIPTION_QUERY)
        }
    }

    // Update tasks in the ditto collection using a DQL UPDATE statement.
    // https://docs.ditto.live/sdk/latest/crud/update#updating
    suspend fun toggle(task: Task) {
        DittoManager.ditto.store.execute(
            "UPDATE tasks SET done = :done WHERE _id = :id",
            mapOf("done" to !task.done, "id" to task._id)
        )
    }

    // UPDATE DQL statement using the soft-delete pattern.
    // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
    suspend fun delete(taskId: String) {
        DittoManager.ditto.store.execute(
            "UPDATE tasks SET deleted = true WHERE _id = :id",
            mapOf("id" to taskId)
        )
    }

    // Load a single task by ID, used to pre-fill the edit form.
    suspend fun getTask(taskId: String): Task? =
        DittoManager.ditto.store.execute(
            "SELECT * FROM tasks WHERE _id = :id",
            mapOf("id" to taskId)
        ) { result ->
            result.items.firstOrNull()?.let { Task.fromJson(it.jsonString()) }
        }

    // Add a task to the ditto collection using a DQL INSERT statement.
    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
    suspend fun insertTask(title: String, done: Boolean) {
        DittoManager.ditto.store.execute(
            "INSERT INTO tasks DOCUMENTS (:task)",
            mapOf(
                "task" to mapOf(
                    "title" to title,
                    "done" to done,
                    "deleted" to false
                )
            )
        )
    }

    // Update the title and done state of an existing task.
    // https://docs.ditto.live/sdk/latest/crud/update#updating
    suspend fun updateTask(id: String, title: String, done: Boolean) {
        DittoManager.ditto.store.execute(
            """
            UPDATE tasks
            SET
              title = :title,
              done = :done
            WHERE _id = :id
            """,
            mapOf(
                "title" to title,
                "done" to done,
                "id" to id
            )
        )
    }
}
