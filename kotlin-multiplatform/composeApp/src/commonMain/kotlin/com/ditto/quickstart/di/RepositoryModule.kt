package com.ditto.quickstart.di

import com.ditto.quickstart.data.repository.PersistentPreferenceRepository
import com.ditto.quickstart.data.repository.PreferenceRepository
import com.ditto.quickstart.data.repository.TaskRepository
import com.ditto.quickstart.data.repository.TasksRepository
import org.koin.dsl.module

fun repositoryModule() = module {
    single<TaskRepository> {
        TasksRepository(get())
    }

    single<PreferenceRepository> {
        PersistentPreferenceRepository(dataStorePathProvider = get())
    }
}
