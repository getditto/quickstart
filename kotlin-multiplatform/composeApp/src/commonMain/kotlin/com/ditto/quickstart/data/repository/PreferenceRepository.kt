package com.ditto.quickstart.data.repository

interface PreferenceRepository {
    suspend fun setSync(boolean: Boolean)
    suspend fun getSync(): Boolean
}
