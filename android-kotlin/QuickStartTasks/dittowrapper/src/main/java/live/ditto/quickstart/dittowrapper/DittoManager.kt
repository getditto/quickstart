package live.ditto.quickstart.dittowrapper

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.android.DefaultAndroidDittoDependencies

class DittoManager(private val applicationContext: Context) {

    lateinit var ditto: Ditto

    suspend fun initDitto(
        appId: String,
        token: String,
        customAuthUrl: String,
        webSocketUrl: String
    ) = withContext(Dispatchers.IO) {
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
    }
}