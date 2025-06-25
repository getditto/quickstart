package com.ditto.quickstart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.data.screenstate.TaskListScreenState
import com.ditto.quickstart.data.repository.TaskRepository
import com.ditto.quickstart.data.dto.UpdateTaskDoneDto

class TaskListScreenViewModel(
    private val taskRepository: TaskRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(
        TaskListScreenState(
            isLoading = true,
            tasks = emptyList()
        )
    )
    val state: StateFlow<TaskListScreenState> = _state
        .asStateFlow()
        .onStart {
            observerTasksChanges()
            _state.value = _state.value.copy(isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = _state.value
        )

    private fun observerTasksChanges() {
        taskRepository.tasksStateFlow
            .onEach { _state.value = _state.value.copy(tasks = it) }
            .launchIn(viewModelScope)
    }

    fun onCheck(task: Task, checked: Boolean) {
        viewModelScope.launch {
            val updateTaskDoneDto = UpdateTaskDoneDto(
                id = task.id,
                done = checked
            )
            taskRepository.updateTaskDone(updateTaskDoneDto = updateTaskDoneDto)
        }
    }
}
