package io.actinis.remote.keyboard.di.module

import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.domain.input.InputStateInteractor
import io.actinis.remote.keyboard.domain.input.InputStateInteractorImpl
import io.actinis.remote.keyboard.domain.keyboard.*
import io.actinis.remote.keyboard.domain.text.TextModificationsInteractor
import io.actinis.remote.keyboard.domain.text.TextModificationsInteractorImpl
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
        InputStateInteractorImpl(
        )
    } bind InputStateInteractor::class

    single {
        KeyboardStateInteractorImpl(
            keyboardLayoutsRepository = get(),
            inputStateInteractor = get(),
            preferencesInteractor = get(),
            textAnalyzer = get(),
        )
    } bind KeyboardStateInteractor::class

    single {
        KeyboardOverlayInteractorImpl(
            preferencesInteractor = get(),
            keyboardStateInteractor = get(),
        )
    } bind KeyboardOverlayInteractor::class

    single {
        TextModificationsInteractorImpl(
            inputStateInteractor = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
        )
    } bind TextModificationsInteractor::class

    single {
        KeyboardInteractorImpl(
            preferencesInteractor = get(),
            keyboardStateInteractor = get(),
            inputStateInteractor = get(),
            keyboardOverlayInteractor = get(),
            textModificationsInteractor = get(),
            textSuggestionsInteractor = get(),
            defaultDispatcher = get(named(DispatchersNames.DEFAULT)),
            ioDispatcher = get(named(DispatchersNames.IO)),
        )
    } bind KeyboardInteractor::class
}