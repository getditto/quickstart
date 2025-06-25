package com.ditto.quickstart.di

import kotlinx.cinterop.ExperimentalForeignApi
import com.ditto.quickstart.data.repository.DataStorePathProvider
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import kotlin.uuid.ExperimentalUuidApi

class IosDataStorePathProvider() : DataStorePathProvider {
    @OptIn(ExperimentalForeignApi::class, ExperimentalUuidApi::class)
    override fun getDataStorePath(fileName: String): String {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )
        return directory
            ?.URLByAppendingPathComponent(fileName)
            ?.path ?: error("Failed to get path")
    }
}

actual fun dataStorePathProviderModule() = module {
    single<DataStorePathProvider> {
        IosDataStorePathProvider()
    }
}
