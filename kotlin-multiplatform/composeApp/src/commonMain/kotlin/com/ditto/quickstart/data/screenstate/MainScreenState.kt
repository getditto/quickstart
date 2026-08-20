package com.ditto.quickstart.data.screenstate

data class MainScreenState(
    val databaseId: String,
    val appToken: String,
    val isLoading: Boolean,
    val isSyncEnabled: Boolean,
    val errorMessage: String?,
) {
    companion object {
        fun initial(): MainScreenState = MainScreenState(
            databaseId = "",
            appToken = "",
            isLoading = true,
            isSyncEnabled = true,
            errorMessage = null,
        )
    }
}
