package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoConnection
import com.ditto.kotlin.DittoFactory
import com.ditto.kotlin.DittoLog
import com.ditto.kotlin.error.DittoException
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto

class TasksApplication : Application() {

    // Create a CoroutineScope
    // Use SupervisorJob so if one coroutine launched in this scope fails, it doesn't cancel the scope
    // SDKS-1294: Don't create Ditto in a scope using Dispatchers.IO
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tag = "TaskApplication"

    companion object {
        private var instance: TasksApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }

    init {
        instance = this
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Ditto synchronously - completes before UI loads
        initializeDitto()

        // Perform authentication asynchronously - can happen in background
        scope.launch {
            performAuthentication()
        }
    }

    private fun initializeDitto() {
        try {
            val appId = BuildConfig.DITTO_APP_ID
            val authUrl = BuildConfig.DITTO_AUTH_URL

            val config = DittoConfig(
                databaseId = appId,
                connect = DittoConfig.Connect.Server(url = authUrl)
            )

            DittoHandler.initialize(config)
            DittoLog.d(tag, "Ditto instance created successfully")

        } catch (ex: Throwable) {
            DittoLog.e(tag, "Failed to initialize Ditto: $ex")
            ex.printStackTrace()
            throw ex
        }
    }

    private suspend fun performAuthentication() {
        try {
            val token = BuildConfig.DITTO_PLAYGROUND_TOKEN

            DittoHandler.ditto.auth?.setExpirationHandler { ditto, _ ->
                try {
                    val clientInfo = ditto.auth?.login(
                        token = token,
                        provider = DittoAuthenticationProvider.development()
                    )
                    DittoLog.d(tag, "Auth response: $clientInfo")
                } catch (ex: Throwable) {
                    DittoLog.e(tag, "Authentication failed: $ex")
                    ex.printStackTrace()
                }
            }

            DittoLog.d(tag, "Ditto authentication setup complete")

        } catch (ex: Throwable) {
            DittoLog.e(tag, "Failed to setup authentication: $ex")
            ex.printStackTrace()
        }
    }
}
