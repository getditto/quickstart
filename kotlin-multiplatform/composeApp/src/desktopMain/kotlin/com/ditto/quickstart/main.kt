package com.ditto.quickstart

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ditto.quickstart.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Quickstart",
    ) {
        App()
    }
}
