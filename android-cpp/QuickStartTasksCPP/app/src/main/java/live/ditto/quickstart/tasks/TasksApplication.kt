package live.ditto.quickstart.tasks

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File

/// Determine whether the app is running on an emulator.
///
/// credit: <https://stackoverflow.com/a/21505193/1175>
val isProbablyRunningOnEmulator: Boolean by lazy {
    return@lazy (
            // Android SDK emulator
            Build.MANUFACTURER == "Google" && Build.BRAND == "google" &&
                    ((Build.FINGERPRINT.startsWith("google/sdk_gphone_")
                            && Build.FINGERPRINT.endsWith(":user/release-keys")
                            && Build.PRODUCT.startsWith("sdk_gphone_")
                            && Build.MODEL.startsWith("sdk_gphone_"))
                            //alternative
                            || (Build.FINGERPRINT.startsWith("google/sdk_gphone64_") && (Build.FINGERPRINT.endsWith(
                        ":userdebug/dev-keys"
                    )
                            || (Build.FINGERPRINT.endsWith(":user/release-keys")) && Build.PRODUCT.startsWith(
                        "sdk_gphone64_"
                    )
                            && Build.MODEL.startsWith("sdk_gphone64_")))
                            //Google Play Games emulator https://play.google.com/googleplaygames https://developer.android.com/games/playgames/emulator#other-downloads
                            || (Build.MODEL == "HPE device" &&
                            Build.FINGERPRINT.startsWith("google/kiwi_") && Build.FINGERPRINT.endsWith(
                        ":user/release-keys"
                    )
                            && Build.BOARD == "kiwi" && Build.PRODUCT.startsWith("kiwi_"))
                            )
                    //
                    || Build.FINGERPRINT.startsWith("generic")
                    || Build.FINGERPRINT.startsWith("unknown")
                    || Build.MODEL.contains("google_sdk")
                    || Build.MODEL.contains("Emulator")
                    || Build.MODEL.contains("Android SDK built for x86")
                    //bluestacks
                    || "QC_Reference_Phone" == Build.BOARD && !"Xiaomi".equals(
                Build.MANUFACTURER,
                ignoreCase = true
            )
                    //bluestacks
                    || Build.MANUFACTURER.contains("Genymotion")
                    || Build.HOST.startsWith("Build")
                    //MSI App Player
                    || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                    || Build.PRODUCT == "google_sdk")
}

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
        Log.d(TAG, "🚀 TasksApplication.onCreate() called!")
        setupDitto()
    }

    private fun setupDitto() {
        val appId = BuildConfig.DITTO_APP_ID
        val token = BuildConfig.DITTO_PLAYGROUND_TOKEN
        val authUrl = BuildConfig.DITTO_AUTH_URL
        val webSocketURL = BuildConfig.DITTO_WEBSOCKET_URL

        try {
            Log.d(TAG, "🔧 Setting up Ditto with AppID: $appId")
            Log.d(TAG, "🌐 WebSocket URL: $webSocketURL")
            Log.d(TAG, "🏃‍♂️ Running on emulator: $isProbablyRunningOnEmulator")
            
            val appContext = applicationContext()
            val persistenceDir = File(appContext.filesDir, "ditto")
            persistenceDir.mkdirs()
            
            Log.d(TAG, "📁 Persistence dir: ${persistenceDir.path}")

            TasksLib.initDitto(
                appContext,
                appId,
                token,
                persistenceDir.path,
                isProbablyRunningOnEmulator,
                authUrl,
                webSocketURL
            )
            Log.d(TAG, "🚀 Ditto initialized, starting sync...")
            TasksLib.startSync()
            Log.d(TAG, "✅ Ditto sync started!")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize Ditto", e)
        }
    }
}
