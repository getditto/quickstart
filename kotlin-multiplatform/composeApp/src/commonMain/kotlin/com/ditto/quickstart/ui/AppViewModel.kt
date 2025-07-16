package com.ditto.quickstart.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.ditto.quickstart.data.repository.TaskRepository
import com.ditto.quickstart.usecases.CreateDittoUseCase
import com.ditto.quickstart.usecases.DestroyDittoUseCase
import com.ditto.quickstart.usecases.GetPersistedSyncStatusUseCase
import com.ditto.quickstart.usecases.SetSyncStatusUseCase

class AppViewModel(
    private val taskRepository: TaskRepository,
    private val createDittoUseCase: CreateDittoUseCase,
    private val destroyDittoUseCase: DestroyDittoUseCase,
    private val getPersistedSyncStatusUseCase: GetPersistedSyncStatusUseCase,
    private val setSyncStatusUseCase: SetSyncStatusUseCase,
) : ViewModel() {
    fun onStartApp() {
        viewModelScope.launch {
            createDittoUseCase.invoke()
            val isPersistedSyncEnabled = getPersistedSyncStatusUseCase.invoke()

            if (isPersistedSyncEnabled) {
                setSyncStatusUseCase.invoke(true)
            }
        }
    }

    override fun onCleared() {
        taskRepository.onCleared()
        destroyDittoUseCase.invoke()

        super.onCleared()
    }
}
