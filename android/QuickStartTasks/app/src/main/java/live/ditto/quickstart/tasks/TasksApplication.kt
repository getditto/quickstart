package live.ditto.quickstart.tasks

import android.app.Application
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.DittoLogLevel
import live.ditto.DittoLogger
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto

class TasksApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        setupDitto()
    }

    private fun setupDitto() {
        val androidDependencies = DefaultAndroidDittoDependencies(applicationContext)
        val appId = BuildConfig.APP_ID
        val token = BuildConfig.TOKEN
        val enableDittoCloudSync = true

        val identity = DittoIdentity.OnlinePlayground(
            androidDependencies,
            appId,
            token,
            enableDittoCloudSync
        )

        ditto = Ditto(androidDependencies, identity)

        DittoLogger.minimumLogLevel = DittoLogLevel.DEBUG

        ditto.disableSyncWithV3()
    }
}
