package com.ditto.quickstart.di

import android.content.Context
import com.ditto.quickstart.App
import com.ditto.quickstart.data.repository.DataStorePathProvider
import org.koin.dsl.module

class AndroidDataStorePathProvider(private val context: Context) : DataStorePathProvider {
    override fun getDataStorePath(fileName: String): String {
        return context.filesDir.resolve(fileName).absolutePath
    }
}

actual fun dataStorePathProviderModule() = module {
    single<DataStorePathProvider> {
        AndroidDataStorePathProvider(context = App.instance)
    }
}
