package live.ditto.quickstart.dittowrapper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import live.ditto.quickstart.dittowrapper.aidl.IDittoManager

/**
 * AIDL Service that exposes Ditto functionality to other applications.
 */
class DittoService : Service() {

    private lateinit var dittoManager: DittoManager

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val binder = object : IDittoManager.Stub() {
        override fun initDitto(
            appId: String,
            token: String,
            customAuthUrl: String,
            webSocketUrl: String
        ) {
            coroutineScope.launch {
                try {
                    dittoManager.initDitto(
                        appId = appId,
                        token = token,
                        customAuthUrl = customAuthUrl,
                        webSocketUrl = webSocketUrl
                    )
                    Log.d(TAG, "Ditto initialized successfully via AIDL")
                } catch (e: Exception) {
                    Log.e(TAG, "Error initializing Ditto via AIDL", e)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "DittoService created")
        dittoManager = DittoManager(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d(TAG, "DittoService bound by client")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "DittoService unbound by client")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "DittoService destroyed")
    }

    companion object {
        private const val TAG = "DittoService"
    }
}