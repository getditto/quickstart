package com.ditto.quickstart.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ditto.quickstart.data.screenstate.TaskAddEditScreenState
import com.ditto.quickstart.data.repository.TaskRepository
import com.ditto.quickstart.data.dto.AddTaskDto
import com.ditto.quickstart.data.dto.UpdateTaskTitleDto

class TaskAddEditScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository
) : ViewModel() {
    private val taskId: String? = savedStateHandle["taskId"]

    private val _state = MutableStateFlow(
        TaskAddEditScreenState(
            isLoading = true,
            isEditing = false,
            taskTitle = "",
        )
    )
    val state: StateFlow<TaskAddEditScreenState> = _state
        .asStateFlow()
        .onStart { loadInitialState(taskId = taskId) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = _state.value
        )

    fun onSubmitClick(taskId: String?, title: String) {
        viewModelScope.launch {
            if (taskId.isNullOrEmpty()) {
                val addTaskDto = AddTaskDto(
                    title = title,
                    done = false,
                    deleted = false
                )
                taskRepository.addTask(addTaskDto = addTaskDto)
            } else {
                val updateTaskTitleDto = UpdateTaskTitleDto(
                    id = taskId,
                    title = title
                )
                taskRepository.updateTaskTitle(updateTaskTitleDto = updateTaskTitleDto)
            }
        }
    }

    /**
     * Loads the initial screen state.
     */
    private fun loadInitialState(taskId: String?) {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val task = if (taskId.isNullOrEmpty()) null else taskRepository.getTask(taskId = taskId)
            val isEditing = task != null
            val taskTitle = task?.title ?: ""

            _state.value = _state.value.copy(
                isLoading = false,
                isEditing = isEditing,
                taskTitle = taskTitle
            )
        }
    }
}
