package live.ditto.quickstart.tasks

import live.ditto.quickstart.tasks.data.Task

// Wraps the C++ JNI code in a Kotlin object.
object TasksLib {
    init {
        // load C++ native library
        System.loadLibrary("taskscpp")
    }

    external fun initDitto(appId: String, token: String)

    external fun startSync()
    external fun stopSync()
    external fun isSyncActive(): Boolean

    external fun createTask(title: String, done: Boolean)
    external fun getTaskWithId(taskId: String): Task
    external fun updateTask(taskId: String, title: String, done: Boolean)
    external fun toggleDoneState(taskId: String)
    external fun deleteTask(taskId: String)
    
    external fun insertInitialDocument(
        taskId: String,
        title: String,
        done: Boolean,
        deleted: Boolean
    )
}

