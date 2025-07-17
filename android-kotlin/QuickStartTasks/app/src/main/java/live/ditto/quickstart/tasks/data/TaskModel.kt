package live.ditto.quickstart.tasks.data

import android.util.Log
import org.json.JSONObject
import java.util.UUID

data class TaskModel(
    val _id: String = UUID.randomUUID().toString(),
    var title: String,
    var done: Boolean = false,
    var deleted: Boolean = false,
) {
    companion object {
        private const val TAG = "TaskModel"
        fun fromMap(value: Map<String, Any?>) : TaskModel {
            return try {
                TaskModel(
                    _id = value["_id"].toString(),
                    title = value["title"].toString(),
                    done = value["done"] as Boolean,
                    deleted = value["deleted"] as Boolean
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to convert JSON to Task", e)
                TaskModel(title = "", done = false, deleted = false)
            }
        }

        fun fromJson(jsonString: String): TaskModel {
            return try {
                val json = JSONObject(jsonString)
                TaskModel(
                    _id = json["_id"].toString(),
                    title = json["title"].toString(),
                    done = json["done"] as Boolean,
                    deleted = json["deleted"] as Boolean
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unable to convert JSON to Task", e)
                TaskModel(title = "", done = false, deleted = false)
            }
        }
    }

    override fun toString(): String {
        return try {
            JSONObject().apply {
                put("_id", _id)
                put("title", title)
                put("done", done)
                put("deleted", deleted)
            }.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Unable to convert Task to JSON", e)
            "{}"
        }
    }
}
