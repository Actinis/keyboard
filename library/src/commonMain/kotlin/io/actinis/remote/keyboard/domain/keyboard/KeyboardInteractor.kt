package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.domain.model.command.KeyboardCommand
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayBubble
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayState
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

        val currentLongPressedItem = keyboardOverlayInteractor.getCurrentLongPressItem()

        coroutineScope.launch {
            handleTrackedKeysReleased(currentLongPressedItem = currentLongPressedItem)
            keyboardStateInteractor.removePressedKey()
        }

        keyboardOverlayInteractor.reset()
    }

    private suspend fun handleTrackedKeysReleased(
        currentLongPressedItem: KeyboardOverlayBubble.LongPressedKey.Item?,
    ) {
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
                        currentLongPressedItem = currentLongPressedItem,
                    )
                }
        }
    }

    private suspend fun handleKeyReleased(
        key: Key,
        currentLongPressedItem: KeyboardOverlayBubble.LongPressedKey.Item? = null,
    ) {
        if (touchTrackedKeys.remove(key.id)) {
            cancelKeyTimingJobs(key.id)

            val isDoubleTap = if (key.actions.doubleTap != null) {
                handlePotentialDoubleTap(key)
            } else {
                false
            }

            if (!isDoubleTap) {
                val command = if (currentLongPressedItem != null) {
                    createCommand(
                        action = key.actions.longPress,
                        currentLongPressedItem.id
                    ) // TODO: Use text instead of ID
                } else {
                    createCommand(
                        action = key.actions.press,
                    )
                }

                if (command == null) {
                    logger.e {
                        "Failed to determine release command for $key: currentLongPressedItem=$currentLongPressedItem"
                    }
                    return
                }

                emitKeyInteractionEvent(
                    KeyInteractionEvent.Up(
                        key = key,
                        command = command,
                    )
                )
            }
        }
    }

    private suspend fun handleKeyUpAction(key: Key, command: KeyboardCommand) {
        logger.d { "handleKeyPressAction: key=${key.id}, command=$command" }

        handleKeyCommandAction(key = key, command = command)
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

    private suspend fun handleKeyCommandAction(
        key: Key,
        command: KeyboardCommand,
    ) {
        logger.d { "handleKeyCommandAction: key=${key}, keyCommand=$command" }

        when (command) {
            KeyboardCommand.Action -> emitKeyboardEvent(KeyboardEvent.ActionClick)
            KeyboardCommand.DeleteBackward -> emitKeyboardEvent(KeyboardEvent.Backspace)
            KeyboardCommand.DeleteWord -> emitKeyboardEvent(KeyboardEvent.DeleteWord)
            is KeyboardCommand.OutputValue -> {
                handleKeyPressOutputAction(key = key, output = command.value)
            }

            is KeyboardCommand.SwitchLayout -> keyboardStateInteractor.switchLayout(command.layoutId)
            KeyboardCommand.ToggleCapsLock -> keyboardStateInteractor.toggleCapsLock()
            KeyboardCommand.ToggleShift -> keyboardStateInteractor.toggleShift()
            KeyboardCommand.ShowCursorControls -> TODO()
            KeyboardCommand.ShowLayouts -> TODO()
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
            is KeyInteractionEvent.Up -> handleKeyUp(key = key, command = event.command)
            is KeyInteractionEvent.DoubleTap -> handleKeyDoubleTap(key = key)
            is KeyInteractionEvent.LongPress -> handleKeyLongPress(key = key)
            is KeyInteractionEvent.Repeat -> handleKeyRepeat(key = key)
        }
    }

    private suspend fun handleKeyDown(key: Key) {
        logger.d { "handleKeyDown: $key" }
    }

    private suspend fun handleKeyUp(key: Key, command: KeyboardCommand) {
        logger.d { "handleKeyUp: $key" }

        handleKeyUpAction(
            key = key,
            command = command,
        )
    }

    private suspend fun handleKeyDoubleTap(key: Key) {
        logger.d { "handleKeyDoubleTap: $key" }

        val doubleTapCommand = createCommand(key.actions.doubleTap)
        if (doubleTapCommand == null) {
            val tapCommand = createCommand(key.actions.press)
            if (tapCommand == null) {
                logger.e { "Failed to create both double tap and tap commands for $key" }
                return
            }
            handleKeyUpAction(key = key, command = tapCommand)
        } else {
            handleKeyCommandAction(key = key, command = doubleTapCommand)
        }
    }

    // TODO: Move somewhere
    private fun createCommand(
        action: Actions.Action?,
        vararg commandArgs: Any,
    ): KeyboardCommand? {
        if (action == null) return null

        return when (action.command) {
            null -> null
            Actions.Action.CommandType.DELETE_BACKWARD -> KeyboardCommand.DeleteBackward
            Actions.Action.CommandType.DELETE_WORD -> KeyboardCommand.DeleteWord
            Actions.Action.CommandType.ACTION -> KeyboardCommand.Action
            Actions.Action.CommandType.SWITCH_LAYOUT -> {
                val layoutId = action.params[Actions.Action.ParameterType.LAYOUT]
                if (layoutId == null) {
                    logger.e { "Tried to create SWITCH_LAYOUT command, but no layoutId in action: $action" }
                    return null
                }
                KeyboardCommand.SwitchLayout(layoutId)
            }

            Actions.Action.CommandType.SHOW_LAYOUTS -> KeyboardCommand.ShowLayouts
            Actions.Action.CommandType.TOGGLE_SHIFT -> KeyboardCommand.ToggleShift
            Actions.Action.CommandType.CAPS_LOCK -> KeyboardCommand.ToggleCapsLock
            Actions.Action.CommandType.SHOW_CURSOR_CONTROLS -> KeyboardCommand.ShowCursorControls
            Actions.Action.CommandType.OUTPUT_VALUE -> {
                var value = commandArgs.firstOrNull() as? String?
                if (value == null) {
                    value = action.params[Actions.Action.ParameterType.VALUE]

                    if (value == null) {
                        logger.e { "Tried to create OUTPUT_VALUE command, but no value given for action=$action" }
                        return null
                    }
                }

                KeyboardCommand.OutputValue(value)
            }
        }
    }

    private fun handleKeyLongPress(key: Key) {
        logger.d { "handleKeyLongPress: $key" }

        keyboardStateInteractor.setLongPressedKey(key.id)
    }

    private suspend fun handleKeyRepeat(key: Key) {
        logger.d { "handleKeyRepeat: $key" }

        val command = createCommand(action = key.actions.press)
        if (command == null) {
            logger.e { "Failed to determine repeat command for $key" }
            return
        }

        handleKeyUpAction(
            key = key,
            command = command
        )
    }

    override fun handleMovementInLongPressMode(deltaX: Float, deltaY: Float) {
        keyboardOverlayInteractor.updateSelection(deltaX = deltaX, deltaY = deltaY)
    }

    private companion object {
        private const val LOG_TAG = "KeyboardInteractor"
    }
}