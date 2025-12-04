package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

}
