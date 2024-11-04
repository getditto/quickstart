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
        // Create an instance of Ditto
        ditto = Ditto(
            androidDependencies, DittoIdentity.OnlinePlayground(
                androidDependencies,
                "ea76785d-812f-4286-ac4a-e8e27c2455b9",
                "3f8c0a0b-588f-4b54-bdd3-0afe2e54fd29",
                enableDittoCloudSync = true
            )
        )

        DittoLogger.minimumLogLevel = DittoLogLevel.DEBUG

        // Disable sync with V3
        ditto.disableSyncWithV3()
    }
}
