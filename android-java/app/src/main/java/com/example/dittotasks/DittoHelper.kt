package com.example.dittotasks

import com.ditto.kotlin.Ditto
import com.ditto.kotlin.DittoAuthenticationProvider
import com.ditto.kotlin.DittoConfig
import com.ditto.kotlin.DittoException
import com.ditto.kotlin.DittoFactory
import com.ditto.kotlin.DittoQueryResult
import com.ditto.kotlin.DittoStoreObserver
import com.ditto.kotlin.DittoSyncSubscription
import kotlinx.coroutines.runBlocking
import java.util.function.Consumer

/**
 * Bridges Ditto v5 Kotlin SDK suspend functions for Java callers.
 */
object DittoHelper {

    @JvmStatic
    fun createDitto(appId: String, serverUrl: String): Ditto {
        val config = DittoConfig(
            databaseId = appId,
            connect = DittoConfig.Connect.Server(serverUrl)
        )
        return DittoFactory.create(config)
    }

    @JvmStatic
    fun setupAuth(ditto: Ditto, token: String) {
        ditto.auth?.let { auth ->
            auth.expirationHandler = { dittoInstance, _ ->
                dittoInstance.auth?.login(token, DittoAuthenticationProvider.development())
            }
        }
    }

    @JvmStatic
    @Throws(DittoException::class)
    fun execute(ditto: Ditto, query: String, args: Map<String, Any?>) {
        runBlocking {
            ditto.store.execute(query, args)
        }
    }

    @JvmStatic
    fun registerSubscription(ditto: Ditto, query: String): DittoSyncSubscription {
        return ditto.sync.registerSubscription(query)
    }

    @JvmStatic
    fun registerObserver(
        ditto: Ditto,
        query: String,
        callback: Consumer<DittoQueryResult>
    ): DittoStoreObserver {
        return ditto.store.registerObserver(query) { result ->
            callback.accept(result)
        }
    }

    @JvmStatic
    fun startSync(ditto: Ditto) {
        ditto.sync.start()
    }

    @JvmStatic
    fun stopSync(ditto: Ditto) {
        ditto.sync.stop()
    }

    @JvmStatic
    fun isSyncActive(ditto: Ditto): Boolean {
        return ditto.sync.isActive
    }
}
