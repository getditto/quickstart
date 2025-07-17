package live.ditto.quickstart.tasks.list

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import live.ditto.DittoError
import live.ditto.DittoSyncSubscription
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.DataManager
import live.ditto.quickstart.tasks.data.TaskModel

class TasksListScreenViewModel(
    val dataManager: DataManager,
    context: Context
) : ViewModel() {

    // The value of the Sync switch is stored in persistent settings
    private val Context.preferencesDataStore by preferencesDataStore("tasks_list_settings")
    private val _syncEnabledKey = booleanPreferencesKey("sync_enabled")
    private var preferencesDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> =
        context.preferencesDataStore
    val syncEnabled = mutableStateOf(true)

    //track the list of task models using a flow
    private val _taskModels = MutableStateFlow<List<TaskModel>>(emptyList())
    val taskModels: StateFlow<List<TaskModel>> = _taskModels.asStateFlow()

    init {
        loadSyncEnabled()
        viewModelScope.launch {
            dataManager.populateTaskCollection()
            dataManager.getTaskModels()
                .collect { taskModelList ->
                    _taskModels.value = taskModelList
                }
        }
    }

    private fun loadSyncEnabled() {
        viewModelScope.launch {
            preferencesDataStore.data.collect { preferences ->
                syncEnabled.value = preferences[_syncEnabledKey] ?: true
                setSyncEnabled(syncEnabled.value)
            }
        }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            //update state and the preferences data store
            if (syncEnabled.value != enabled) {
                preferencesDataStore.edit { preferences ->
                    preferences[_syncEnabledKey] = enabled
                }
                syncEnabled.value = enabled
            }
            dataManager.setSyncEnabled(enabled)
        }
    }

    fun toggle(id: String) {
        viewModelScope.launch {
            dataManager.toggleComplete(id)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            dataManager.deleteTaskModel(id)
        }
    }
}
