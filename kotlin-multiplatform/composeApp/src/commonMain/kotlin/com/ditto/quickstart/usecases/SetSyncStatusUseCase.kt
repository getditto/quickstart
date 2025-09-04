package com.ditto.quickstart.usecases

import com.ditto.kotlin.DittoLog
import com.ditto.kotlin.error.DittoError
import com.ditto.quickstart.data.repository.PreferenceRepository
import com.ditto.quickstart.ditto.DittoManager

private const val TAG = "SetSyncStatusUseCase"

class SetSyncStatusUseCase(
    private val dittoManager: DittoManager,
    private val preferenceRepository: PreferenceRepository,
) {
    /**
     * Enables or disables sync.
     * This value is persisted in preferences file.
     *
     * @param isSyncing true to enable sync, false to disable sync.
     * @return true the operation was successful, false otherwise.
     */
    suspend operator fun invoke(isSyncing: Boolean): Boolean {
        return if (isSyncing) enableSync() else disableSync()
    }

    private suspend fun enableSync(): Boolean {
        println("🔄 SetSyncStatusUseCase: Enabling sync...")
        
        try {
            dittoManager.startSync()
            println("✅ SetSyncStatusUseCase: dittoManager.startSync() called successfully")
        } catch (e: DittoError) {
            println("❌ SetSyncStatusUseCase: Failed to start sync: $e")
            DittoLog.e(TAG, "Failed to start sync: $e")
            return false
        }

        try {
            preferenceRepository.setSync(true)
            println("✅ SetSyncStatusUseCase: Saved sync=true to preferences")
        } catch (e: Throwable) {
            println("❌ SetSyncStatusUseCase: Failed to save preference: $e")
            DittoLog.e(TAG, "Failed to set sync(true) preference: $e")
            return false
        }

        val isSyncing = dittoManager.isSyncing()
        println("🔍 SetSyncStatusUseCase: Final sync status: $isSyncing")
        return isSyncing
    }

    private suspend fun disableSync(): Boolean {
        try {
            dittoManager.stopSync()
        } catch (e: DittoError) {
            DittoLog.e(TAG, "Failed to stop sync: $e")
            return false
        }

        try {
            preferenceRepository.setSync(false)
        } catch (e: Throwable) {
            DittoLog.e(TAG, "Failed to set sync(false) preference: $e")
            return false
        }

        return !dittoManager.isSyncing()
    }
}
