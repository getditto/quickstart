package live.ditto.quickstart.tasks.data

import android.util.Log
import java.util.*
import org.json.JSONObject
import java.lang.Exception

data class Task(
    val _id: String = UUID.randomUUID().toString(),
    val body: String,
    val isCompleted: Boolean
) {
    companion object {
        fun fromJson(jsonString: String): Task {
            return try {
                val json = JSONObject(jsonString)
                Task(
                    _id = json["_id"].toString(),
                    body = json["body"].toString(),
                    isCompleted = json["isCompleted"] as Boolean
                )
            } catch (e: Exception) {
                Log.e("ERROR:", e.message.toString())
                Task(body = "", isCompleted = false)
            }
        }
    }
}
