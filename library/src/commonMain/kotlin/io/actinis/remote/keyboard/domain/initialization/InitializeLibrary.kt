package io.actinis.remote.keyboard.domain.initialization

import io.actinis.remote.keyboard.data.config.repository.ConfigurationRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

interface InitializeLibrary {
    fun execute()
}

internal class InitializeLibraryImpl(
    private val configurationRepository: ConfigurationRepository,
    private val defaultDispatcher: CoroutineDispatcher,
) : InitializeLibrary {

    override fun execute() {
        CoroutineScope(defaultDispatcher).launch {
            configurationRepository.initialize()
        }
    }
}

