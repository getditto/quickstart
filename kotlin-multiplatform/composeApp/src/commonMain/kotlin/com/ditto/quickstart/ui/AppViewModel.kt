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
            println("🚀 AppViewModel: Starting app...")
            
            createDittoUseCase.invoke()
            println("✅ AppViewModel: Ditto created successfully")
            
            // Always start sync by default for better testing and user experience
            println("🔄 AppViewModel: Starting sync by default...")
            val syncStartResult = setSyncStatusUseCase.invoke(true)
            println("🔄 AppViewModel: Sync start result: $syncStartResult")
            
            // Still respect persisted preferences if they exist
            val isPersistedSyncEnabled = getPersistedSyncStatusUseCase.invoke()
            println("💾 AppViewModel: Persisted sync enabled: $isPersistedSyncEnabled")
            
            if (!isPersistedSyncEnabled) {
                // If user had previously disabled sync, respect that
                println("⚠️ AppViewModel: User had disabled sync, stopping...")
                setSyncStatusUseCase.invoke(false)
            } else {
                println("✅ AppViewModel: Sync should be active")
            }
        }
    }

    override fun onCleared() {
        taskRepository.onCleared()
        destroyDittoUseCase.invoke()

        super.onCleared()
    }
}
