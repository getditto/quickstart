package com.ditto.quickstart.usecases

import com.ditto.quickstart.ditto.DittoManager

class IsDittoCreatedUseCase(
    private val dittoManager: DittoManager
) {
    /**
     * Returns true if Ditto is created, false otherwise.
     */
    suspend operator fun invoke() = dittoManager.isDittoCreated()
}
