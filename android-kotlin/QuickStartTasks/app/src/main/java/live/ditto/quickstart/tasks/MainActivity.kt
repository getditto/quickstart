package live.ditto.quickstart.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import live.ditto.transports.DittoSyncPermissions
import android.os.StrictMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .penaltyLog() // Log violations to logcat
                    .build()
            )
        }

        setContent {
            Root()
        }

        requestMissingPermissions()
    }

    private fun requestMissingPermissions() {
        // requesting permissions at runtime
        // https://docs.ditto.live/sdk/latest/install-guides/kotlin#requesting-permissions-at-runtime
        val missingPermissions = DittoSyncPermissions(this).missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            this.requestPermissions(missingPermissions, 0)
        }
    }
}



