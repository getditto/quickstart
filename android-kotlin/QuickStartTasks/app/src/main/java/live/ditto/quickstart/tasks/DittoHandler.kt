package live.ditto.quickstart.tasks

import com.ditto.kotlin.*

class DittoHandler {
    companion object {
        lateinit var ditto: Ditto
            private set

        fun initialize(config: DittoConfig) {
            if (::ditto.isInitialized) {
                throw IllegalStateException("Ditto is already initialized")
            }
            ditto = DittoFactory.create(config = config)
        }

        val isInitialized: Boolean
            get() = ::ditto.isInitialized
    }
}
