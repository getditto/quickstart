package com.ditto.quickstart.di

import com.ditto.example.kotlin.quickstart.configuration.DittoSecretsConfiguration
import com.ditto.quickstart.ditto.DittoManager
import org.koin.dsl.module

fun dittoModule() = module {
    single {
        DittoSecretsConfiguration
    }
    single {
        DittoManager(
            secrets = get(),
        )
    }
}
