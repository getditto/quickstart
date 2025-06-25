package com.ditto.quickstart.data.repository

import com.ditto.kotlin.DittoQueryResultItem
import com.ditto.kotlin.DittoSyncSubscription
import com.ditto.kotlin.serialization.DittoCborSerializable
import com.ditto.kotlin.serialization.DittoCborSerializable.Utf8String
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.data.dto.AddTaskDto
import com.ditto.quickstart.data.dto.toDittoDictionary
import com.ditto.quickstart.data.dto.UpdateTaskDoneDto
import com.ditto.quickstart.data.dto.UpdateTaskTitleDto
import com.ditto.quickstart.ditto.DittoManager

private const val QUERY_SELECT_TASKS = """
SELECT * FROM tasks WHERE NOT deleted ORDER BY _id
"""

private const val QUERY_SELECT_TASK = """
SELECT * FROM tasks WHERE deleted = false AND _id = :taskId LIMIT 1
"""

private const val QUERY_INSERT_TASK = """
INSERT INTO tasks DOCUMENTS (:task)
"""

private const val QUERY_UPDATE_TASK_TITLE = """
UPDATE tasks SET title = :title WHERE _id = :taskId
"""

private const val QUERY_UPDATE_TASK_DONE = """
UPDATE tasks SET done = :done WHERE _id = :taskId
"""

private const val QUERY_UPDATE_TASK_DELETED = """
UPDATE tasks SET deleted = :deleted WHERE _id = :taskId
"""

class DittoTaskRepository(
    private val dittoManager: DittoManager,
) : TaskRepository {
    private var syncSubscription: DittoSyncSubscription? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val tasksMutableStateFlow = MutableStateFlow(emptyList<Task>())
    override val tasksStateFlow: StateFlow<List<Task>> = tasksMutableStateFlow.asStateFlow()
        .onStart {
            registerSubscription()
            registerObserver()
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.Companion.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    override suspend fun getTask(taskId: String): Task? {
        return dittoManager.executeDql(
            query = QUERY_SELECT_TASK,
            parameters = DittoCborSerializable.Dictionary(
                mapOf(Utf8String("taskId") to Utf8String(taskId))
            )
        )?.items?.firstOrNull()?.toTask()
    }

    override suspend fun addTask(addTaskDto: AddTaskDto) {
        dittoManager.executeDql(
            query = QUERY_INSERT_TASK,
            parameters = DittoCborSerializable.Dictionary(
                mapOf(Utf8String("task") to addTaskDto.toDittoDictionary())
            )
        )
    }

    override suspend fun updateTaskTitle(updateTaskTitleDto: UpdateTaskTitleDto) {
        dittoManager.executeDql(
            query = QUERY_UPDATE_TASK_TITLE,
            parameters = DittoCborSerializable.Dictionary(mapOf(
                Utf8String("title") to Utf8String(updateTaskTitleDto.title),
                Utf8String("taskId") to Utf8String(updateTaskTitleDto.id),
            ))
        )
    }

    override suspend fun updateTaskDone(updateTaskDoneDto: UpdateTaskDoneDto) {
        dittoManager.executeDql(
            query = QUERY_UPDATE_TASK_DONE,
            parameters = DittoCborSerializable.Dictionary(mapOf(
                Utf8String("taskId") to  Utf8String(updateTaskDoneDto.id),
                Utf8String("done") to DittoCborSerializable.BooleanValue(updateTaskDoneDto.done)
            ))
        )
    }

    override suspend fun removeTask(taskId: String) {
        dittoManager.executeDql(
            query = QUERY_UPDATE_TASK_DELETED,
            parameters = DittoCborSerializable.Dictionary(mapOf(
                Utf8String("deleted") to DittoCborSerializable.BooleanValue(true),
                Utf8String("taskId") to Utf8String(taskId),
            ))
        )
    }

    override fun onCleared() {
        syncSubscription?.close()
        syncSubscription = null

        scope.cancel()
    }

    private fun DittoQueryResultItem.toTask(): Task = Task(
        id = this.value["_id"].string,
        title = this.value["title"].string,
        done = this.value["done"].boolean,
        deleted = this.value["deleted"].boolean,
    )

    private suspend fun registerSubscription() {
        syncSubscription = dittoManager.registerSubscription(QUERY_SELECT_TASKS)
    }

    private suspend fun registerObserver() {
        val observer = dittoManager.registerObserver(QUERY_SELECT_TASKS)
        scope.launch {
            observer
                .map { result -> result.items.map { item -> item.toTask() } }
                .collect {
                    tasksMutableStateFlow.value = it
                }
        }
    }
}
