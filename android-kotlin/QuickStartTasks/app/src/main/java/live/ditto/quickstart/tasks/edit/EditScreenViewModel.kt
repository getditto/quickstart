package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.ditto.kotlin.error.DittoException
import com.ditto.kotlin.serialization.toDittoCbor
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.data.Task

import com.ditto.kotlin.serialization.DittoCborSerializable
import com.ditto.kotlin.serialization.DittoCborSerializable.Utf8String

class EditScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "EditScreenViewModel"
    }

    private var _id: String? = null

    var title = MutableLiveData<String>("")
    var done = MutableLiveData<Boolean>(false)
    var canDelete = MutableLiveData<Boolean>(false)

    fun setupWithTask(id: String?) {
        check(live.ditto.quickstart.tasks.DittoHandler.isInitialized) {
            "Ditto must be initialized before ViewModels are created"
        }

        canDelete.postValue(id != null)
        val taskId: String = id ?: return

        viewModelScope.launch {
            try {
                val item = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId).toDittoCbor()
                ).items.first()

                val task = Task.fromJson(item.jsonString())
                _id = task._id
                title.postValue(task.title)
                done.postValue(task.done)
            } catch (e: DittoException) {
                Log.e(TAG, "Unable to setup view task data", e)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            try {
                val titleValue = title.value ?: ""
                val doneValue = done.value ?: false
                if (_id == null) {
                    // Add tasks into the ditto collection using DQL INSERT statement
                    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                    val addMap = DittoCborSerializable.Dictionary(
                        mapOf(
                            Utf8String("title") to Utf8String(titleValue),
                            Utf8String("done") to DittoCborSerializable.BooleanValue(doneValue),
                            Utf8String("deleted") to DittoCborSerializable.BooleanValue(false)
                        )
                    )
                    ditto.store.execute(
                        "INSERT INTO tasks DOCUMENTS (:doc)",
                        DittoCborSerializable.Dictionary(
                                mapOf(Utf8String("doc") to addMap)
                        )
                    )
                } else {
                    // Update tasks into the ditto collection using DQL UPDATE statement
                    // https://docs.ditto.live/sdk/latest/crud/update#updating
                    _id?.let { id ->
                        val editMap = DittoCborSerializable.Dictionary(
                            mapOf(
                                Utf8String("title") to Utf8String(titleValue),
                                Utf8String("done") to DittoCborSerializable.BooleanValue(doneValue),
                                Utf8String("id") to DittoCborSerializable.Utf8String(id)
                            )
                        )
                        ditto.store.execute(
                            """
                            UPDATE tasks
                            SET
                              title = :title,
                              done = :done
                            WHERE _id = :id
                            AND NOT deleted
                            """,
                           arguments = editMap
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
                        DittoCborSerializable.Dictionary(
                            mapOf(
                                Utf8String("id") to Utf8String(id)
                            )
                        )
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
