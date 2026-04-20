package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoFactory
import com.ditto.kotlin.DittoLog

class TasksApplication : Application() {

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
        initializeDitto()
        setupAuthentication()
    }

    private fun initializeDitto() {
        try {
            val config = DittoConfig(
                databaseId = BuildConfig.DITTO_APP_ID,
                connect = DittoConfig.Connect.Server(url = BuildConfig.DITTO_AUTH_URL)
            )

            DittoHandler.initialize(config)
            DittoLog.d(tag, "Ditto instance created successfully")
        } catch (ex: Throwable) {
            DittoLog.e(tag, "Failed to initialize Ditto: $ex")
            throw ex
        }
    }

    private fun setupAuthentication() {
        try {
            val token = BuildConfig.DITTO_PLAYGROUND_TOKEN

            // Set the expiration handler before starting sync
            // https://docs.ditto.live/sdk/latest/sync/authentication
            DittoHandler.ditto.auth?.let { auth ->
                auth.expirationHandler = { ditto, _ ->
                    try {
                        val clientInfo = ditto.auth?.login(
                            token = token,
                            provider = DittoAuthenticationProvider.development()
                        )
                        DittoLog.d(tag, "Auth response: $clientInfo")
                    } catch (ex: Throwable) {
                        DittoLog.e(tag, "Authentication failed: $ex")
                    }
                }
            }
            DittoLog.d(tag, "Ditto authentication setup complete")
        } catch (ex: Throwable) {
            DittoLog.e(tag, "Failed to setup authentication: $ex")
        }
    }
}
