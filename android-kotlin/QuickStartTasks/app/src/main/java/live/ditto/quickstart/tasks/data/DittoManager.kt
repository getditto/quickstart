package live.ditto.quickstart.tasks.data

import android.util.Log
import androidx.activity.result.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.ditto.Ditto
import live.ditto.DittoError
import live.ditto.DittoIdentity
import live.ditto.DittoLogLevel
import live.ditto.DittoLogger
import live.ditto.DittoStoreObserver
import live.ditto.DittoSyncSubscription
import live.ditto.android.AndroidDittoDependencies
import live.ditto.quickstart.tasks.models.DittoConfig
import live.ditto.quickstart.tasks.services.ErrorService

class DittoManager(
    override val dittoConfig: DittoConfig,
    androidDittoDependencies: AndroidDittoDependencies,
    private val errorService: ErrorService
) : DataManager {
    companion object {
        private const val TAG = "DittoManager"
    }

    // Use Dispatchers.IO for the scope if most operations will be I/O bound
    //https://developer.android.com/kotlin/coroutines/coroutines-adv#main-safety
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var ditto: Ditto? = null
    private var subscription: DittoSyncSubscription? = null
    private var storeObserver: DittoStoreObserver? = null

    init {
        try {
            DittoLogger.minimumLogLevel = DittoLogLevel.DEBUG

            val identity = DittoIdentity.OnlinePlayground(
                androidDittoDependencies,
                dittoConfig.appId,
                dittoConfig.authToken,
                false,
                dittoConfig.authUrl
            )
            this.ditto = Ditto(androidDittoDependencies, identity)

            ditto?.let { ditto ->
                scope.launch {
                    try {
                        // Set the Ditto Websocket URL
                        ditto.updateTransportConfig { config ->
                            config.connect.websocketUrls.add(dittoConfig.websocketUrl)
                        }

                        // disable sync with v3 peers, required for DQL to work
                        ditto.disableSyncWithV3()

                        // Disable DQL strict mode
                        // when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
                        // https://docs.ditto.live/dql/strict-mode
                        ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")
                    } catch (e: Exception) {
                        errorService.showError("Error during initial database operation: ${e.message}")
                        Log.e(TAG, "Error during initial database operation", e)
                    }
                }
            }
        } catch (e: DittoError) {
            errorService.showError("Failed to initialize Ditto: ${e.message}")
        }
    }

    /**
     * Populates the Ditto tasks collection with initial seed data if it's empty.
     *
     * This method:
     * - Creates a set of predefined tasks with unique IDs and titles
     * - Inserts each task into the Ditto store using DQL (Ditto Query Language)
     * - Uses the INITIAL keyword to only insert if documents don't already exist
     *
     * The initial tasks include common todo items like:
     * - Buy groceries
     * - Clean the kitchen
     * - Schedule dentist appointment
     * - Pay bills
     *
     * Implementation details:
     * - Uses DQL INSERT statement with INITIAL keyword to prevent duplicates
     * - Each task document contains _id, title, done, and deleted fields
     * - Handles errors gracefully and logs them for debugging
     *
     * @throws Exception if the insert operations fail
     * Ditto Docs:
     * https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
     */
    override suspend fun populateTaskCollection() {
        return withContext(Dispatchers.IO) {
            val tasks = listOf(
                TaskModel("50191411-4C46-4940-8B72-5F8017A04FA7", "Buy groceries"),
                TaskModel("6DA283DA-8CFE-4526-A6FA-D385089364E5", "Clean the kitchen"),
                TaskModel("5303DDF8-0E72-4FEB-9E82-4B007E5797F0", "Schedule dentist appointment"),
                TaskModel("38411F1B-6B49-4346-90C3-0B16CE97E174", "Pay bills")
            )

            tasks.forEach { task ->
                try {
                    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                    ditto?.store?.execute(
                        "INSERT INTO tasks INITIAL DOCUMENTS (:task)",
                        mapOf(
                            "task" to mapOf(
                                "_id" to task._id,
                                "title" to task.title,
                                "done" to task.done,
                                "deleted" to task.deleted,
                            )
                        )
                    )
                } catch (e: Exception) {
                    errorService.showError("Unable to insert initial documents: ${e.message}")
                    Log.e(TAG, "Unable to insert initial document", e)
                }
            }
        }
    }

    /**
     * Creates a Flow that observes and emits changes to the tasks collection in Ditto.
     * https://docs.ditto.live/sdk/latest/crud/read#using-args-to-query-dynamic-values
     *
     * This method:
     * - Sets up a live query observer for the tasks collection
     * - Emits a new list of TaskModel objects whenever the data changes
     * - Automatically cleans up the observer when the flow is cancelled
     *
     * The query:
     * - Returns all non-archived TaskModel (done = false)
     * - Transforms raw Ditto data into TaskModel objects
     *
     * @return Flow<List<TaskModel>> A flow that emits updated lists of Tasks
     * whenever changes occur in the collection
     *
     */
    override fun getTaskModels(): Flow<List<TaskModel>> = callbackFlow {
        try {
            val query = "SELECT * FROM tasks WHERE NOT deleted"

            storeObserver = ditto?.store?.registerObserver(query) { results ->
                val items = results.items.map { item ->
                    TaskModel.fromMap(item.value)
                }
                trySend(items)
            }
            awaitClose {
                storeObserver?.close()
                storeObserver = null
            }
        } catch (e: Exception) {
            errorService.showError("Failed to setup observer for getting taskModels: ${e.message}")
            Log.e(TAG, "Failed to setup observer for getting taskModels", e)
        }
    }

    /**
     * Enables or disables synchronization with the Ditto cloud (BigPeer).
     *
     * This method:
     * - Manages the sync state of the Ditto instance
     * - Persists the sync preference in DataStore
     * - Updates the UI state through LiveData
     *
     * Implementation details:
     * - Retrieves default sync state from preferences if not specified
     * - Starts sync if enabled and not already active
     * - Stops sync if disabled and currently active
     * - Updates both DataStore and LiveData to maintain state
     *
     * @param enabled Boolean value to set sync state to. If null, uses stored preference
     *
     * @see startSync()
     * @see stopSync()
     */
    override suspend fun setSyncEnabled(enabled: Boolean) {
        //get default value from preferences if not set
        if (enabled  && !(ditto?.isSyncActive)!!) {
            startSync()
        } else {
            stopSync()
        }
    }

    /**
     * Starts the Ditto sync process and sets up the initial subscription.
     *
     * This method:
     * - Initiates the sync process
     * - Creates a subscription to the tasks collection
     * - Ensures real-time synchronization of task data across devices
     *
     * The subscription:
     * - Queries all documents in the tasks collection
     * - Maintains the subscription until sync is stopped or the app is terminated
     * - Automatically syncs changes between the local store and the Ditto cloud
     *
     * Implementation details:
     * - Uses Ditto's sync API to establish connection with the BigPeer
     * - Sets up a subscription using DQL to monitor the tasks collection
     * - Handles errors gracefully and logs them for debugging
     *
     * @throws Exception if sync initialization or subscription setup fails
     *
     * Ditto Docs:
     * https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
     * https://docs.ditto.live/sdk/latest/sync/syncing-data
     *
     */
    private suspend fun startSync() {
        return withContext(Dispatchers.IO) {
            try {
                ditto?.let {
                    it.startSync()

                    val subscriptionQuery = "SELECT * from tasks"
                    subscription = it.sync.registerSubscription(subscriptionQuery)
                }
            } catch (e: Exception) {
                errorService.showError("Failed to start ditto sync: ${e.message}")
                Log.e(TAG, "Failed to start ditto sync", e)
            }
        }
    }

    /**
     * Stops the Ditto sync process and cleans up associated resources.
     *
     * This method:
     * - Cancels any active subscriptions to the tasks collection
     * - Stops the sync process with the Ditto cloud (BigPeer)
     * - Cleans up resources to prevent memory leaks
     *
     * Implementation details:
     * - Closes the subscription and sets it to null
     * - Calls Ditto's stopSync() to terminate the sync process
     * - Should be called when sync is no longer needed or app is terminating
     *
     * @see Ditto.stopSync()
     * @see DittoSyncSubscription.close()
     */
    suspend fun stopSync() {
        return withContext(Dispatchers.IO) {
            //implements stopping sync by calling cancel and setting object to nil
            //and then running stopSync function
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#canceling-subscriptions
            subscription?.close()
            subscription = null
            ditto?.stopSync()
        }
    }



    /**
     * Creates a new TaskModel document in the Ditto store.
     * https://docs.ditto.live/sdk/latest/crud/create#creating-documents
     *
     * Implementation details:
     * - Inserts a complete document with all required fields using DQL
     *
     * @param taskModel The new TaskModel object to insert
     */
    override suspend fun insertTaskModel(taskModel: TaskModel) {
        return withContext(Dispatchers.IO) {
            try {
                val query = "INSERT INTO tasks DOCUMENTS (:newTask)"
                ditto?.store?.execute(
                    query,
                    mapOf(
                        "newTask" to mapOf(
                            "_id" to taskModel._id,
                            "title" to taskModel.title,
                            "done" to taskModel.done,
                            "deleted" to taskModel.deleted
                        )
                    )
                )
            } catch (e: Exception) {
                errorService.showError("Failed to add taskModel: ${e.message}")
                Log.e(TAG, "Failed to add taskModel:", e)
            }
        }
    }

    /**
     * Updates existing TaskModel's properties in the Ditto store.
     * https://docs.ditto.live/sdk/latest/crud/update#updating
     * Implementation details:
     * - Uses DQL to update the document
     *
     * @param taskModel The TaskModel object containing updated values
     */
    override suspend fun updateTaskModel(taskModel: TaskModel) {
        return withContext(Dispatchers.IO) {
            try {
                // https://docs.ditto.live/dql/update#basic-update
                // https://docs.ditto.live/sdk/latest/crud/update#updating
                val query = """
                UPDATE tasks
                SET title = :title,
                    done = :done,
                    deleted = :deleted
                WHERE _id = :_id 
                """

                // https://docs.ditto.live/sdk/latest/crud/update#updating
                ditto?.store?.execute(
                    query,
                    mapOf(
                        "title" to taskModel.title,
                        "done" to taskModel.done,
                        "deleted" to taskModel.deleted,
                        "_id" to taskModel._id
                    )
                )
            } catch (e: Exception) {
                errorService.showError("Failed to update taskModel: ${e.message}")
                Log.e(TAG, "Failed to update taskModel:", e)
            }
        }
    }

    /**
     * Updates an existing TaskModel in the Ditto store, setting the done
     * field to true.
     *
     * This method:
     * - Updates the done field to true
     * - Maintains the taskModel's ID and references
     * - Triggers a sync with other devices
     *
     * The taskModel object should have:
     * - A unique ID
     * - All required fields populated
     *
     * @param id The _id TaskModel object to update
     * @throws Exception if the insert operation fails
     */
    override suspend fun toggleComplete(id: String) {
        return withContext(Dispatchers.IO) {
            try {
                ditto?.let {
                    val doc = it.store.execute(
                        "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                        mapOf("_id" to id)
                    ).items.first()

                    val done = doc.value["done"] as Boolean

                    // https://docs.ditto.live/dql/update#basic-update
                    // https://docs.ditto.live/sdk/latest/crud/update#updating
                    val query = """
                        UPDATE tasks
                        SET done = :done 
                        WHERE _id == :_id
                    """

                    // https://docs.ditto.live/sdk/latest/crud/update#updating
                    it.store.execute(
                        query,
                        mapOf(
                            "done" to !done,
                            "_id" to id
                        )
                    )
                }
            } catch (e: Exception) {
                errorService.showError("Failed to update taskModel: ${e.message}")
                Log.e(TAG, "Failed to update taskModel:", e)
            }
        }
    }

    /**
     * Marks a TaskModel as archived in the Ditto store.
     * This uses the 'Soft-Delete' pattern:  https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
     *
     * Implementation details:
     * - Uses a simple DQL UPDATE query to set done flag
     *
     * @param id The _id of the TaskModel to archive
     */
    override suspend fun deleteTaskModel(id: String) {
        return withContext(Dispatchers.IO) {
            try {
                // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
                val query = """
                UPDATE tasks
                SET deleted = true
                WHERE _id = :_id 
                """
                ditto?.store?.execute(
                    query,
                    mapOf(
                        "_id" to id,
                    )
                )
            } catch (e: Exception) {
                errorService.showError("Failed to archive taskModel: ${e.message}")
                Log.e(TAG, "Failed to archive taskModel:", e)
            }
        }
    }
}