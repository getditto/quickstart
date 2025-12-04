package live.ditto.quickstart.dittowrapper

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import live.ditto.Ditto
import live.ditto.DittoIdentity
import live.ditto.DittoQueryResult
import live.ditto.DittoSignalNext
import live.ditto.DittoStoreObserver
import live.ditto.DittoSyncSubscription
import live.ditto.android.DefaultAndroidDittoDependencies
import live.ditto.transports.DittoSyncPermissions
import java.util.UUID

class DittoManager(private val applicationContext: Context) {

    lateinit var ditto: Ditto

    val isSyncActive
        get() = ditto.isSyncActive

    private val activeSubscriptions = mutableMapOf<String, DittoSyncSubscription>()
    private val activeObservers = mutableMapOf<String, DittoStoreObserver>()

    suspend fun initDitto(
        appId: String,
        token: String,
        customAuthUrl: String,
        webSocketUrl: String
    ) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Initializing Ditto...")

        val androidDependencies = DefaultAndroidDittoDependencies(applicationContext)
        val identity = DittoIdentity.OnlinePlayground(
            dependencies = androidDependencies,
            appId = appId,
            token = token,
            customAuthUrl = customAuthUrl,
            enableDittoCloudSync = false
        )

        ditto = Ditto(androidDependencies, identity).also { dittoInstance ->
            dittoInstance.updateTransportConfig { config ->
                config.connect.websocketUrls.add(webSocketUrl)
            }

            dittoInstance.disableSyncWithV3()

            dittoInstance.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false")
        }

        Log.d(TAG, "Ditto initialized and sync started")
    }

    fun startSync() {
        ditto.startSync()
    }

    fun stopSync() {
        ditto.stopSync()
    }

    fun getMissingPermissions(): List<String> =
        DittoSyncPermissions(applicationContext).requiredPermissions()

    /**
     * Registers the subscription with Ditto
     * Internally this class keeps track of the subscriptions so that later subscriptions can be
     * closed by providing the string again
     *
     * @param query the subscription query with or without argument placeholders
     * @param args query arguments
     *
     * @return a randomly generated UUID to reference the [DittoSyncSubscription]
     */
    fun registerSubscription(query: String, args: Map<String, Any>? = null): String {
        val subscription = ditto.sync.registerSubscription(query, args)
        val uuid = UUID.randomUUID().toString()
        activeSubscriptions[uuid] = subscription
        return uuid
    }

    fun closeSubscription(uuid: String) {
        val subscription = activeSubscriptions[uuid]
        subscription?.close()
        activeSubscriptions.remove(uuid)
    }

    suspend fun execute(query: String, args: Map<String, Any>?): QueryResult {
        val result = ditto.store.execute(query, args)
        return result.use { dittoQueryResult ->
            QueryResult(
                resultJson = dittoQueryResult.items.map { it.jsonString() },
                mutatedIds = dittoQueryResult.mutatedDocumentIds().map { it.toString() }
            )
        }
    }

    /**
     * Registers an observer callback with Ditto and returns a UUID String to reference the
     * observer so that it can be used later to close the observer. This is to manage the lifecycle
     * with AIDL implementations, as it may be difficult to use Kotlin idiomatic approaches (e.g.
     * a callbackFlow)
     *
     * We keep track of observers to play nicely with AIDL implementation
     *
     * @param query the query string with or without query placeholders
     * @param args any arguments to replace placeholders in the query string
     *
     * @return a UUID string reference to the observer so it can be closed
     */
    fun registerObserver(query: String, args: Map<String, Any>?, onResult: (DittoQueryResult, DittoSignalNext) -> Unit): String {
        val observer = ditto.store.registerObserver(
            query = query,
            arguments = args
        ) { result, signalNext ->
            onResult(result, signalNext)
        }
        val uuid = UUID.randomUUID().toString()
        activeObservers[uuid] = observer
        return uuid
    }

    fun closeObserver(uuid: String) {
        val observer = activeObservers[uuid]
        observer?.close()
        activeObservers.remove(uuid)
    }

    companion object {
        private const val TAG = "DittoManager"
    }
}