package live.ditto.quickstart.tasks

import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoFactory
import com.ditto.kotlin.DittoLog

/**
 * Owns configuration and lifecycle of the Ditto instance: creating it, wiring up
 * development-mode authentication, and controlling sync. It knows nothing about
 * tasks.
 *
 * It vends the configured Ditto instance (via [ditto]) so the rest of the app
 * can use the real Ditto API directly.
 */
class DittoManager {
    companion object {
        private const val TAG = "DittoManager"

        lateinit var ditto: Ditto
            private set

        val isInitialized: Boolean
            get() = ::ditto.isInitialized

        /**
         * Create and configure the Ditto instance and set up authentication.
         * Does not start sync — call [startSync] for that.
         */
        fun initialize(config: DittoConfig, developmentToken: String, offlineLicenseToken: String) {
            if (::ditto.isInitialized) {
                throw IllegalStateException("Ditto is already initialized")
            }
            ditto = DittoFactory.create(config = config)
            if (offlineLicenseToken.isNotBlank()) {
                ditto.setOfflineOnlyLicenseToken(offlineLicenseToken.trim())
            } else {
                setupAuthentication(developmentToken)
            }
            DittoLog.d(TAG, "Ditto instance created successfully")
        }

        // Set the expiration handler before starting sync.
        // https://docs.ditto.live/sdk/latest/sync/authentication
        private fun setupAuthentication(token: String) {
            ditto.auth?.let { auth ->
                auth.expirationHandler = { ditto, _ ->
                    try {
                        val clientInfo = ditto.auth?.login(
                            token = token,
                            provider = DittoAuthenticationProvider.development()
                        )
                        DittoLog.d(TAG, "Auth response: $clientInfo")
                    } catch (ex: Throwable) {
                        DittoLog.e(TAG, "Authentication failed: $ex")
                    }
                }
            }
            DittoLog.d(TAG, "Ditto authentication setup complete")
        }

        /** Start syncing data with other devices. */
        // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
        fun startSync() {
            if (!ditto.sync.isActive) {
                ditto.sync.start()
            }
        }

        /** Stop syncing data with other devices. */
        fun stopSync() {
            if (ditto.sync.isActive) {
                ditto.sync.stop()
            }
        }

        /** Whether sync is currently active. */
        val isSyncActive: Boolean
            get() = ditto.sync.isActive
    }
}
