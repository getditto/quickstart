package com.ditto.quickstart.data.repository

import kotlinx.coroutines.flow.Flow
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.data.dto.AddTaskDto
import com.ditto.quickstart.data.dto.UpdateTaskDoneDto
import com.ditto.quickstart.data.dto.UpdateTaskTitleDto

interface TaskRepository {
    // Emits the visible task list (soft-deleted tasks excluded) and re-emits whenever
    // the local store changes.
    fun observeTasks(): Flow<List<Task>>

    suspend fun getTask(taskId: String): Task?
    suspend fun addTask(addTaskDto: AddTaskDto)
    suspend fun updateTaskTitle(updateTaskTitleDto: UpdateTaskTitleDto)
    suspend fun updateTaskDone(updateTaskDoneDto: UpdateTaskDoneDto)
    suspend fun removeTask(taskId: String)
    fun onCleared()
}
