package com.ditto.quickstart.data.screenstate

data class MainScreenState(
    val appId: String,
    val appToken: String,
    val isLoading: Boolean,
    val isSyncEnabled: Boolean,
    val errorMessage: String?,
) {
    companion object {
        fun initial(): MainScreenState = MainScreenState(
            appId = "",
            appToken = "",
            isLoading = true,
            isSyncEnabled = false,
            errorMessage = null,
        )
    }
}
