package live.ditto.quickstart.dittowrapper

import android.app.Service
import android.content.Intent
import android.os.Bundle
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
        override fun isSyncActive(): Boolean = dittoManager.isSyncActive

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

        override fun getMissingPermissions(): List<String> = dittoManager.getMissingPermissions()

        override fun startSync() {
            dittoManager.startSync()
        }

        @Suppress("DEPRECATION")
        override fun registerSubscription(subscriptionQuery: String, args: Bundle?): String {

            // Convert Bundle to Map<String, Any>?
            val argsMap: Map<String, Any>? = args?.let { bundle ->
                bundle.keySet().associateWith { key ->
                    bundle.get(key) ?: throw IllegalStateException("Could not get bundle value for key=$key")
                }
            }

            return dittoManager.registerSubscription(
                query = subscriptionQuery,
                args = argsMap
            )
        }

        override fun closeSubscription(uuid: String) {
            dittoManager.closeSubscription(uuid)
        }

        override fun stopSync() {
            dittoManager.stopSync()
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