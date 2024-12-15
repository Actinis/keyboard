package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.presentation.touch.KeyInteractionEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow


internal interface KeyboardInteractor {
    val keyboardState: StateFlow<KeyboardState>
    val currentLayout: StateFlow<KeyboardLayout?>
    val keyboardEvents: Flow<KeyboardEvent>

    fun initialize(inputType: InputType, isPassword: Boolean)
    fun handleActiveKey(key: Key)
    fun handleKeysReleased()
}

internal class KeyboardInteractorImpl(
    private val keyboardStateInteractor: KeyboardStateInteractor,
    private val defaultDispatcher: CoroutineDispatcher,
    private val ioDispatcher: CoroutineDispatcher,
) : KeyboardInteractor {

    private val logger = Logger.withTag(LOG_TAG)

    override val keyboardState: StateFlow<KeyboardState>
        get() = keyboardStateInteractor.keyboardState

    override val currentLayout: StateFlow<KeyboardLayout?>
        get() = keyboardStateInteractor.currentLayout

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

    override fun handleActiveKey(key: Key) {
        logger.d { "handleActiveKey: key=${key.id}" }

        coroutineScope.launch {
            removeInactiveKeys(activeKey = key)

            addTrackedKey(key)
            keyboardStateInteractor.addPressedKey(key.id)
        }
    }

    private suspend fun addTrackedKey(key: Key) {
        if (!touchTrackedKeys.contains(key.id)) {
            touchTrackedKeys.add(key.id)
            emitKeyInteractionEvent(KeyInteractionEvent.Down(key))
            startKeyTimingJobs(key)
        }
    }

    private suspend fun removeInactiveKeys(activeKey: Key) {
        keyboardStateInteractor
            .getPressedKeysIdsExcept(activeKey.id)
            .map { keyId ->
                coroutineScope.async {
                    val key = currentLayout.value?.findKey { it.id == keyId }
                    if (key == null) {
                        logger.w { "Failed to find key with id=$keyId in current layout while removing inactive keys" }
                        return@async null
                    }
                    handleKeyReleased(key)
                    keyId
                }
            }
            .toSet()
            .awaitAll()
            .filterNotNull()
            .let { keys -> keyboardStateInteractor.removePressedKeys(keys) }
    }

    override fun handleKeysReleased() {
        logger.d { "handleKeysReleased" }

        coroutineScope.launch {
            handleTrackedKeysReleased()
            keyboardStateInteractor.removePressedKeys()
        }
    }

    private suspend fun handleTrackedKeysReleased() {
        touchTrackedKeys.toList().forEach { keyId ->
            keyId
                .takeIf {
                    keyboardStateInteractor.isKeyPressed(keyId = keyId)
                }
                ?.let {
                    currentLayout.value?.findKey { it.id == keyId }
                }
                ?.let { key ->
                    handleKeyReleased(key)
                }
        }
    }

    private suspend fun handleKeyReleased(key: Key) {
        if (touchTrackedKeys.remove(key.id)) {
            cancelKeyTimingJobs(key.id)

            val isDoubleTap = if (key.actions.doubleTap != null) {
                handlePotentialDoubleTap(key)
            } else {
                false
            }

            if (!isDoubleTap) {
                emitKeyInteractionEvent(KeyInteractionEvent.Up(key))
            }
        }
    }

    private suspend fun handleKeyPressAction(key: Key) {
        logger.d { "handleKeyPressAction: key=${key.id}" }

        when {
            key.actions.press.output != null -> {
                handleKeyPressOutputAction(key = key, output = key.actions.press.output)
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
        logger.d { "emitKeyboardEvent: event=$event" }

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
            is KeyInteractionEvent.Up -> handleKeyUp(key = key)
            is KeyInteractionEvent.DoubleTap -> handleKeyDoubleTap(key = key)
            is KeyInteractionEvent.LongPress -> handleKeyLongPress(key = key)
            is KeyInteractionEvent.Repeat -> handleKeyRepeat(key = key)
        }
    }

    private suspend fun handleKeyDown(key: Key) {
        logger.d { "handleKeyDown: $key" }
    }

    private suspend fun handleKeyUp(key: Key) {
        logger.d { "handleKeyUp: $key" }

        handleKeyPressAction(key = key)
    }

    private suspend fun handleKeyDoubleTap(key: Key) {
        logger.d { "handleKeyDoubleTap: $key" }

        val doubleTapCommand = key.actions.doubleTap?.command
        if (doubleTapCommand == null) {
            handleKeyPressAction(key = key)
        } else {
            handleKeyCommandAction(key = key, command = doubleTapCommand)
        }
    }

    private fun handleKeyLongPress(key: Key) {
        logger.d { "handleKeyLongPress: $key" }

        keyboardStateInteractor.addLongPressedKey(key.id)
    }

    private suspend fun handleKeyRepeat(key: Key) {
        logger.d { "handleKeyRepeat: $key" }

        handleKeyPressAction(key = key)
    }

    private companion object {
        private const val LOG_TAG = "KeyboardInteractor"
    }
}