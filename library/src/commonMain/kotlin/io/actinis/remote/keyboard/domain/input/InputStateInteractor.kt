package io.actinis.remote.keyboard.domain.input

import io.actinis.remote.keyboard.data.state.model.InputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface InputStateInteractor {
    val inputState: StateFlow<InputState?>
    val language: StateFlow<String?>

    suspend fun updateInputState(inputState: InputState)
    suspend fun updateLanguage(language: String?)
}

internal class InputStateInteractorImpl : InputStateInteractor {

    override val inputState: MutableStateFlow<InputState?> = MutableStateFlow(null)
    override val language: MutableStateFlow<String?> = MutableStateFlow(null)

    override suspend fun updateInputState(inputState: InputState) {
        this.inputState.value = inputState
    }

    override suspend fun updateLanguage(language: String?) {
        this.language.value = language
    }
}