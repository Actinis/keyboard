package io.actinis.remote.keyboard.domain.input

import io.actinis.remote.keyboard.data.state.model.InputState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal interface InputStateInteractor {
    val inputState: StateFlow<InputState?>

    suspend fun updateInputState(inputState: InputState)

}

internal class InputStateInteractorImpl() : InputStateInteractor {

    override val inputState: MutableStateFlow<InputState?> = MutableStateFlow(null)

    override suspend fun updateInputState(inputState: InputState) {
        this.inputState.value = inputState
    }
}