package live.ditto.quickstart.tasks.data

import java.util.UUID

// Kotlin representation of a to-do item.
//
// Note: JNI code depends on the names, order, and types of these fields.
// Do not change anything about this declaration without also changing the
// associated C++ code.
data class Task(
    val _id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false,
    val deleted: Boolean = false,
)
