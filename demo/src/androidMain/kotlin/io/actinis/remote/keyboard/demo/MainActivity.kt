package io.actinis.remote.keyboard.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.koin.compose.KoinContext


@Composable
fun AndroidAppView() {
    MaterialTheme {
        MainApp()
    }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            KoinContext {
                AndroidAppView()
            }
        }
    }
}