package live.ditto.quickstart.tasks.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoManager
import live.ditto.quickstart.tasks.data.TasksRepository

class EditScreenViewModel : ViewModel() {

    init {
        check(DittoManager.isInitialized) {
            "Ditto must be initialized before ViewModels are created"
        }
    }

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
        _canDelete.value = id != null
        val taskId: String = id ?: return

        viewModelScope.launch {
            try {
                TasksRepository.getTask(taskId)?.let {
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
                val id = _id
                if (id == null) {
                    TasksRepository.insertTask(titleValue, doneValue)
                } else {
                    TasksRepository.updateTask(id, titleValue, doneValue)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to save task", e)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                _id?.let { id -> TasksRepository.delete(id) }
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
