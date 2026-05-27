package live.ditto.quickstart.tasks.list

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoHandler
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

// The value of the Sync switch is stored in persistent settings
private val Context.preferencesDataStore by preferencesDataStore("tasks_list_settings")
private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")

class TasksListScreenViewModel : ViewModel() {

    // Verify Ditto readiness before any property initializer below touches it.
    init {
        check(DittoHandler.isInitialized) {
            "Ditto must be initialized before ViewModels are created"
        }
    }

    companion object {
        private const val TAG = "TasksListScreenViewModel"

        private const val QUERY = "SELECT * FROM tasks WHERE NOT deleted"
    }

    private val preferencesDataStore = TasksApplication.applicationContext().preferencesDataStore

    // Use StateFlow with store.observe() for reactive updates
    // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
    val tasks: StateFlow<List<Task>> = ditto.store.observe(QUERY) { result ->
        result.items.map { item -> Task.fromJson(item.jsonString()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Derive sync state directly from DataStore so the UI reflects the persisted value
    // as soon as it loads, instead of flashing a hardcoded default first.
    val syncEnabled: StateFlow<Boolean> = preferencesDataStore.data
        .map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    private var syncSubscription: DittoSyncSubscription? = null

    init {
        viewModelScope.launch { populateTasksCollection() }

        // Apply the persisted sync preference whenever it changes.
        viewModelScope.launch {
            preferencesDataStore.data
                .map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }
                .distinctUntilChanged()
                .collect { enabled -> applySyncState(enabled) }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.edit { settings ->
                settings[SYNC_ENABLED_KEY] = enabled
            }
        }
    }

    private fun applySyncState(enabled: Boolean) {
        if (enabled && !ditto.sync.isActive) {
            try {
                // Starting sync
                // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
                ditto.sync.start()

                // Register a subscription, which determines what data syncs to this peer
                // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
                syncSubscription = ditto.sync.registerSubscription(QUERY)
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to start sync", e)
            }
        } else if (!enabled && ditto.sync.isActive) {
            try {
                syncSubscription?.close()
                syncSubscription = null
                ditto.sync.stop()
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to stop sync", e)
            }
        }
    }

    // Add initial tasks to the collection if they have not already been added.
    private suspend fun populateTasksCollection() {
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
                    mapOf("task" to task.toMap())
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to insert initial document", e)
            }
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch {
            try {
                val task = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id AND NOT deleted",
                    mapOf("_id" to taskId)
                ) { result ->
                    result.items.firstOrNull()?.let { Task.fromJson(it.jsonString()) }
                }

                task?.let {
                    // Update tasks in the ditto collection using DQL UPDATE statement
                    // https://docs.ditto.live/sdk/latest/crud/update#updating
                    ditto.store.execute(
                        "UPDATE tasks SET done = :toggled WHERE _id = :_id AND NOT deleted",
                        mapOf("toggled" to !it.done, "_id" to taskId)
                    )
                }
            } catch (e: Throwable) {
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
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
