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
import live.ditto.DittoSyncSubscription
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

// The value of the Sync switch is stored in persistent settings
private val Context.preferencesDataStore by preferencesDataStore("tasks_list_settings")
private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")

class TasksListScreenViewModel : ViewModel() {

    companion object {
        private const val TAG = "TasksListScreenViewModel"

        private const val QUERY = "SELECT * FROM tasks WHERE deleted != true"
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
                    ditto.startSync()
                    syncSubscription = ditto.sync.registerSubscription(QUERY)
                } catch (e: DittoError) {
                    Log.e(TAG, e.message.toString())
                }
            } else if (ditto.isSyncActive) {
                try {
                    syncSubscription?.close()
                    syncSubscription = null
                    ditto.stopSync()
                } catch (e: DittoError) {
                    Log.e(TAG, e.message.toString())
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            ditto.store.registerObserver(QUERY) { result ->
                val list = result.items.map { item -> Task.fromJson(item.jsonString()) }
                tasks.postValue(list)
            }

            setSyncEnabled(
                preferencesDataStore.data.map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }.first()
            )
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch {
            try {
                val doc = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id",
                    mapOf("_id" to taskId)
                ).items.first()

                val done = doc.value["done"] as Boolean

                ditto.store.execute(
                    "UPDATE tasks SET done = :toggled WHERE _id = :_id",
                    mapOf(
                        "toggled" to !done,
                        "_id" to taskId
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            try {
                ditto.store.execute(
                    "UPDATE tasks SET deleted = true WHERE _id = :id",
                    mapOf("id" to taskId)
                )
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
        }
    }
}
