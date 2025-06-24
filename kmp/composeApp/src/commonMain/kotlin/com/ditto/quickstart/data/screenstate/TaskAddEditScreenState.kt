package com.ditto.quickstart.data.screenstate

data class TaskAddEditScreenState(
    val isLoading: Boolean,
    val isEditing: Boolean,
    val taskTitle: String,
)
