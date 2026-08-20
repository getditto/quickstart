package com.ditto.quickstart.ditto

import com.ditto.example.kotlin.quickstart.configuration.DittoSecretsConfiguration
import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoLog
import com.ditto.kotlin.DittoLogLevel
import com.ditto.kotlin.DittoLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "DittoManager"

/**
 * Manages the lifecycle and configuration of a Ditto instance: config, open, auth,
 * transport, and starting/stopping sync. It vends a configured, running [Ditto]
 * instance (via [getDitto]) for the tasks repository to use directly.
 *
 * This class deliberately does NOT wrap Ditto's APIs (store/sync/etc.). The
 * repository calls the real Ditto API — e.g. `dittoManager.getDitto()?.store?.execute(...)`
 * — so the quickstart shows how to use Ditto directly rather than modeling an
 * abstraction layer over it.
 */
class DittoManager(
    val secrets: DittoSecretsConfiguration,
) {
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob())
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
                    databaseId = secrets.DITTO_DATABASE_ID,
                    connect = DittoConfig.Connect.Server(
                        url = secrets.DITTO_SERVER_URL,
                    ),
                )

                createDitto(
                    config = config
                ).apply {
                    auth?.expirationHandler = { ditto, _ ->
                        // Authenticate when a token is expiring
                        val clientInfo = ditto.auth?.login(
                            token = secrets.DITTO_DEVELOPMENT_TOKEN,
                            provider = DittoAuthenticationProvider.development(),
                        )
                        DittoLog.d(TAG, "Auth response: $clientInfo")
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
            getDitto()?.sync?.stop()
            getDitto()?.close()
            ditto = null
        }
    }

    suspend fun startSync() {
        val ditto = getDitto() ?: return
        ditto.sync.start()
    }

    suspend fun stopSync() {
        getDitto()?.sync?.stop()
    }

    suspend fun isSyncing() = getDitto()?.sync?.isActive == true

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
