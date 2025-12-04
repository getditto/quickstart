package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto

class TasksApplication : Application() {

    // Create a CoroutineScope
    // Use SupervisorJob so if one coroutine launched in this scope fails, it doesn't cancel the scope
    //
    // https://developer.android.com/kotlin/coroutines/coroutines-adv
    // Dispatchers.IO - This dispatcher is optimized to perform disk or network I/O outside of the main thread.
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)


    companion object {

        private const val TAG = "TasksApplication"
        private var instance: TasksApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    init {
        instance = this
    }

    // AIDL Service Connection - exposed publicly for MainActivity access
    lateinit var dittoServiceConnection: DittoServiceConnection
        private set

    override fun onCreate() {
        super.onCreate()

        // Bind to AIDL service
        dittoServiceConnection = DittoServiceConnection(this)
        dittoServiceConnection.bind()

        // Wait for service to bind, then call initDitto
        ioScope.launch {
            // Original Ditto setup (keep this for now)
            setupDitto()

            // Wait for the service connection to complete
            Log.d(TAG, "Waiting for AIDL service connection...")
            val connected = dittoServiceConnection.awaitConnection()

            if (connected) {
                Log.d(TAG, "AIDL service connected successfully!")
                // Call initDitto with placeholder values
                dittoServiceConnection.initDitto(
                    appId = BuildConfig.DITTO_APP_ID,
                    token = BuildConfig.DITTO_PLAYGROUND_TOKEN,
                    customAuthUrl = BuildConfig.DITTO_AUTH_URL,
                    webSocketUrl = BuildConfig.DITTO_WEBSOCKET_URL
                )
            } else {
                Log.e(TAG, "Failed to connect to AIDL service. Make sure dittowrapper APK is installed.")
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        dittoServiceConnection.unbind()
    }

    //todo: remove when aidl setup is complete
    private suspend fun setupDitto() {
        val androidDependencies = DefaultAndroidDittoDependencies(applicationContext)

        //read values from build.gradle.kts (Module:app) which reads from environment file
        val appId = BuildConfig.DITTO_APP_ID
        val token = BuildConfig.DITTO_PLAYGROUND_TOKEN
        val authUrl = BuildConfig.DITTO_AUTH_URL
        val webSocketURL = BuildConfig.DITTO_WEBSOCKET_URL

        val enableDittoCloudSync = false

        /*
         *  Setup Ditto Identity
         *  https://docs.ditto.live/sdk/latest/install-guides/kotlin#integrating-and-initializing
         */
        val identity = DittoIdentity.OnlinePlayground(
            dependencies = androidDependencies,
            appId = appId,
            token = token,
            customAuthUrl = authUrl,
            enableDittoCloudSync = enableDittoCloudSync // This is required to be set to false to use the correct URLs
        )

        ditto = Ditto(androidDependencies, identity)
        ditto.updateTransportConfig { config ->
            // Set the Ditto Websocket URL
            config.connect.websocketUrls.add(webSocketURL)
        }

        ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")

        // disable sync with v3 peers, required for DQL
        ditto.disableSyncWithV3()
    }

}
