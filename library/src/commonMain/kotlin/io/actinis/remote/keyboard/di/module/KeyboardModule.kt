package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.domain.keyboard.KeyboardInteractor
import io.actinis.remote.keyboard.domain.keyboard.KeyboardInteractorImpl
import io.actinis.remote.keyboard.domain.keyboard.KeyboardStateInteractor
import io.actinis.remote.keyboard.domain.keyboard.KeyboardStateInteractorImpl
import io.actinis.remote.keyboard.presentation.KeyboardViewModel
import io.actinis.remote.keyboard.presentation.KeyboardViewModelImpl
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

internal val keyboardModule = module {

    viewModel {
        KeyboardViewModelImpl(
            keyboardInteractor = get(),
        )
    } bind KeyboardViewModel::class

    single {
        KeyboardStateInteractorImpl(
            configurationRepository = get(),
        )
    } bind KeyboardStateInteractor::class

    single {
        KeyboardInteractorImpl(
            keyboardStateInteractor = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
            ioDispatcher = get(named(DispatchersNames.IO)),
        )
    } bind KeyboardInteractor::class
}