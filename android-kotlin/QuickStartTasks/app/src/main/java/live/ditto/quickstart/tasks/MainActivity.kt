package live.ditto.quickstart.tasks

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
        // Get the application instance to access the AIDL service connection
        val app = application as TasksApplication

        // Get missing permissions from the AIDL DittoService
        val missingPermissions = app.dittoServiceConnection.getMissingPermissions()

        Log.d(TAG, "Missing permissions from AIDL service: $missingPermissions")

        if (missingPermissions.isNotEmpty()) {
            this.requestPermissions(missingPermissions.toTypedArray(), 0)
        } else {
            Log.d(TAG, "All required permissions already granted")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}



