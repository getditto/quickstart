package com.ditto.quickstart.data.screenstate

import androidx.compose.runtime.Immutable
import com.ditto.quickstart.data.Task

@Immutable
data class TaskListScreenState(
    val isLoading: Boolean,
    val tasks: List<Task>
)
