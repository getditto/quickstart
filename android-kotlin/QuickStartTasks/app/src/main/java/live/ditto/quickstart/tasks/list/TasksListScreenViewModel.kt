package live.ditto.quickstart.tasks.list

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import live.ditto.DittoError
import live.ditto.DittoQueryResult
import live.ditto.DittoSyncSubscription
import live.ditto.quickstart.tasks.DittoHandler
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task
import java.util.UUID

// The value of the Sync switch is stored in persistent settings
private val Context.preferencesDataStore by preferencesDataStore("tasks_list_settings")
private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")

class TasksListScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "TasksListScreenViewModel"

        private const val QUERY = "SELECT * FROM tasks WHERE NOT deleted ORDER BY _id"
    }

    private val preferencesDataStore = TasksApplication.applicationContext().preferencesDataStore

    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    private val _syncEnabled = MutableLiveData(true)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    private var syncSubscription: DittoSyncSubscription? = null
    
    // Store the query result reference to test memory leak
    private var currentQueryResult: DittoQueryResult? = null
    
    // Memory monitoring
    private val _memoryUsage = MutableLiveData<Long>(0)
    val memoryUsage: LiveData<Long> = _memoryUsage
    
    // Data generation status
    private val _dataGenerationStatus = MutableLiveData<String>("")
    val dataGenerationStatus: LiveData<String> = _dataGenerationStatus

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.edit { settings ->
                settings[SYNC_ENABLED_KEY] = enabled
            }
            _syncEnabled.value = enabled

            if (enabled && !ditto.isSyncActive) {
                try {
                    // starting sync
                    // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
                    ditto.startSync()

                    // Register a subscription, which determines what data syncs to this peer
                    // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
                    syncSubscription = ditto.sync.registerSubscription(QUERY)
                } catch (e: DittoError) {
                    Log.e(TAG, "Unable to start sync", e)
                }
            } else if (ditto.isSyncActive) {
                try {
                    syncSubscription?.close()
                    syncSubscription = null
                    ditto.stopSync()
                } catch (e: DittoError) {
                    Log.e(TAG, "Unable to stop sync", e)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            // Wait for Ditto to be initialized
            DittoHandler.waitForInitialization()
            
            populateTasksCollection()

            // Register observer, which runs against the local database on this peer
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
            ditto.store.registerObserver(QUERY) { result ->
                // Store the query result reference for memory leak testing
                currentQueryResult = result
                
                val list = result.items.map { item -> Task.fromJson(item.jsonString()) }
                tasks.postValue(list)
                
                // Update memory usage
                updateMemoryUsage()
            }

            setSyncEnabled(
                preferencesDataStore.data.map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }.first()
            )
        }
    }

    // Add initial tasks to the collection if they have not already been added.
    private fun populateTasksCollection() {
        viewModelScope.launch {
            val tasks = listOf(
                Task("50191411-4C46-4940-8B72-5F8017A04FA7", "Buy groceries"),
                Task("6DA283DA-8CFE-4526-A6FA-D385089364E5", "Clean the kitchen"),
                Task("5303DDF8-0E72-4FEB-9E82-4B007E5797F0", "Schedule dentist appointment"),
                Task("38411F1B-6B49-4346-90C3-0B16CE97E174", "Pay bills")
            )

            tasks.forEach { task ->
                try {
                    // Add tasks into the ditto collection using DQL INSERT statement
                    // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
                    ditto.store.execute(
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
                } catch (e: DittoError) {
                    Log.e(TAG, "Unable to insert initial document", e)
                }
            }
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch {
            try {
                val doc = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId)
                ).items.first()

                val done = doc.value["done"] as Boolean

                // Update tasks into the ditto collection using DQL UPDATE statement
                // https://docs.ditto.live/sdk/latest/crud/update#updating
                ditto.store.execute(
                    "UPDATE tasks SET done = :toggled WHERE _id = :_id AND NOT deleted",
                    mapOf(
                        "toggled" to !done,
                        "_id" to taskId
                    )
                )
            } catch (e: DittoError) {
                Log.e(TAG, "Unable to toggle done state", e)
            }
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            try {
                // UPDATE DQL Statement using Soft-Delete pattern
                // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
                ditto.store.execute(
                    "UPDATE tasks SET deleted = true WHERE _id = :id",
                    mapOf("id" to taskId)
                )
            } catch (e: DittoError) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
    
    // Memory leak testing methods
    
    /**
     * Generate large amount of test data to reproduce memory leak
     */
    fun generateLargeDataset() {
        viewModelScope.launch {
            _dataGenerationStatus.value = "Generating test data..."
            
            // Generate 1000 tasks with large descriptions (approx 10KB each = 10MB total)
            val largeDescription = "A".repeat(10000) // 10KB of data per task
            val batchSize = 100
            val totalTasks = 1000
            
            for (batch in 0 until (totalTasks / batchSize)) {
                for (i in 1..batchSize) {
                    val taskNum = batch * batchSize + i
                    val taskData = mapOf(
                        "_id" to "TEST-${UUID.randomUUID()}",
                        "title" to "Test Task #$taskNum with large data",
                        "done" to false,
                        "deleted" to false,
                        "description" to largeDescription // Extra field with large data
                    )
                    
                    try {
                        // Insert individual task using the same syntax as populateTasksCollection
                        ditto.store.execute(
                            "INSERT INTO tasks DOCUMENTS (:task)",
                            mapOf("task" to taskData)
                        )
                    } catch (e: DittoError) {
                        Log.e(TAG, "Unable to insert test task #$taskNum", e)
                        _dataGenerationStatus.value = "Error generating task #$taskNum: ${e.message}"
                        return@launch
                    }
                }
                
                _dataGenerationStatus.value = "Generated ${(batch + 1) * batchSize} / $totalTasks tasks..."
            }
            
            _dataGenerationStatus.value = "Generated $totalTasks tasks with ~10MB of data"
            updateMemoryUsage()
        }
    }
    
    /**
     * Close only the DittoQueryResult (should leak memory according to bug report)
     */
    fun closeQueryResultOnly() {
        viewModelScope.launch {
            Log.d(TAG, "Memory before closing query result: ${getMemoryUsageMB()}MB")
            
            currentQueryResult?.close()
            currentQueryResult = null
            
            // Force garbage collection to see if memory is freed
            System.gc()
            System.runFinalization()
            System.gc()
            
            // Wait a bit for GC to complete
            kotlinx.coroutines.delay(1000)
            
            updateMemoryUsage()
            Log.d(TAG, "Memory after closing query result only: ${getMemoryUsageMB()}MB")
            _dataGenerationStatus.value = "Closed query result only - check memory usage"
        }
    }
    
    /**
     * Close both DittoQueryResult and individual items (should free memory properly)
     */
    fun closeQueryResultAndItems() {
        viewModelScope.launch {
            Log.d(TAG, "Memory before closing query result and items: ${getMemoryUsageMB()}MB")
            
            // Close individual items first
            currentQueryResult?.items?.forEach { item ->
                item.close()
            }
            
            // Then close the query result
            currentQueryResult?.close()
            currentQueryResult = null
            
            // Force garbage collection
            System.gc()
            System.runFinalization()
            System.gc()
            
            // Wait a bit for GC to complete
            kotlinx.coroutines.delay(1000)
            
            updateMemoryUsage()
            Log.d(TAG, "Memory after closing query result and items: ${getMemoryUsageMB()}MB")
            _dataGenerationStatus.value = "Closed query result and items - check memory usage"
        }
    }
    
    /**
     * Clear all test data
     */
    fun clearTestData() {
        viewModelScope.launch {
            try {
                // Delete all test tasks
                ditto.store.execute(
                    "DELETE FROM tasks WHERE _id LIKE 'TEST-%'"
                )
                _dataGenerationStatus.value = "Cleared all test data"
                updateMemoryUsage()
            } catch (e: DittoError) {
                Log.e(TAG, "Unable to clear test data", e)
                _dataGenerationStatus.value = "Error clearing data: ${e.message}"
            }
        }
    }
    
    /**
     * Update memory usage tracking
     */
    private fun updateMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory())
        _memoryUsage.postValue(usedMemory)
    }
    
    /**
     * Get memory usage in MB
     */
    private fun getMemoryUsageMB(): Long {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory())
        return usedMemory / (1024 * 1024) // Convert to MB
    }
}
