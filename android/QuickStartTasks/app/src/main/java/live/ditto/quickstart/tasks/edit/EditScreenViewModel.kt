package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.data.Task
import java.lang.Exception

class EditScreenViewModel: ViewModel() {

    private var _id: String? = null

    var body = MutableLiveData<String>("")
    var isCompleted = MutableLiveData<Boolean>(false)
    var canDelete = MutableLiveData<Boolean>(false)

    fun setupWithTask(id: String?) {
        canDelete.postValue(id != null)
        val taskId: String = id ?: return

        viewModelScope.launch {
            try {
                val item = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id",
                    mapOf("_id" to taskId)
                ).items.first()

                val task = Task.fromJson(item.jsonString())
                _id = task._id
                body.postValue(task.body)
                isCompleted.postValue(task.isCompleted)
            } catch (e: Exception) {
                Log.e("ERROR:", e.message.toString())
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                if (_id == null) {
                    ditto.store.execute(
                        "INSERT INTO tasks DOCUMENTS (:doc)",
                        mapOf("doc" to mapOf(
                            "body" to body.value,
                            "isCompleted" to isCompleted.value,
                            "isDeleted" to false
                        ))
                    )
                } else {
                    _id?.let { id ->
                        ditto.store.execute("""
                            UPDATE tasks
                            SET
                              body = :body,
                              isCompleted = :isCompleted
                            WHERE _id = :id
                            """,
                            mapOf(
                                "body" to body.value,
                                "isCompleted" to isCompleted.value,
                                "id" to id
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ERROR:", e.message.toString())
            }
        }
    }

    // 4.
    fun delete() {
        viewModelScope.launch {
            try {
                _id?.let { id ->
                    ditto.store.execute(
                        "UPDATE tasks SET isDeleted = true WHERE _id = :id",
                        mapOf("id" to id)
                    )
                }
            } catch (e: Exception) {
                Log.e("ERROR:", e.message.toString())
            }
        }
    }
}