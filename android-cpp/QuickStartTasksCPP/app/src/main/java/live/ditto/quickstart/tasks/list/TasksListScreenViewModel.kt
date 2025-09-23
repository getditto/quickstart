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

        private const val QUERY = "SELECT * FROM tasks WHERE NOT deleted ORDER BY title ASC"
    }

    inner class UpdateHandler : TasksObserver {
        override fun onTasksUpdated(tasksJson: Array<String>) {
            Log.d(TAG, "🧪 TEST DEBUG: onTasksUpdated called with ${tasksJson.size} tasks")
            println("🧪 TEST DEBUG: onTasksUpdated called with ${tasksJson.size} tasks")
            val newList = tasksJson.map { Task.fromJson(it) }
            Log.d(TAG, "🧪 TEST DEBUG: Tasks mapped, posting to LiveData")
            println("🧪 TEST DEBUG: Tasks mapped, posting to LiveData")
            tasks.postValue(newList)
        }
    }

    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    private val updateHandler: UpdateHandler = UpdateHandler()

    init {
        Log.d(TAG, "🧪 TEST DEBUG: TasksListScreenViewModel init() called")
        println("🧪 TEST DEBUG: TasksListScreenViewModel init() called")
        viewModelScope.launch {
            Log.d(TAG, "🧪 TEST DEBUG: About to call TasksLib.insertInitialDocuments()")
            println("🧪 TEST DEBUG: About to call TasksLib.insertInitialDocuments()")
            TasksLib.insertInitialDocuments()
            Log.d(TAG, "🧪 TEST DEBUG: About to call TasksLib.setTasksObserver()")
            println("🧪 TEST DEBUG: About to call TasksLib.setTasksObserver()")
            TasksLib.setTasksObserver(updateHandler)
            Log.d(TAG, "🧪 TEST DEBUG: TasksObserver set successfully")
            println("🧪 TEST DEBUG: TasksObserver set successfully")
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
