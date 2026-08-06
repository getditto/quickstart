package com.ditto.quickstart.data.repository

import com.ditto.kotlin.DittoQueryResultItem
import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.data.dto.AddTaskDto
import com.ditto.quickstart.data.dto.UpdateTaskDoneDto
import com.ditto.quickstart.data.dto.UpdateTaskTitleDto
import com.ditto.quickstart.ditto.DittoManager

// The subscription controls what syncs to this device and is written to the
// local database; the observer (QUERY_SELECT_TASKS below) reacts to changes in
// that local database, hiding soft-deleted tasks from the displayed list.
private const val SUBSCRIPTION_QUERY_SELECT_TASKS = """
SELECT * FROM tasks
"""

private const val QUERY_SELECT_TASKS = """
SELECT * FROM tasks WHERE NOT deleted
"""

private const val QUERY_SELECT_TASK = """
SELECT * FROM tasks WHERE _id = :id
"""

private const val QUERY_INSERT_TASK = """
INSERT INTO tasks DOCUMENTS (:task)
"""

private const val QUERY_UPDATE_TASK_TITLE = """
UPDATE tasks SET title = :title WHERE _id = :id
"""

private const val QUERY_UPDATE_TASK_DONE = """
UPDATE tasks SET done = :done WHERE _id = :id
"""

private const val QUERY_UPDATE_TASK_DELETED = """
UPDATE tasks SET deleted = true WHERE _id = :id
"""

class TasksRepository(
    private val dittoManager: DittoManager,
) : TaskRepository {
    private var syncSubscription: DittoSyncSubscription? = null

    // Emits the visible task list (soft-deleted tasks excluded) and re-emits on every
    // change to the local store. Registers the sync subscription when collection starts.
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    override fun observeTasks(): Flow<List<Task>> = flow {
        registerSubscription()
        val ditto = dittoManager.getDitto() ?: return@flow
        emitAll(
            ditto.store.observe(QUERY_SELECT_TASKS) { result ->
                result.items.map { item -> item.toTask() }
            }
        )
    }

    override suspend fun getTask(taskId: String): Task? =
        dittoManager.getDitto()?.store?.execute(
            QUERY_SELECT_TASK,
            mapOf("id" to taskId)
        ) { result ->
            result.items.firstOrNull()?.toTask()
        }

    override suspend fun addTask(addTaskDto: AddTaskDto) {
        dittoManager.getDitto()?.store?.execute(
            QUERY_INSERT_TASK,
            mapOf(
                "task" to mapOf(
                    "title" to addTaskDto.title,
                    "done" to addTaskDto.done,
                    "deleted" to addTaskDto.deleted,
                )
            )
        )
    }

    override suspend fun updateTaskTitle(updateTaskTitleDto: UpdateTaskTitleDto) {
        dittoManager.getDitto()?.store?.execute(
            QUERY_UPDATE_TASK_TITLE,
            mapOf(
                "title" to updateTaskTitleDto.title,
                "id" to updateTaskTitleDto.id,
            )
        )
    }

    override suspend fun updateTaskDone(updateTaskDoneDto: UpdateTaskDoneDto) {
        dittoManager.getDitto()?.store?.execute(
            QUERY_UPDATE_TASK_DONE,
            mapOf(
                "id" to updateTaskDoneDto.id,
                "done" to updateTaskDoneDto.done,
            )
        )
    }

    override suspend fun removeTask(taskId: String) {
        dittoManager.getDitto()?.store?.execute(
            QUERY_UPDATE_TASK_DELETED,
            mapOf("id" to taskId)
        )
    }

    override fun onCleared() {
        syncSubscription?.close()
        syncSubscription = null
    }

    private fun DittoQueryResultItem.toTask(): Task = Task(
        id = this.value["_id"].string,
        title = this.value["title"].string,
        done = this.value["done"].boolean,
        deleted = this.value["deleted"].boolean,
    )

    // Register a subscription, which determines what data syncs to this device.
    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
    private suspend fun registerSubscription() {
        if (syncSubscription != null) return
        syncSubscription =
            dittoManager.getDitto()?.sync?.registerSubscription(SUBSCRIPTION_QUERY_SELECT_TASKS)
    }
}
