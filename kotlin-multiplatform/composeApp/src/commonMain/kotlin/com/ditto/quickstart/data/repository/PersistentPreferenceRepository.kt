package com.ditto.quickstart.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.firstOrNull
import okio.Path.Companion.toPath

private val syncPreferencesKey = booleanPreferencesKey("sync")


class PersistentPreferenceRepository(
    private val dataStorePathProvider: DataStorePathProvider
) : PreferenceRepository {
    private val dataStore: DataStore<Preferences> by lazy {
        createDataStore(dataStorePathProvider = dataStorePathProvider)
    }

    override suspend fun setSync(boolean: Boolean) {
        dataStore.edit {
            it[syncPreferencesKey] = boolean
        }
    }

    override suspend fun getSync(): Boolean {
        // Default to true for sync enabled (better for testing and user experience)
        return dataStore.data.firstOrNull()?.get(syncPreferencesKey) ?: true
    }

    private fun createDataStore(
        dataStorePathProvider: DataStorePathProvider
    ) = PreferenceDataStoreFactory.createWithPath(
        produceFile = {
            dataStorePathProvider.getDataStorePath("tasks.preferences_pb").toPath()
        }
    )
}
