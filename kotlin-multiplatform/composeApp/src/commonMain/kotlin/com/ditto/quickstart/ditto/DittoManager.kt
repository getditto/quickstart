package com.ditto.quickstart.ditto

import com.ditto.example.kotlin.quickstart.configuration.DittoSecretsConfiguration
import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
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
 * Because ditto also has a "database" component, it is fine to expose this class to a Repository.
 */
class DittoManager {
    val secrets: DittoSecretsConfiguration

    constructor(secrets: DittoSecretsConfiguration) {
        this.secrets = secrets
        this.scope = CoroutineScope(SupervisorJob())
    }

    private val scope: CoroutineScope
    private var createJob: Job? = null
    private var closeJob: Job? = null
    private var ditto: Ditto? = null

    suspend fun createDitto() {
        if (getDitto() != null) return

        // SDKS-1294: Don't create Ditto in a scope using Dispatchers.IO
        createJob = scope.launch(Dispatchers.Default) {
            ditto = try {
                DittoLogger.minimumLogLevel = DittoLogLevel.Info

                val config = DittoConfig(
                    databaseId = secrets.DITTO_APP_ID,
                    connect = DittoConfig.Connect.Server(
                        url = secrets.DITTO_AUTH_URL,
                    ),
                )

                createDitto(
                    config = config
                ).apply {
                    auth?.setExpirationHandler { ditto, secondsRemaining ->
                        // Authenticate when a token is expiring
                        val clientInfo = ditto.auth?.login(
                            token = secrets.DITTO_PLAYGROUND_TOKEN,
                            provider = DittoAuthenticationProvider.development(),
                        )
                    }
                }.apply {
                    updateTransportConfig { config ->
                        config.peerToPeer.lan.enabled = true
                        config.peerToPeer.bluetoothLe.enabled = true
                        config.peerToPeer.wifiAware.enabled = true
                    }
                }
            } catch (e: Throwable) {
                DittoLog.e(TAG, "Failed to create Ditto instance: $e")
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun isDittoCreated() = getDitto() != null

    suspend fun getDitto(): Ditto? {
        waitForWorkInProgress()
        return ditto
    }

    fun destroyDitto() {
        closeJob = scope.launch(Dispatchers.IO) {
            getDitto()?.stopSync()
            getDitto()?.close()
            ditto = null
        }
    }

    suspend fun startSync() {
        val ditto = getDitto() ?: return
        ditto.startSync()
    }

    suspend fun stopSync() {
        getDitto()?.stopSync()
    }

    suspend fun isSyncing() = getDitto()?.isSyncActive == true

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
    ): Flow<DittoQueryResult> = requireNotNull(getDitto()).store.observe(
        query = query,
        arguments = arguments
    )

    private suspend fun waitForWorkInProgress() {
        createJob?.join()
        closeJob?.join()
    }
}

/**
 * Defines how to create a Ditto Config in Multiplatform, and on each platform pass the required dependencies - for
 * example, on Android we require Context.
 */
internal expect fun createDitto(config: DittoConfig): Ditto
