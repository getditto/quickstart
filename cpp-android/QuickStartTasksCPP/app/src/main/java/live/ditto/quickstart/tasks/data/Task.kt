package live.ditto.quickstart.tasks.data

import org.json.JSONObject
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
) {
    companion object {
        fun fromJson(jsonString: String): Task {
            val json = JSONObject(jsonString)
            return Task(
                _id = json["_id"].toString(),
                title = json["title"].toString(),
                done = json["done"] as Boolean,
                deleted = json["deleted"] as Boolean
            )
        }
    }
}
