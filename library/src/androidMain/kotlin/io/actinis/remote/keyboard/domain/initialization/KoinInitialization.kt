package io.actinis.remote.keyboard.domain.initialization

import org.koin.core.KoinApplication

/**
 * Use this method to initialize library if your app is already using Koin
 */
fun KoinApplication.initializeKeyboard() {
    val initializeLibrary: InitializeLibrary = koin.get()
    initializeLibrary.execute()
}