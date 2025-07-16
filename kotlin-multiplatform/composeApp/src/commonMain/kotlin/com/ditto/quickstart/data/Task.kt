package com.ditto.quickstart.data

data class Task(
    val id: String,
    val title: String,
    val done: Boolean,
    val deleted: Boolean,
)
