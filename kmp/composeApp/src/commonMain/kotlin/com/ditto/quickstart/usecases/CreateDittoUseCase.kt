package com.ditto.quickstart.usecases

import com.ditto.quickstart.ditto.DittoManager

class CreateDittoUseCase(
    private val dittoManager: DittoManager
) {
    /**
     * Creates Ditto.
     */
    suspend operator fun invoke() = dittoManager.createDitto()
}
