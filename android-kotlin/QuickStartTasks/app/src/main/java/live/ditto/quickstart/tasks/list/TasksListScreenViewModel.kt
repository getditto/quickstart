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
import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

import com.ditto.kotlin.serialization.DittoCborSerializable
import com.ditto.kotlin.serialization.DittoCborSerializable.Utf8String
import okio.Utf8

// The value of the Sync switch is stored in persistent settings
private val Context.preferencesDataStore by preferencesDataStore("tasks_list_settings")
private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")

class TasksListScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "TasksListScreenViewModel"

        private const val QUERY = "SELECT * FROM tasks WHERE NOT deleted"
    }

    private val preferencesDataStore = TasksApplication.applicationContext().preferencesDataStore

    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    private val _syncEnabled = MutableLiveData(true)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    private var syncSubscription: DittoSyncSubscription? = null

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
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable to start sync", e)
                }
            } else if (ditto.isSyncActive) {
                try {
                    syncSubscription?.close()
                    syncSubscription = null
                    ditto.stopSync()
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable to stop sync", e)
                }
            }
        }
    }

    init {
        // Defensive check - should never fail with synchronous initialization
        check(live.ditto.quickstart.tasks.DittoHandler.isInitialized) {
            "Ditto must be initialized before ViewModels are created"
        }

        viewModelScope.launch {
            populateTasksCollection()

            // Register observer, which runs against the local database on this peer
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
            ditto.store.registerObserver(QUERY) { result ->
                val list = result.items.map {
                    item -> Task.fromJson(item.jsonString())
                }
                tasks.postValue(list)
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
                    val addMap = DittoCborSerializable.Dictionary(
                        mapOf(
                            Utf8String("_id") to Utf8String(task._id),
                            Utf8String("title") to Utf8String(task.title),
                            Utf8String("done") to DittoCborSerializable.BooleanValue(task.done),
                            Utf8String("deleted") to DittoCborSerializable.BooleanValue(task.deleted)

                        )
                    )
                    ditto.store.execute(
                        "INSERT INTO tasks INITIAL DOCUMENTS (:task)",
                        DittoCborSerializable.Dictionary(
                            mapOf(
                                Utf8String("task") to addMap
                            )
                        )
                    )
                } catch (e: Throwable) {
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
                    DittoCborSerializable.Dictionary(
                        mapOf(
                            Utf8String("_id") to Utf8String(taskId)
                        )
                    )
                ).items.first()

                val done = doc.value["done"].boolean

                // Update tasks into the ditto collection using DQL UPDATE statement
                // https://docs.ditto.live/sdk/latest/crud/update#updating
                ditto.store.execute(
                    "UPDATE tasks SET done = :toggled WHERE _id = :_id AND NOT deleted",
                    DittoCborSerializable.Dictionary(
                        mapOf(
                            Utf8String("toggled") to DittoCborSerializable.BooleanValue(!done),
                            Utf8String("_id") to Utf8String(taskId)
                        )
                    )
                )
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
                    DittoCborSerializable.Dictionary(
                        mapOf(
                            Utf8String("id") to Utf8String(taskId)
                        )
                    )
                )
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
