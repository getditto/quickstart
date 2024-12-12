package live.ditto.quickstart.tasks.data

import android.util.Log
import org.json.JSONObject
import java.util.UUID

data class Task(
    val _id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean = false,
    val deleted: Boolean = false,
) {
    companion object {
        private const val TAG = "Task"

        fun fromJson(jsonString: String): Task {
            return try {
                val json = JSONObject(jsonString)
                Task(
                    _id = json["_id"].toString(),
                    title = json["title"].toString(),
                    done = json["done"] as Boolean,
                    deleted = json["deleted"] as Boolean
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to convert JSON to Task", e)
                Task(title = "", done = false, deleted = false)
            }
        }
    }
}
