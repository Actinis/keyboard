package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.data.config.repository.ConfigurationRepository
import io.actinis.remote.keyboard.data.config.repository.ConfigurationRepositoryImpl
import io.actinis.remote.keyboard.di.name.DispatchersNames
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val configurationModule = module {

    single {
        ConfigurationRepositoryImpl(
            json = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
            ioDispatcher = get(named(DispatchersNames.IO)),
        )
    } bind ConfigurationRepository::class
}