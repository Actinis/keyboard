package io.actinis.remote.keyboard.presentation

import androidx.lifecycle.ViewModel
import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.domain.keyboard.KeyboardInteractor
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

abstract class KeyboardViewModel : ViewModel() {
    abstract val keyboardEvents: Flow<KeyboardEvent>

    abstract val currentLayout: StateFlow<KeyboardLayout?>
    abstract val keyboardState: StateFlow<KeyboardState>
    abstract val overlayState: StateFlow<KeyboardOverlayState>

    abstract fun initialize(inputType: InputType, isPassword: Boolean)
    abstract fun handleActiveKey(key: Key)
    abstract fun handleKeysReleased()

    abstract fun handleMovementInLongPressMode(deltaX: Float, deltaY: Float)
}


internal class KeyboardViewModelImpl(
    private val keyboardInteractor: KeyboardInteractor,
) : KeyboardViewModel() {

    private val logger = Logger.withTag(LOG_TAG)

    override val keyboardEvents: Flow<KeyboardEvent> = keyboardInteractor.keyboardEvents
    override val currentLayout: StateFlow<KeyboardLayout?> = keyboardInteractor.currentLayout
    override val keyboardState: StateFlow<KeyboardState> = keyboardInteractor.keyboardState
    override val overlayState: StateFlow<KeyboardOverlayState> = keyboardInteractor.overlayState

    override fun initialize(inputType: InputType, isPassword: Boolean) {
        keyboardInteractor.initialize(
            inputType = inputType,
            isPassword = isPassword,
        )
    }

    override fun handleActiveKey(key: Key) {
        keyboardInteractor.handlePressedKey(key)
    }

    override fun handleKeysReleased() {
        keyboardInteractor.handleKeysReleased()
    }

    override fun handleMovementInLongPressMode(deltaX: Float, deltaY: Float) {
        keyboardInteractor.handleMovementInLongPressMode(deltaX = deltaX, deltaY = deltaY)
    }


    private companion object {
        private const val LOG_TAG = "KeyboardViewModel"
    }
}