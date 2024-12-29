package io.actinis.remote.keyboard.demo

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    MaterialTheme {
        MainApp()
    }
}