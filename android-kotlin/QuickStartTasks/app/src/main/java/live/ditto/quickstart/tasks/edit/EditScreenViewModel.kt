package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.data.Task

class EditScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "EditScreenViewModel"
    }

    private var _id: String? = null

    var title = MutableLiveData<String>("")
    var done = MutableLiveData<Boolean>(false)
    var canDelete = MutableLiveData<Boolean>(false)

    fun setupWithTask(id: String?) {
        canDelete.postValue(id != null)
        val taskId: String = id ?: return

        viewModelScope.launch {
            try {
                val item = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId)
                ).items.first()

                val task = Task.fromJson(item.jsonString())
                _id = task._id
                title.postValue(task.title)
                done.postValue(task.done)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to setup view task data", e)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                if (_id == null) {
                    // Add tasks into the ditto collection using DQL INSERT statement
                    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                    ditto.store.execute(
                        "INSERT INTO tasks DOCUMENTS (:doc)",
                        mapOf(
                            "doc" to mapOf(
                                "title" to title.value,
                                "done" to done.value,
                                "deleted" to false
                            )
                        )
                    )
                } else {
                    // Update tasks into the ditto collection using DQL UPDATE statement
                    // https://docs.ditto.live/sdk/latest/crud/update#updating
                    _id?.let { id ->
                        ditto.store.execute(
                            """
                            UPDATE tasks
                            SET
                              title = :title,
                              done = :done
                            WHERE _id = :id
                            AND NOT deleted
                            """,
                            mapOf(
                                "title" to title.value,
                                "done" to done.value,
                                "id" to id
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to save task", e)
            }
        }
    }

    fun delete() {
        // UPDATE DQL Statement using Soft-Delete pattern
        // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
        viewModelScope.launch {
            try {
                _id?.let { id ->
                    ditto.store.execute(
                        "UPDATE tasks SET deleted = true WHERE _id = :id",
                        mapOf("id" to id)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
