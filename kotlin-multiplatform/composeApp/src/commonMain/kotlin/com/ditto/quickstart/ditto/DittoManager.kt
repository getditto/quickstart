package com.ditto.quickstart.ditto

import com.ditto.example.kotlin.quickstart.configuration.DittoSecretsConfiguration
import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoIdentity
import com.ditto.kotlin.DittoLog
import com.ditto.kotlin.DittoLogLevel
import com.ditto.kotlin.DittoLogger
import com.ditto.kotlin.DittoQueryResult
import com.ditto.kotlin.DittoSyncSubscription
import com.ditto.kotlin.error.DittoError
import com.ditto.kotlin.serialization.DittoCborSerializable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private const val TAG = "DittoManager"

/**
 * Manages a Ditto instance.
 *
 * In cases where a ViewModel needs to interact with this class, then create a UseCase for it.
 * Keeping this class inaccessible from the ViewModel, will prevent the abuse of ditto APIs like:
 * "ditto.sync", "ditto.transport", "ditto.store", etc
 *
 * Because ditto also have a "database" component, it is fine to expose this class to a Repository.
 */
class DittoManager {
    private val scope = CoroutineScope(SupervisorJob())
    private var createJob: Job? = null
    private var closeJob: Job? = null
    private var ditto: Ditto? = null

    suspend fun createDitto() {
        if (getDitto() != null) return

        // SDKS-1294: Don't create Ditto in a scope using Dispatchers.IO
        createJob = scope.launch(Dispatchers.Default) {
            ditto = try {
                val identity = DittoIdentity.OnlinePlayground(
                    appId = DittoSecretsConfiguration.DITTO_APP_ID,
                    token = DittoSecretsConfiguration.DITTO_PLAYGROUND_TOKEN,
                    // Cloud sync is intentionally disabled to avoid authentication issues in test environments.
                    // When enabled, Ditto Cloud Sync requires additional auth setup that causes certificate 
                    // validation failures in BrowserStack. Disabling ensures sync occurs via WebSocket URLs only.
                    enableDittoCloudSync = false,
                    customAuthUrl = DittoSecretsConfiguration.DITTO_AUTH_URL,
                )

                val config = createDittoConfig(identity = identity)

                DittoLogger.minimumLogLevel = DittoLogLevel.Verbose
                println("🔧 DittoManager: Setting up Ditto with verbose logging...")
                println("🔧 DittoManager: App ID: ${DittoSecretsConfiguration.DITTO_APP_ID}")
                println("🔧 DittoManager: WebSocket URL: ${DittoSecretsConfiguration.DITTO_WEBSOCKET_URL}")
                println("🔧 DittoManager: Auth URL: ${DittoSecretsConfiguration.DITTO_AUTH_URL}")
                Ditto(config = config).apply {
                    updateTransportConfig { config ->
                        config.connect.websocketUrls.add(DittoSecretsConfiguration.DITTO_WEBSOCKET_URL)
                    }
                }
            } catch (e: Throwable) {
                DittoLog.e(TAG, "Failed to create Ditto instance: $e")
                null
            }
        }
    }

    suspend fun isDittoCreated() = getDitto() != null

    suspend fun getDitto(): Ditto? {
        waitForWorkInProgress()
        return ditto
    }

    suspend fun executeDql(
        query: String,
        parameters: DittoCborSerializable.Dictionary = DittoCborSerializable.Dictionary()
    ): DittoQueryResult? = try {
        getDitto()?.store?.execute(query, parameters)
    } catch (e: DittoError) {
        DittoLog.e("ExecuteDqlUse", "Error executing DQL query: ${e.message}")
        null
    }

    suspend fun registerSubscription(
        query: String,
        arguments: DittoCborSerializable.Dictionary? = null
    ): DittoSyncSubscription? = try {
        getDitto()?.sync?.registerSubscription(query, arguments)
    } catch (e: DittoError) {
        DittoLog.e("RegisterSubscription", "Error registering subscription: ${e.message}")
        null
    }

    suspend fun registerObserver(
        query: String,
        arguments: DittoCborSerializable.Dictionary? = null
    ): Flow<DittoQueryResult> = requireNotNull(getDitto()).store.registerObserver(
        query = query,
        arguments = arguments
    )

    suspend fun startSync() {
        val ditto = getDitto()
        if (ditto == null) {
            println("❌ DittoManager: Cannot start sync - Ditto instance is null!")
            return
        }
        
        println("🔄 DittoManager: Starting sync on Ditto instance...")
        println("🔧 DittoManager: Transport config before sync:")
        println("🔧 DittoManager: WebSocket URLs: ${ditto.transportConfig.connect.websocketUrls}")
        
        ditto.startSync()
        println("✅ DittoManager: Ditto.startSync() called")
        
        // Give a moment for sync to initialize
        kotlinx.coroutines.delay(1000)
        
        val isActive = ditto.isSyncActive
        println("🔍 DittoManager: Sync active status: $isActive")
        
        // Log transport status
        println("🌐 DittoManager: Transport status after sync start:")
        println("🌐 DittoManager: WebSocket URLs configured: ${ditto.transportConfig.connect.websocketUrls.size}")
        ditto.transportConfig.connect.websocketUrls.forEachIndexed { index, url ->
            println("🌐 DittoManager: WebSocket URL[$index]: $url")
        }
    }

    suspend fun stopSync() {
        getDitto()?.stopSync()
    }

    suspend fun isSyncing() = getDitto()?.isSyncActive == true

    fun destroyDitto() {
        closeJob = scope.launch(Dispatchers.IO) {
            getDitto()?.stopSync()
            getDitto()?.close()
            ditto = null
        }
    }

    private suspend fun waitForWorkInProgress() {
        createJob?.join()
        closeJob?.join()
    }
}

/**
 * Defines how to create a Ditto Config in Multiplatform, and on each platform pass the required dependencies - for
 * example, on Android we require Context.
 */
internal expect fun createDittoConfig(
    identity: DittoIdentity,
): DittoConfig
