package live.ditto.quickstart.tasks.data

import android.util.Log
import java.util.*
import org.json.JSONObject
import java.lang.Exception

data class Task(
    val _id: String = UUID.randomUUID().toString(),
    val title: String,
    val done: Boolean,
    val deleted: Boolean
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
                Log.e(TAG, e.message.toString())
                Task(title = "", done = false, deleted = false)
            }
        }
    }
}
