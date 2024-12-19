package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.domain.keyboard.*
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
            keyboardLayoutsRepository = get(),
            preferencesInteractor = get(),
        )
    } bind KeyboardStateInteractor::class

    single {
        KeyboardOverlayInteractorImpl(
            preferencesInteractor = get(),
        )
    } bind KeyboardOverlayInteractor::class

    single {
        KeyboardInteractorImpl(
            preferencesInteractor = get(),
            keyboardStateInteractor = get(),
            keyboardOverlayInteractor = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
            ioDispatcher = get(named(DispatchersNames.IO)),
        )
    } bind KeyboardInteractor::class
}