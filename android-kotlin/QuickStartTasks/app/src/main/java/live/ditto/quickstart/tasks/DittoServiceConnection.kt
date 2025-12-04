package live.ditto.quickstart.tasks

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import live.ditto.quickstart.dittowrapper.aidl.IDittoManager
import live.ditto.quickstart.dittowrapper.aidl.IObserverCallback
import live.ditto.quickstart.dittowrapper.aidl.QueryResult

/**
 * Helper class to manage connection to the AIDL DittoService
 */
class DittoServiceConnection(private val context: Context) {

    private var dittoManager: IDittoManager? = null

    private var bound = false
    private val connectionDeferred = CompletableDeferred<Boolean>()
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(TAG, "Service connected: $name")
            dittoManager = IDittoManager.Stub.asInterface(service)
            bound = true
            connectionDeferred.complete(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service disconnected: $name")
            dittoManager = null
            bound = false
        }
    }

    /**
     * Bind to the DittoService
     */
    fun bind() {
        Log.d(TAG, "Binding to DittoService...")
        val intent = Intent().apply {
            component = ComponentName(
                "live.ditto.quickstart.dittowrapper",
                "live.ditto.quickstart.dittowrapper.DittoService"
            )
        }

        try {
            val success = context.bindService(
                intent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            if (success) {
                Log.d(TAG, "Service binding initiated successfully")
            } else {
                Log.e(TAG, "Failed to bind to service - bindService returned false")
                connectionDeferred.complete(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while binding to service", e)
            connectionDeferred.complete(false)
        }
    }

    /**
     * Suspend function to wait for the service to be connected
     * @param timeoutMillis Maximum time to wait for connection (default 5 seconds)
     * @return true if connected, false if failed or timed out
     */
    suspend fun awaitConnection(timeoutMillis: Long = 5000): Boolean {
        return try {
            kotlinx.coroutines.withTimeout(timeoutMillis) {
                connectionDeferred.await()
            }
        } catch (_: TimeoutCancellationException) {
            Log.e(TAG, "Timeout waiting for service connection after ${timeoutMillis}ms")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error waiting for connection", e)
            false
        }
    }

    /**
     * Unbind from the DittoService
     */
    fun unbind() {
        if (bound) {
            Log.d(TAG, "Unbinding from DittoService")
            context.unbindService(serviceConnection)
            bound = false
            dittoManager = null
        }
    }

    /**
     * Call initDitto on the remote service
     */
    fun initDitto(appId: String, token: String, customAuthUrl: String, webSocketUrl: String) {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call initDitto - service not bound")
            return
        }

        try {
            Log.d(TAG, "Calling initDitto on remote service...")
            dittoManager?.initDitto(appId, token, customAuthUrl, webSocketUrl)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling initDitto", e)
        }
    }

    /**
     * Get missing permissions from the remote service
     * @return List of missing permissions, or empty list if service not bound
     */
    fun getMissingPermissions(): List<String> {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call getMissingPermissions - service not bound")
            return emptyList()
        }

        return try {
            Log.d(TAG, "Calling getMissingPermissions on remote service...")
            dittoManager?.getMissingPermissions() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling getMissingPermissions", e)
            emptyList()
        }
    }

    /**
     * Check if sync is currently active
     * @return true if sync is active, false otherwise
     */
    fun isSyncActive(): Boolean {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call isSyncActive - service not bound")
            return false
        }

        return try {
            dittoManager?.isSyncActive() ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error calling isSyncActive", e)
            false
        }
    }

    /**
     * Start sync on the remote service
     */
    fun startSync() {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call startSync - service not bound")
            return
        }

        try {
            Log.d(TAG, "Calling startSync on remote service...")
            dittoManager?.startSync()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling startSync", e)
        }
    }

    /**
     * Stop sync on the remote service
     */
    fun stopSync() {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call stopSync - service not bound")
            return
        }

        try {
            Log.d(TAG, "Calling stopSync on remote service...")
            dittoManager?.stopSync()
        } catch (e: Exception) {
            Log.e(TAG, "Error calling stopSync", e)
        }
    }

    /**
     * Register a subscription with the remote service
     * @param subscriptionQuery DQL query string
     * @param args Query arguments (optional)
     * @return UUID string to reference this subscription, or null if failed
     */
    fun registerSubscription(subscriptionQuery: String, args: Bundle? = null): String? {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call registerSubscription - service not bound")
            return null
        }

        return try {
            Log.d(TAG, "Calling registerSubscription on remote service...")
            dittoManager?.registerSubscription(subscriptionQuery, args)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling registerSubscription", e)
            null
        }
    }

    /**
     * Close a subscription on the remote service
     * @param uuid UUID string reference to the subscription
     */
    fun closeSubscription(uuid: String) {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call closeSubscription - service not bound")
            return
        }

        try {
            Log.d(TAG, "Calling closeSubscription on remote service...")
            dittoManager?.closeSubscription(uuid)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling closeSubscription", e)
        }
    }

    /**
     * Execute a DQL query on the remote service
     * @param query DQL query string
     * @param args Query arguments as Map (optional)
     * @return QueryResult containing the result JSON and mutated IDs, or null if failed
     */
    fun execute(query: String, args: Map<String, Any>? = null): QueryResult? {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call execute - service not bound")
            return null
        }

        return try {
            Log.d(TAG, "Calling execute on remote service...")
            val argsBundle = args?.toBundle()
            dittoManager?.execute(query, argsBundle)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling execute", e)
            null
        }
    }

    /**
     * Register an observer with a callback for result updates
     * @param query DQL query string
     * @param args Query arguments (optional)
     * @param onResult Callback function to handle result updates
     * @return UUID string to reference this observer, or null if failed
     */
    fun registerObserver(
        query: String,
        args: Map<String, Any>? = null,
        onResult: (List<String>) -> Unit
    ): String? {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call registerObserver - service not bound")
            return null
        }

        return try {
            Log.d(TAG, "Calling registerObserver on remote service...")
            val callback = object : IObserverCallback.Stub() {
                override fun onResult(resultJson: List<String>) {
                    onResult(resultJson)
                }
            }
            val argsBundle = args?.toBundle()
            dittoManager?.registerObserver(query, argsBundle, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling registerObserver", e)
            null
        }
    }

    /**
     * Close an observer on the remote service
     * @param uuid UUID string reference to the observer
     */
    fun closeObserver(uuid: String) {
        if (!bound || dittoManager == null) {
            Log.e(TAG, "Cannot call closeObserver - service not bound")
            return
        }

        try {
            Log.d(TAG, "Calling closeObserver on remote service...")
            dittoManager?.closeObserver(uuid)
        } catch (e: Exception) {
            Log.e(TAG, "Error calling closeObserver", e)
        }
    }

    /**
     * Convert Map to Bundle for AIDL calls
     */
    private fun Map<String, Any>.toBundle(): Bundle {
        return Bundle().apply {
            this@toBundle.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Double -> putDouble(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is ArrayList<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        when {
                            value.all { it is String } -> putStringArrayList(key, value as ArrayList<String>)
                            value.all { it is Int } -> putIntegerArrayList(key, value as ArrayList<Int>)
                            else -> throw IllegalArgumentException("Unsupported ArrayList type for key: $key")
                        }
                    }
                    else -> throw IllegalArgumentException("Unsupported value type for key: $key, type: ${value::class.java}")
                }
            }
        }
    }

    companion object {
        private const val TAG = "DittoServiceConnection"
    }

}