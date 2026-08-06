package com.example.dittotasks

import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoFactory

/**
 * Owns Ditto instance management for the app: configuration, identity/auth, and
 * starting/stopping sync. It knows nothing about tasks — the tasks concern lives
 * in [TasksRepository], which reaches Ditto through the [ditto] instance exposed here.
 */
class DittoManager(
    databaseId: String,
    serverUrl: String,
    authToken: String,
    offlineLicenseToken: String
) {

    /** The configured Ditto instance. Collaborators use the real Ditto API through this. */
    val ditto: Ditto

    init {
        val mode = DittoMode.select(offlineLicenseToken.trim())

        // Create Ditto with either a server connection or an offline-only mesh.
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
            ditto.setOfflineOnlyLicenseToken(offlineLicenseToken.trim())
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
    fun startSync() {
        ditto.sync.start()
    }

    fun stopSync() {
        ditto.sync.stop()
    }

    val isSyncActive: Boolean
        get() = ditto.sync.isActive
}
