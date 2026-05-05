package live.ditto.quickstart.tasks.data

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

data class Task(
    val _id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false,
    val deleted: Boolean = false,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "_id" to _id,
        "title" to title,
        "done" to done,
        "deleted" to deleted
    )

    companion object {
        private const val TAG = "Task"

        fun fromJson(jsonString: String): Task {
            return try {
                val json = JSONObject(jsonString)
                Task(
                    _id = json.optString("_id", UUID.randomUUID().toString()),
                    title = json.optString("title", ""),
                    done = json.optBoolean("done", false),
                    deleted = json.optBoolean("deleted", false)
                )
            } catch (e: JSONException) {
                Log.e(TAG, "Unable to convert JSON to Task", e)
                Task(title = "")
            }
        }
    }
}
