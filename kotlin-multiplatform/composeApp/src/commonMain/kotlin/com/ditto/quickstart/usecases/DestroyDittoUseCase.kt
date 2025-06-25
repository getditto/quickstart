package com.ditto.quickstart.usecases

import com.ditto.quickstart.ditto.DittoManager

class DestroyDittoUseCase(
    private val dittoManager: DittoManager
) {
    /**
     * Destroys Ditto.
     */
    operator fun invoke() = dittoManager.destroyDitto()
}
