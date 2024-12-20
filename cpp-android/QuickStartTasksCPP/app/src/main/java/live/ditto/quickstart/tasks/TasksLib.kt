package live.ditto.quickstart.tasks

import live.ditto.quickstart.tasks.data.Task

// Wraps the C++ JNI code in a Kotlin object.
object TasksLib {
    init {
        // load C++ native library
        System.loadLibrary("taskscpp")
    }

    // Initialize the Ditto client.
    //
    // This must be called before any other methods of this object are called.
    external fun initDitto(appId: String, token: String)

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
}

