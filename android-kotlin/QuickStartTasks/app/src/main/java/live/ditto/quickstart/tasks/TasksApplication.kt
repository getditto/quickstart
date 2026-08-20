package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import com.ditto.kotlin.DittoConfig
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
    }

    private fun initializeDitto() {
        try {
            val config = DittoConfig(
                databaseId = BuildConfig.DITTO_DATABASE_ID,
                connect = DittoConfig.Connect.Server(url = BuildConfig.DITTO_SERVER_URL)
            )

            DittoManager.initialize(config, BuildConfig.DITTO_DEVELOPMENT_TOKEN)
        } catch (ex: Throwable) {
            DittoLog.e(tag, "Failed to initialize Ditto: $ex")
            throw ex
        }
    }
}
