package live.ditto.quickstart.tasks.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoManager
import live.ditto.quickstart.tasks.data.Task
import live.ditto.quickstart.tasks.data.TasksRepository

class TasksListScreenViewModel : ViewModel() {

    // Verify Ditto readiness before any property initializer below touches it.
    init {
        check(DittoManager.isInitialized) {
            "Ditto must be initialized before ViewModels are created"
        }
    }

    companion object {
        private const val TAG = "TasksListScreenViewModel"
    }

    // The repository owns the observer; the ViewModel exposes it as UI state.
    val tasks: StateFlow<List<Task>> = TasksRepository.observeTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sync is on by default so the app syncs out of the box. This is in-memory
    // session state — the toggle is not persisted across launches.
    private val _syncEnabled = MutableStateFlow(true)
    val syncEnabled: StateFlow<Boolean> = _syncEnabled.asStateFlow()

    init {
        // Start sync on launch to match the default enabled state.
        applySyncState(_syncEnabled.value)
    }

    fun setSyncEnabled(enabled: Boolean) {
        _syncEnabled.value = enabled
        applySyncState(enabled)
    }

    private fun applySyncState(enabled: Boolean) {
        try {
            if (enabled) {
                DittoManager.startSync()
                TasksRepository.registerSubscription()
            } else {
                DittoManager.stopSync()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Unable to apply sync state (enabled=$enabled)", e)
        }
    }

    fun toggle(task: Task) {
        viewModelScope.launch {
            try {
                TasksRepository.toggle(task)
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to toggle done state", e)
            }
        }
    }

    fun delete(taskId: String) {
        viewModelScope.launch {
            try {
                TasksRepository.delete(taskId)
            } catch (e: Throwable) {
                Log.e(TAG, "Unable to set deleted=true", e)
            }
        }
    }
}
