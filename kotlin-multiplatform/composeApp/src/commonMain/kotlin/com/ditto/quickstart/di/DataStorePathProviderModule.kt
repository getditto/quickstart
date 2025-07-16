package com.ditto.quickstart.di

import org.koin.core.module.Module

/**
 * Actual implementation MUST provide [com.ditto.quickstart.data.repository.DataStorePathProvider]
 */
expect fun dataStorePathProviderModule(): Module
