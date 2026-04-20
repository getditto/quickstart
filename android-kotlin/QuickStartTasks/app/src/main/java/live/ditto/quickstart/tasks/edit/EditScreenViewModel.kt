package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.data.Task

class EditScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "EditScreenViewModel"
    }

    private var _id: String? = null

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private val _canDelete = MutableStateFlow(false)
    val canDelete: StateFlow<Boolean> = _canDelete.asStateFlow()

    fun setTitle(value: String) {
        _title.value = value
    }

    fun setDone(value: Boolean) {
        _done.value = value
    }

    fun setupWithTask(id: String?) {
        check(live.ditto.quickstart.tasks.DittoHandler.isInitialized) {
            "Ditto must be initialized before ViewModels are created"
        }

        _canDelete.value = id != null
        val taskId: String = id ?: return

        viewModelScope.launch {
            try {
                val task = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId)
                ) { result ->
                    result.items.firstOrNull()?.let { Task.fromJson(it.jsonString()) }
                }

                task?.let {
                    _id = it._id
                    _title.value = it.title
                    _done.value = it.done
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to setup view task data", e)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                val titleValue = _title.value
                val doneValue = _done.value
                if (_id == null) {
                    // Add tasks into the ditto collection using DQL INSERT statement
                    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                    ditto.store.execute(
                        "INSERT INTO tasks DOCUMENTS (:doc)",
                        mapOf(
                            "doc" to mapOf(
                                "title" to titleValue,
                                "done" to doneValue,
                                "deleted" to false
                            )
                        )
                    )
                } else {
                    // Update tasks in the ditto collection using DQL UPDATE statement
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
                                "title" to titleValue,
                                "done" to doneValue,
                                "id" to id
                            )
                        )
                    }
                }
            } catch (e: Throwable) {
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
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
