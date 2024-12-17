package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.domain.debug.IsDebug
import io.actinis.remote.keyboard.domain.debug.IsDebugImpl
import io.actinis.remote.keyboard.domain.initialization.InitializeLibrary
import io.actinis.remote.keyboard.domain.initialization.InitializeLibraryImpl
import kotlinx.serialization.json.Json
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val coreModule = module {
    single { IsDebugImpl() } bind IsDebug::class

    single {
        val isDebug: IsDebug = get()

        Json {
            prettyPrint = isDebug.execute()
            ignoreUnknownKeys = true
            isLenient = false
        }
    }

    factory {
        InitializeLibraryImpl(
            keyboardLayoutsRepository = get(),
            preferencesInteractor = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
        )
    } bind InitializeLibrary::class
}