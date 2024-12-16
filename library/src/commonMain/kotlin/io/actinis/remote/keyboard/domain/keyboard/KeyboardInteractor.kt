package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.presentation.model.KeyboardOverlayBubble
import io.actinis.remote.keyboard.presentation.model.KeyboardOverlayState
import io.actinis.remote.keyboard.presentation.touch.KeyInteractionEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine


internal interface KeyboardInteractor {
    val keyboardState: StateFlow<KeyboardState>
    val currentLayout: StateFlow<KeyboardLayout?>
    val keyboardEvents: Flow<KeyboardEvent>
    val overlayState: StateFlow<KeyboardOverlayState>

    fun initialize(inputType: InputType, isPassword: Boolean)
    fun handlePressedKey(key: Key)
    fun handleKeysReleased()

    fun handleMovementInLongPressMode(deltaX: Float, deltaY: Float)
}

internal class KeyboardInteractorImpl(
    private val keyboardStateInteractor: KeyboardStateInteractor,
    private val keyboardOverlayInteractor: KeyboardOverlayInteractor,
    private val defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) : KeyboardInteractor {

    private val logger = Logger.withTag(LOG_TAG)

    override val keyboardState: StateFlow<KeyboardState> = keyboardStateInteractor.keyboardState
    override val currentLayout: StateFlow<KeyboardLayout?> = keyboardStateInteractor.currentLayout
    override val overlayState: StateFlow<KeyboardOverlayState> = keyboardOverlayInteractor.overlayState

    override val keyboardEvents: MutableSharedFlow<KeyboardEvent> = MutableSharedFlow()

    private val keyInteractionEvents = MutableSharedFlow<KeyInteractionEvent>(extraBufferCapacity = 8)

    private val coroutineScope = CoroutineScope(defaultDispatcher + SupervisorJob())

    private val touchTrackedKeys = mutableSetOf<String>()
    private val keyTimingJobs = mutableMapOf<String, Job>()
    private val lastKeyUpTimestamps = mutableMapOf<String, Long>()

    init {
        coroutineScope.launch(defaultDispatcher) {
            keyInteractionEvents.collect(::handleKeyInteractionEvent)
        }

        coroutineScope.launch(defaultDispatcher) {
            combine(
                keyboardState,
                currentLayout,
            ) { state, layout ->
                keyboardOverlayInteractor.updateOverlayState(state, layout)
            }.collect {}
        }
    }

    override fun initialize(inputType: InputType, isPassword: Boolean) {
        logger.d { "initialize: inputType=$inputType, isPassword=$isPassword" }

        coroutineScope.launch {
            keyboardStateInteractor.initialize(
                inputType = inputType,
                isPassword = isPassword,
            )
        }
    }

    override fun handlePressedKey(key: Key) {
        logger.d { "handlePressedKey: key=${key.id}" }

        coroutineScope.launch {
            removeOldPressedKey(currentPressedKey = key)

            addTrackedKey(key)
            keyboardStateInteractor.setPressedKey(key.id)
        }
    }

    private suspend fun addTrackedKey(key: Key) {
        if (!touchTrackedKeys.contains(key.id)) {
            touchTrackedKeys.add(key.id)
            emitKeyInteractionEvent(KeyInteractionEvent.Down(key))
            startKeyTimingJobs(key)
        }
    }

    private suspend fun removeOldPressedKey(currentPressedKey: Key) {
        keyboardStateInteractor
            .keyboardState
            .value
            .pressedKeyId
            ?.takeIf { it != currentPressedKey.id }
            ?.let { pressedKeyId ->
                logger.d { "Removing pressed key = $pressedKeyId" }

                val key = currentLayout.value?.findKey { it.id == pressedKeyId }
                if (key != null) {
                    handleKeyReleased(key)
                } else {
                    logger.w { "Failed to find key with id=$pressedKeyId in current layout while removing inactive keys" }
                }

                keyboardStateInteractor.removePressedKey()
            }
    }

    override fun handleKeysReleased() {
        logger.d { "handleKeysReleased" }

        val longPressedSelectedActionId = getLongPressedSelectedActionId()

        coroutineScope.launch {
            handleTrackedKeysReleased(keyActionId = longPressedSelectedActionId)
            keyboardStateInteractor.removePressedKey()
        }

        keyboardOverlayInteractor.reset()
    }

    // FIXME: It should return something like an universal Action instead
    private fun getLongPressedSelectedActionId(): String? {
        val overlayActiveBubble = overlayState.value.activeBubble as? KeyboardOverlayBubble.LongPressedKey
            ?: return null

        return overlayActiveBubble.items[overlayActiveBubble.selectedItemRow][overlayActiveBubble.selectedItemColumn].id
    }

    private suspend fun handleTrackedKeysReleased(keyActionId: String?) {
        touchTrackedKeys.toList().forEach { keyId ->
            keyId
                .takeIf {
                    keyboardStateInteractor.isKeyPressed(keyId = keyId)
                }
                ?.let {
                    currentLayout.value?.findKey { it.id == keyId }
                }
                ?.let { key ->
                    handleKeyReleased(
                        key = key,
                        actionId = keyActionId,
                    )
                }
        }
    }

    private suspend fun handleKeyReleased(key: Key, actionId: String? = null) {
        if (touchTrackedKeys.remove(key.id)) {
            cancelKeyTimingJobs(key.id)

            val isDoubleTap = if (key.actions.doubleTap != null) {
                handlePotentialDoubleTap(key)
            } else {
                false
            }

            if (!isDoubleTap) {
                emitKeyInteractionEvent(
                    KeyInteractionEvent.Up(
                        key = key,
                        actionId = actionId,
                    )
                )
            }
        }
    }

    private suspend fun handleKeyUpAction(key: Key, actionId: String? = null) {
        logger.d { "handleKeyPressAction: key=${key.id}, actionId=$actionId" }

        // FIXME: Instead of actionId, use some Action class
        when {
            key.actions.press.output != null -> {
                if (actionId != null) {
                    handleKeyPressOutputAction(key = key, output = actionId) // FIXME: text instead of actionId
                } else {
                    handleKeyPressOutputAction(key = key, output = key.actions.press.output)
                }
            }

            key.actions.press.command != null -> {
                handleKeyCommandAction(key = key, command = key.actions.press.command)
            }

            else -> {
                logger.d { "No key press action for key=${key}" }
            }
        }
    }

    private suspend fun handleKeyPressOutputAction(key: Key, output: String) {
        logger.d { "handleKeyPressOutputAction: key=${key.id}" }

        val text = when {
            keyboardStateInteractor.isShiftActive -> {
                output.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            keyboardStateInteractor.isCapsLockActive -> output.uppercase()

            else -> output
        }

        emitKeyboardEvent(KeyboardEvent.TextInput(text))

        // Turn off shift (if active) after character input
        keyboardStateInteractor.turnOffShift()
    }

    private suspend fun handleKeyCommandAction(key: Key, command: Actions.Command) {
        logger.d { "handleKeyCommandAction: key=${key}, keyCommand=$command" }

        when (command) {
            Actions.Command.DELETE_BACKWARD -> emitKeyboardEvent(KeyboardEvent.Backspace)
            Actions.Command.ACTION -> emitKeyboardEvent(KeyboardEvent.ActionClick)
            Actions.Command.SWITCH_LAYOUT -> {
                val layoutId = key.actions.press.params["layout"] ?: return
                keyboardStateInteractor.switchLayout(layoutId)
            }

            Actions.Command.TOGGLE_SHIFT -> keyboardStateInteractor.toggleShift()
            Actions.Command.CAPS_LOCK -> keyboardStateInteractor.toggleCapsLock()
            Actions.Command.SHOW_LAYOUTS -> TODO()
        }
    }

    private suspend fun handlePotentialDoubleTap(key: Key): Boolean {
        val now = System.nanoTime()
        val lastTap = lastKeyUpTimestamps[key.id]

        if (lastTap != null) {
            val maxDelay = keyboardStateInteractor.currentLayout.value?.behaviors?.doubleTap?.maxDelay ?: return false
            val deltaMs = (now - lastTap) / 1_000_000

            if (deltaMs <= maxDelay) {
                emitKeyInteractionEvent(KeyInteractionEvent.DoubleTap(key))
                lastKeyUpTimestamps.remove(key.id)
                return true
            }
        }

        lastKeyUpTimestamps[key.id] = now

        return false
    }


    private suspend fun emitKeyboardEvent(event: KeyboardEvent) {
//        logger.d { "emitKeyboardEvent: event=$event" }

        keyboardEvents.emit(event)
    }

    private fun startKeyTimingJobs(key: Key) {
        cancelKeyTimingJobs(key.id)

        val requiresTimingJobs = currentLayout.value?.behaviors?.longPress != null || key.actions.repeat

        if (requiresTimingJobs) {
            keyTimingJobs[key.id] = coroutineScope.launch(defaultDispatcher) {
                // Handle long press
                currentLayout.value?.behaviors?.longPress?.delay?.let { delay ->
                    launch {
                        delay(delay)
                        if (keyboardStateInteractor.isKeyPressed(key.id)) {
                            emitKeyInteractionEvent(KeyInteractionEvent.LongPress(key))
                        }
                    }
                }

                // Handle key repeat if enabled
                if (key.actions.repeat) {
                    currentLayout.value?.behaviors?.keyRepeat?.let { repeat ->
                        launch {
                            delay(repeat.initialDelay)
                            while (keyboardStateInteractor.isKeyPressed(key.id)) {
                                emitKeyInteractionEvent(KeyInteractionEvent.Repeat(key))
                                delay(repeat.repeatInterval)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun cancelKeyTimingJobs(keyId: String) {
        keyTimingJobs.remove(keyId)?.cancel()
    }

    private suspend fun emitKeyInteractionEvent(event: KeyInteractionEvent) {
        keyInteractionEvents.emit(event)
    }

    private suspend fun handleKeyInteractionEvent(event: KeyInteractionEvent) {
        logger.d { "handleKeyInteractionEvent: $event" }

        val key = event.key

        when (event) {
            is KeyInteractionEvent.Down -> handleKeyDown(key = key)
            is KeyInteractionEvent.Up -> handleKeyUp(key = key, actionId = event.actionId)
            is KeyInteractionEvent.DoubleTap -> handleKeyDoubleTap(key = key)
            is KeyInteractionEvent.LongPress -> handleKeyLongPress(key = key)
            is KeyInteractionEvent.Repeat -> handleKeyRepeat(key = key)
        }
    }

    private suspend fun handleKeyDown(key: Key) {
        logger.d { "handleKeyDown: $key" }
    }

    private suspend fun handleKeyUp(key: Key, actionId: String?) {
        logger.d { "handleKeyUp: $key" }

        handleKeyUpAction(
            key = key,
            actionId = actionId,
        )
    }

    private suspend fun handleKeyDoubleTap(key: Key) {
        logger.d { "handleKeyDoubleTap: $key" }

        val doubleTapCommand = key.actions.doubleTap?.command
        if (doubleTapCommand == null) {
            handleKeyUpAction(key = key)
        } else {
            handleKeyCommandAction(key = key, command = doubleTapCommand)
        }
    }

    private fun handleKeyLongPress(key: Key) {
        logger.d { "handleKeyLongPress: $key" }

        keyboardStateInteractor.setLongPressedKey(key.id)
    }

    private suspend fun handleKeyRepeat(key: Key) {
        logger.d { "handleKeyRepeat: $key" }

        handleKeyUpAction(key = key)
    }

    override fun handleMovementInLongPressMode(deltaX: Float, deltaY: Float) {
        keyboardOverlayInteractor.updateSelection(deltaX = deltaX, deltaY = deltaY)
    }

    private companion object {
        private const val LOG_TAG = "KeyboardInteractor"
    }
}