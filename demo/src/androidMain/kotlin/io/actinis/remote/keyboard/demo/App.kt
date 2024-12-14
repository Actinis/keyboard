package io.actinis.remote.keyboard.demo

import android.app.Application
import co.touchlab.kermit.Logger

class App : Application() {

    private val logger = Logger.withTag(LOG_TAG)

    override fun onCreate() {
        super.onCreate()
    }

    private companion object {
        private const val LOG_TAG = "App"
    }
}