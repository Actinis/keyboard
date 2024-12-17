package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.data.config.repository.KeyboardLayoutsRepository
import io.actinis.remote.keyboard.data.config.repository.KeyboardLayoutsRepositoryImpl
import io.actinis.remote.keyboard.di.name.DispatchersNames
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val configurationModule = module {

    single {
        KeyboardLayoutsRepositoryImpl(
            json = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
            ioDispatcher = get(named(DispatchersNames.IO)),
        )
    } bind KeyboardLayoutsRepository::class
}