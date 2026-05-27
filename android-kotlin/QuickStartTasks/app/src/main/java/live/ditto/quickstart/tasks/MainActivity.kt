package live.ditto.quickstart.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.ditto.kotlin.DittoLog
import com.ditto.kotlin.transports.DittoSyncPermissions

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val denied = results.filterValues { granted -> !granted }.keys
        if (denied.isNotEmpty()) {
            DittoLog.w(
                TAG,
                "Sync transport permissions denied: $denied. P2P discovery may be limited."
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            Root()
        }

        requestMissingPermissions()
    }

    // Requesting permissions at runtime
    // https://docs.ditto.live/sdk/latest/install-guides/kotlin#requesting-permissions-at-runtime
    private fun requestMissingPermissions() {
        val missing = DittoSyncPermissions(this).missingPermissions()
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
