package live.ditto.quickstart.dittowrapper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.quickstart.dittowrapper.aidl.IDittoManager

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

    private val binder = object : IDittoManager.Stub() {

        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        override fun initDitto(
            appId: String,
            token: String,
            customAuthUrl: String,
            webSocketUrl: String
        ) {

            Log.d("BINDER_TEST", "initDitto()")
//            coroutineScope.launch {
//                this@DittoManager.initDitto(
//                    appId = appId,
//                    token = token,
//                    customAuthUrl = customAuthUrl,
//                    webSocketUrl = webSocketUrl
//                )
//            }
        }

    }
}