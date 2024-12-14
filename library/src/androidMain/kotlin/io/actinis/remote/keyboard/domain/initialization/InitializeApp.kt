package io.actinis.remote.keyboard.domain.initialization

import android.content.Context
import androidx.startup.Initializer
import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.di.configureModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.GlobalContext.startKoin

private const val LOG_TAG = "InitializeApp"

private val logger = Logger.withTag(LOG_TAG)

// Referred in target's AndroidManifest
@Suppress("unused")
class AndroidLibraryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        initializeAndroidLibrary(context = context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}

fun initializeAndroidLibrary(context: Context) {
    logger.i { "Initializing Android part of library" }

    val diApp = startDi(context = context)

    val initializeLibrary: InitializeLibrary = diApp.koin.get()
    initializeLibrary.execute()

    logger.i { "Initialized Android part" }
}

private fun startDi(context: Context): KoinApplication {
    logger.i { "Starting DI" }

    return startKoin {
        androidContext(context)

        configureModules()
    }.also {
        logger.i { "Started DI" }
    }
}