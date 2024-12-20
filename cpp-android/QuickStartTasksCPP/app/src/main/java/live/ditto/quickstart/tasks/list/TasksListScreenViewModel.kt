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
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.TasksLib
import live.ditto.quickstart.tasks.data.Task

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

    // TODO: private var syncSubscription: DittoSyncSubscription? = null

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataStore.edit { settings ->
                settings[SYNC_ENABLED_KEY] = enabled
            }
            _syncEnabled.value = enabled

            if (enabled && !TasksLib.isSyncActive()) {
                try {
                    TasksLib.startSync()
                    // TODO: syncSubscription = ditto.sync.registerSubscription(QUERY)
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to start sync", e)
                }
            } else if (TasksLib.isSyncActive()) {
                try {
                    // TODO: syncSubscription?.close()
                    // TODO: syncSubscription = null
                    TasksLib.stopSync()
                } catch (e: Exception) {
                    Log.e(TAG, "Unable to stop sync", e)
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            TasksLib.insertInitialDocuments()

            // TODO: register C++ observer
//            ditto.store.registerObserver(QUERY) { result ->
//                val list = result.items.map { item -> Task.fromJson(item.jsonString()) }
//                tasks.postValue(list)
//            }

            setSyncEnabled(
                preferencesDataStore.data.map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }.first()
            )
        }
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
