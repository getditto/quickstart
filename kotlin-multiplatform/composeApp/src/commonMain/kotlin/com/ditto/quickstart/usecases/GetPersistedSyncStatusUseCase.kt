package com.ditto.quickstart.usecases

import com.ditto.quickstart.data.repository.PreferenceRepository

class GetPersistedSyncStatusUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    /**
     * Returns the persisted sync status.
     */
    suspend operator fun invoke() = preferenceRepository.getSync()
}
