package live.ditto.quickstart.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Root()
        }

        requestMissingPermissions()
    }

    private fun requestMissingPermissions() {
        // Check if TasksApplication has been initialized first
        val app = application as? TasksApplication
        if (app?.isInitialized() == true) {
            val missingPermissions = TasksLib.getMissingPermissions()
            if (missingPermissions.isNotEmpty()) {
                this.requestPermissions(missingPermissions, 0)
            }
        }
    }
}
