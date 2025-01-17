package live.ditto.quickstart.tasks.list

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.TasksLib
import live.ditto.quickstart.tasks.TasksObserver
import live.ditto.quickstart.tasks.data.Task

class TasksListScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "TasksListScreenViewModel"

        private const val QUERY = "SELECT * FROM tasks WHERE NOT deleted ORDER BY _id"
    }

    inner class UpdateHandler : TasksObserver {
        override fun onTasksUpdated(tasksJson: Array<String>) {
            val newList = tasksJson.map { Task.fromJson(it) }
            tasks.postValue(newList)
        }
    }

    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    private val updateHandler: UpdateHandler = UpdateHandler()

    init {
        viewModelScope.launch {
            TasksLib.insertInitialDocuments()
            TasksLib.setTasksObserver(updateHandler)
        }
    }

    override fun onCleared() {
        TasksLib.removeTasksObserver()
        super.onCleared()
    }

    fun toggle(taskId: String) {
        viewModelScope.launch {
            try {
                TasksLib.toggleDoneState(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to toggle done state", e)
            }
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            try {
                TasksLib.deleteTask(taskId)
            } catch (e: Exception) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
