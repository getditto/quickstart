package live.ditto.quickstart.dittowrapper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.transports.DittoSyncPermissions

class DittoManager(private val applicationContext: Context) {

    private val TAG = "DittoManager"

    lateinit var ditto: Ditto

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

            dittoInstance.startSync()
        }

        Log.d(TAG, "Ditto initialized and sync started")
    }

    fun getMissingPermissions(): List<String> = DittoSyncPermissions(applicationContext).requiredPermissions()
}