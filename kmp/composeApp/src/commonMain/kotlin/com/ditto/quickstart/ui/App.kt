package com.ditto.quickstart.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.ditto.quickstart.di.koinModules
import org.koin.compose.KoinApplication
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    modifier: Modifier = Modifier
) {
    KoinApplication(
        application = { modules(koinModules()) }
    ) {
        val appViewModel = koinViewModel<AppViewModel>()

        LaunchedEffect(key1 = appViewModel) {
            appViewModel.onStartApp()
        }

        MainScreen(modifier = modifier)
    }
}
