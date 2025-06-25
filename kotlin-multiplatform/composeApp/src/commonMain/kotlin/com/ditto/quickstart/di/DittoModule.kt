package com.ditto.quickstart.di

import com.ditto.quickstart.ditto.DittoManager
import org.koin.dsl.module

fun dittoModule() = module {
    single {
        DittoManager()
    }
}
