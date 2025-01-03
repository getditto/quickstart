package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import android.util.Log
import java.io.File

class TasksApplication : Application() {

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

    override fun onCreate() {
        super.onCreate()
        setupDitto()
    }

    private fun setupDitto() {
        val appId = BuildConfig.DITTO_APP_ID
        val token = BuildConfig.DITTO_PLAYGROUND_TOKEN

        try {
            val appContext = applicationContext()
            val persistenceDir = File(appContext.filesDir, "ditto")
            persistenceDir.mkdirs()
            TasksLib.initDitto(applicationContext(), appId, token, persistenceDir.path)
        } catch (e: Exception) {
            Log.e(TAG, "unable to initialize Ditto", e)
        }
    }
}
