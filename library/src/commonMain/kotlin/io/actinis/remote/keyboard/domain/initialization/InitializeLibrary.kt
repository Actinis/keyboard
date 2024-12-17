package io.actinis.remote.keyboard.domain.initialization

import io.actinis.remote.keyboard.data.config.repository.KeyboardLayoutsRepository
import io.actinis.remote.keyboard.domain.preferences.PreferencesInteractor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface InitializeLibrary {
    fun execute()
}

internal class InitializeLibraryImpl(
    private val keyboardLayoutsRepository: KeyboardLayoutsRepository,
    private val preferencesInteractor: PreferencesInteractor,
    private val defaultDispatcher: CoroutineDispatcher,
) : InitializeLibrary {

    override fun execute() {
        CoroutineScope(defaultDispatcher).launch {
            keyboardLayoutsRepository.initialize()
            preferencesInteractor.initialize()
        }
    }
}

