package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.DittoError
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.data.DataManager
import live.ditto.quickstart.tasks.data.TaskModel

class EditScreenViewModel(
    private val dataManager: DataManager)
    : ViewModel() {

    companion object {
        private const val TAG = "EditScreenViewModel"
    }

    private var _id: String? = null
    private var task:TaskModel? = null

    var title = mutableStateOf("")
    var done = mutableStateOf(false)
    var canDelete = mutableStateOf(false)

    fun setupWithTask(taskJson: String?) {
        canDelete.value = (taskJson != null)
        val json: String = taskJson ?: return
        task = TaskModel.fromJson(json)
        task?.let {
            title.value = it.title
            done.value = it.done
        }
    }

    fun save() {
        viewModelScope.launch {
            if (task == null) {
                val taskModel = TaskModel(
                    title = title.value,
                    done = done.value,
                    deleted = false
                )
                dataManager.insertTaskModel(taskModel)
            } else {
                task?.let {
                    //update the task before saving it to the database
                    it.title = title.value
                    it.deleted = false
                    it.done = done.value

                    dataManager.updateTaskModel(it)
                }
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            task?.let {
                dataManager.deleteTaskModel(it._id)
            }
        }
    }
}
