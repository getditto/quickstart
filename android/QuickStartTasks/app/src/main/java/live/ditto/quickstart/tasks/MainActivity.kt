package live.ditto.quickstart.tasks

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import live.ditto.DittoError
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.transports.DittoSyncPermissions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            ditto.startSync()
        } catch (e: DittoError) {
            Toast.makeText(
                this@MainActivity,
                """
                    Uh oh! There was an error trying to start Ditto's sync feature.
                    That's okay, it will still work as a local database.
                    This is the error: ${e.localizedMessage}
                """.trimIndent(), Toast.LENGTH_LONG
            ).show()
        }

        setContent {
            Root()
        }

        lifecycleScope.launch {
            ditto.store.execute("EVICT FROM tasks WHERE isDeleted = true")
        }

        requestMissingPermissions()
    }

    private fun requestMissingPermissions() {
        val missingPermissions = DittoSyncPermissions(this).missingPermissions()
        if (missingPermissions.isNotEmpty()) {
            this.requestPermissions(missingPermissions, 0)
        }
    }

}



