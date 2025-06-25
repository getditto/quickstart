package com.ditto.quickstart.data.repository

interface DataStorePathProvider {
    fun getDataStorePath(fileName: String): String
}
