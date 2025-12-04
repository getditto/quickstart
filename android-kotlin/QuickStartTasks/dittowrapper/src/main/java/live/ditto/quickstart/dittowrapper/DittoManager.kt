package live.ditto.quickstart.dittowrapper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.DittoSyncSubscription
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.transports.DittoSyncPermissions
import java.util.UUID

class DittoManager(private val applicationContext: Context) {

    lateinit var ditto: Ditto

    val isSyncActive
        get() = ditto.isSyncActive

    private val activeSubscriptions = mutableMapOf<String, DittoSyncSubscription>()

    suspend fun initDitto(
        appId: String,
        token: String,
        customAuthUrl: String,
        webSocketUrl: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing Ditto...")

        val androidDependencies = DefaultAndroidDittoDependencies(applicationContext)
        val identity = DittoIdentity.OnlinePlayground(
            dependencies = androidDependencies,
            appId = appId,
            token = token,
            customAuthUrl = customAuthUrl,
            enableDittoCloudSync = false
        )

        ditto = Ditto(androidDependencies, identity).also { dittoInstance ->
            dittoInstance.updateTransportConfig { config ->
                config.connect.websocketUrls.add(webSocketUrl)
            }

            dittoInstance.disableSyncWithV3()

            dittoInstance.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")
        }

        Log.d(TAG, "Ditto initialized and sync started")
    }

    fun startSync() {
        ditto.startSync()
    }

    fun stopSync() {
        ditto.stopSync()
    }

    fun getMissingPermissions(): List<String> = DittoSyncPermissions(applicationContext).requiredPermissions()

    /**
     * Registers the subscription with Ditto
     * Internally this class keeps track of the subscriptions so that later subscriptions can be
     * closed by providing the string again
     *
     * @param query the subscription query with or without argument placeholders
     * @param args query arguments
     *
     * @return a randomly generated UUID to reference the [DittoSyncSubscription]
     */
    fun registerSubscription(query: String, args: Map<String, Any>? = null): String {
        val subscription = ditto.sync.registerSubscription(query, args)
        val uuid = UUID.randomUUID().toString()
        activeSubscriptions[uuid] = subscription
        return uuid
    }

    fun closeSubscription(uuid: String) {
        val subscription = activeSubscriptions[uuid]
        subscription?.close()
    }

    companion object {
        private const val TAG = "DittoManager"
    }
}