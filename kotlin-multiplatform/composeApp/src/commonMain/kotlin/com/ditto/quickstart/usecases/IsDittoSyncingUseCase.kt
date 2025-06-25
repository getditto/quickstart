package com.ditto.quickstart.usecases

import com.ditto.quickstart.ditto.DittoManager

class IsDittoSyncingUseCase(
    private val dittoManager: DittoManager
) {
    /**
     * Returns true if Ditto is syncing, false otherwise.
     */
    suspend operator fun invoke() = dittoManager.isSyncing()
}
