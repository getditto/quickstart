package com.ditto.quickstart.di

fun koinModules() = listOf(
    dataStorePathProviderModule(),
    dittoModule(),
    repositoryModule(),
    useCaseModule(),
    viewModelModule(),
)
