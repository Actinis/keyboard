package io.actinis.remote.keyboard.demo

import io.actinis.remote.keyboard.di.keyboardModules
import io.actinis.remote.keyboard.domain.initialization.initializeKeyboard
import org.koin.core.context.startKoin

fun startDi() {
    val koinApp = startKoin {
        modules(*keyboardModules)
    }
    koinApp.initializeKeyboard()
}