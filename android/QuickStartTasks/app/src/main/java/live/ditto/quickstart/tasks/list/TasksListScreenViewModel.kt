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
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.TasksApplication
import live.ditto.quickstart.tasks.data.Task

// The value of the Sync switch is stored in persistent settings
private val Context.dataStore by preferencesDataStore("tasks_list_settings")
private val SYNC_ENABLED_KEY = booleanPreferencesKey("sync_enabled")

class TasksListScreenViewModel : ViewModel() {
    private val dataStore = TasksApplication.applicationContext().dataStore

    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    private val _syncEnabled = MutableLiveData(true)
    val syncEnabled: LiveData<Boolean> = _syncEnabled

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { settings ->
                settings[SYNC_ENABLED_KEY] = enabled
            }
            _syncEnabled.value = enabled

            // TODO: start/stop sync
        }
    }

    init {
        viewModelScope.launch {
            _syncEnabled.value =
                dataStore.data.map { prefs -> prefs[SYNC_ENABLED_KEY] ?: true }.first()

            val query = "SELECT * FROM tasks WHERE deleted != true"
            ditto.sync.registerSubscription(query)
            ditto.store.registerObserver(query) { result ->
                val list = result.items.map { item -> Task.fromJson(item.jsonString()) }
                tasks.postValue(list)
            }
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
                Log.e("ERROR:", e.message.toString())
            }
        }
    }
}
