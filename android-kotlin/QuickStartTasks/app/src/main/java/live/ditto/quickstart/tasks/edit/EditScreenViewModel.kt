package live.ditto.quickstart.tasks.edit

import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoServiceConnection
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

class EditScreenViewModel(
    private val dittoServiceConnection: DittoServiceConnection = TasksApplication.getInstance().dittoServiceConnection
) : ViewModel() {

    private var _id: String? = null

    var title = MutableLiveData("")

    var done = MutableLiveData(false)
    var canDelete = MutableLiveData(false)
    fun setupWithTask(id: String?) {
        canDelete.postValue(id != null)
        val taskId: String = id ?: return

        viewModelScope.launch {
            try {
                val result = dittoServiceConnection.execute(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId)
                )

                result?.resultJson?.firstOrNull()?.let { jsonString ->
                    val task = Task.fromJson(jsonString)
                    _id = task._id
                    title.postValue(task.title)
                    done.postValue(task.done)
                }
            } catch (e: RemoteException) {
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
                    dittoServiceConnection.execute(
                        "INSERT INTO tasks DOCUMENTS (:doc)",
                        mapOf(
                            "doc" to mapOf(
                                "title" to (title.value ?: ""),
                                "done" to (done.value ?: false),
                                "deleted" to false
                            )
                        )
                    )
                } else {
                    // Update tasks into the ditto collection using DQL UPDATE statement
                    // https://docs.ditto.live/sdk/latest/crud/update#updating
                    _id?.let { id ->
                        dittoServiceConnection.execute(
                            """
                            UPDATE tasks
                            SET
                              title = :title,
                              done = :done
                            WHERE _id = :id
                            AND NOT deleted
                            """,
                            mapOf(
                                "title" to (title.value ?: ""),
                                "done" to (done.value ?: false),
                                "id" to id
                            )
                        )
                    }
                }
            } catch (e: RemoteException) {
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
                    dittoServiceConnection.execute(
                        "UPDATE tasks SET deleted = true WHERE _id = :id",
                        mapOf("id" to id)
                    )
                }
            } catch (e: RemoteException) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }

    companion object {
        private const val TAG = "EditScreenViewModel"
    }
}
