package live.ditto.quickstart.tasks

import android.util.Log
import live.ditto.quickstart.tasks.data.Task

interface TasksObserver {
    fun onTasksUpdated(tasksJson: Array<String>)
}

// Wraps the C++ JNI code in a Kotlin object.
//
// The associated C++ code is in cpp/taskslib.cpp.
object TasksLib {
    private const val TAG = "TasksLib"

    init {
        // load C++ native library
        Log.i(TAG, "Loading taskscpp library...")
        System.loadLibrary("taskscpp")
        Log.i(TAG, "Loaded taskscpp library")
    }

    // Initialize the Ditto client.
    //
    // This must be called before any other methods of this object are called.
    external fun initDitto(
        appContext: android.content.Context,
        appId: String,
        token: String,
        persistenceDir: String,
        isRunningOnEmulator: Boolean,
        customAuthUrl: String,
        websocketUrl: String
    )

    // Terminate the Ditto client.
    //
    // After this is called, no other methods may be called.
    external fun terminateDitto()

    // Populate the tasks collection with a set of initial to-do items.
    external fun insertInitialDocuments()

    external fun startSync()
    external fun stopSync()
    external fun isSyncActive(): Boolean

    external fun createTask(title: String, done: Boolean)
    external fun getTaskWithId(taskId: String): Task
    external fun updateTask(taskId: String, title: String, done: Boolean)
    external fun toggleDoneState(taskId: String)
    external fun deleteTask(taskId: String)

    // Set an object whose onTasksUpdated() method will be called when the tasks collection changes.
    //
    // Only one tasks observer can be set at a time. Use removeTasksObserver() to clear it.
    external fun setTasksObserver(observer: TasksObserver)

    // Remove the tasks observer.
    external fun removeTasksObserver()
}

