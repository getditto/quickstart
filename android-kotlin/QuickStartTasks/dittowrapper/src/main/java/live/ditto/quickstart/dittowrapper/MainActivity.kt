package live.ditto.quickstart.dittowrapper

import android.app.Activity
import android.os.Bundle

/**
 * Blank MainActivity for dittowrapper.
 *
 * This activity serves as a launcher to allow the APK to be installed independently.
 * The actual functionality is provided through AIDL services that can be accessed
 * by other applications.
 */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Blank activity - main functionality is through AIDL services
        finish() // Immediately close after launch
    }
}