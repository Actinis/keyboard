package io.actinis.remote.keyboard.demo

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Actinis Remote",
        state = rememberWindowState(
            width = with(LocalDensity.current) { 1080.toDp() },
            height = with(LocalDensity.current) { 1920.toDp() },
        ),
    ) {
//        KoinApplication(
//            application = koinConfiguration(),
//        ) {
//            MainApp()
//        }
        MainApp()
    }
}