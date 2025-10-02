package com.ditto.quickstart.di

import com.ditto.quickstart.data.DittoCredentials
import com.ditto.quickstart.ditto.DittoManager
import org.koin.dsl.module

fun dittoModule() = module {
    single {
        DittoCredentials(
            appId = "da244a14-bb3d-435d-92b0-b4f667a1b004",
            appToken = "161848a6-8a68-48d7-8b44-ecdf46648ca6"
        )
    }
    single {
        DittoManager(
            credentials = get(),
        )
    }
}
