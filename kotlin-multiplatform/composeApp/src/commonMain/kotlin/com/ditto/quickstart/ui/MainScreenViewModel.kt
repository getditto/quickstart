package com.ditto.quickstart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ditto.example.kotlin.quickstart.configuration.DittoSecretsConfiguration
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ditto.quickstart.data.screenstate.MainScreenState
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.data.repository.TaskRepository
import com.ditto.quickstart.usecases.GetPersistedSyncStatusUseCase
import com.ditto.quickstart.usecases.IsDittoCreatedUseCase
import com.ditto.quickstart.usecases.SetSyncStatusUseCase

class MainScreenViewModel(
    private val taskRepository: TaskRepository,
    private val isDittoCreatedUseCase: IsDittoCreatedUseCase,
    private val getPersistedSyncStatusUseCase: GetPersistedSyncStatusUseCase,
    private val setSyncStatusUseCase: SetSyncStatusUseCase
) : ViewModel() {
    private val _state = MutableStateFlow(MainScreenState.initial())
    val state: StateFlow<MainScreenState> = _state
        .asStateFlow()
        .onStart { loadInitialState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = _state.value
        )

    fun onSyncChange(enabled: Boolean) {
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            val operationSucceed = setSyncStatusUseCase.invoke(enabled)
            val errorMessage = if (operationSucceed) null else "Failed to set sync status"
            val isSyncEnabled = if (operationSucceed) enabled else _state.value.isSyncEnabled

            _state.value = _state.value.copy(
                isLoading = false,
                isSyncEnabled = isSyncEnabled,
                errorMessage = errorMessage
            )
        }
    }

    fun removeTask(task: Task) {
        viewModelScope.launch {
            taskRepository.removeTask(task.id)
        }
    }

    /**
     * Loads the initial screen state.
     */
    private suspend fun loadInitialState() {
        _state.value = _state.value.copy(isLoading = true)

        val errorMessage = viewModelScope.async {
            if (isDittoCreatedUseCase.invoke()) {
                null
            } else {
                "Ditto is not created, check logs for more details"
            }
        }

        val isSyncEnabled = viewModelScope.async {
            getPersistedSyncStatusUseCase.invoke()
        }

        _state.value = _state.value.copy(
            isLoading = false,
            isSyncEnabled = isSyncEnabled.await(),
            appId = DittoSecretsConfiguration.DITTO_APP_ID,
            appToken = DittoSecretsConfiguration.DITTO_PLAYGROUND_TOKEN,
            errorMessage = errorMessage.await(),
        )
    }
}
