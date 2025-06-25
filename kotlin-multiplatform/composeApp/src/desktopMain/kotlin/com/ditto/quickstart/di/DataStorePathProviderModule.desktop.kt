package com.ditto.quickstart.di

import com.ditto.quickstart.data.repository.DataStorePathProvider
import org.koin.dsl.module
import java.io.File

class DesktopStorePathProvider() : DataStorePathProvider {
    override fun getDataStorePath(fileName: String): String {
        return File(fileName).absolutePath
    }
}

actual fun dataStorePathProviderModule() = module {
    single<DataStorePathProvider> {
        DesktopStorePathProvider()
    }
}
