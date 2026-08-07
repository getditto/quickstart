package com.example.dittotasks

import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoFactory

/**
 * Owns configuration and lifecycle of the Ditto instance: creating it, wiring up
 * development-mode authentication, and controlling sync. It knows nothing about tasks.
 *
 * The Ditto instance is a process-global singleton, created exactly once at app
 * startup (see [TasksApplication]) and living for the whole process. It is deliberately
 * NOT tied to any Activity: a configuration change such as rotation recreates the
 * Activity, but must never recreate Ditto — two live instances on the same persistence
 * directory would contend on its lock. Collaborators use the real Ditto API directly
 * through [ditto].
 */
class DittoManager {
    companion object {
        /** The configured Ditto instance. Collaborators use the real Ditto API through this. */
        lateinit var ditto: Ditto
            private set

        @JvmStatic
        val isInitialized: Boolean
            get() = ::ditto.isInitialized

        /**
         * Create and configure the Ditto instance and set up authentication. Called once,
         * from [TasksApplication.onCreate]. Does not start sync — call [startSync] for that.
         */
        @JvmStatic
        fun initialize(
            databaseId: String,
            serverUrl: String,
            authToken: String,
            offlineLicenseToken: String
        ) {
            if (::ditto.isInitialized) {
                throw IllegalStateException("Ditto is already initialized")
            }

            val normalizedOfflineLicenseToken = offlineLicenseToken.trim()
            val mode = DittoMode.select(normalizedOfflineLicenseToken)

            // Create Ditto with either a server or offline peer-to-peer connection.
            // https://docs.ditto.live/sdk/latest/install-guides/java#integrating-and-initializing
            val config = DittoConfig(
                databaseId = databaseId,
                connect = if (mode == DittoMode.OFFLINE) {
                    DittoConfig.Connect.SmallPeersOnly(privateKey = null)
                } else {
                    DittoConfig.Connect.Server(serverUrl)
                }
            )
            ditto = DittoFactory.create(config)

            if (mode == DittoMode.OFFLINE) {
                ditto.setOfflineOnlyLicenseToken(normalizedOfflineLicenseToken)
            } else {
                // Set up the authentication handler (must be configured before sync.start()).
                ditto.auth?.let { auth ->
                    auth.expirationHandler = { dittoInstance, _ ->
                        dittoInstance.auth?.login(authToken, DittoAuthenticationProvider.development())
                    }
                }
            }
        }

        // Start/stop sync.
        // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
        @JvmStatic
        fun startSync() {
            if (!ditto.sync.isActive) {
                ditto.sync.start()
            }
        }

        @JvmStatic
        fun stopSync() {
            if (ditto.sync.isActive) {
                ditto.sync.stop()
            }
        }

        @JvmStatic
        val isSyncActive: Boolean
            get() = ditto.sync.isActive
    }
}
