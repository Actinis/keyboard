package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier
import io.actinis.remote.keyboard.data.config.repository.ConfigurationRepository
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal interface KeyboardStateInteractor {
    val keyboardState: StateFlow<KeyboardState>
    val currentLayout: StateFlow<KeyboardLayout?>

    val isShiftActive: Boolean
    val isCapsLockActive: Boolean

    suspend fun initialize(inputType: InputType, isPassword: Boolean)
    suspend fun switchLayout(layoutId: String)

    fun addPressedKey(keyId: String)
    fun addLongPressedKey(keyId: String)

    fun removePressedKey(keyId: String)
    fun removePressedKeys(keysIds: Collection<String>)
    fun removePressedKeys()

    fun getPressedKeysIdsExcept(exceptKeyId: String): Sequence<String>

    fun isKeyPressed(keyId: String): Boolean

    fun toggleModifier(keyboardModifier: KeyboardModifier)
    fun addModifier(keyboardModifier: KeyboardModifier)
    fun removeModifier(keyboardModifier: KeyboardModifier)

    fun toggleShift()
    fun toggleCapsLock()

    fun turnOffShift()

}

/**
 * TODO: Cache Key id for active keys to speed up lookups
 */
internal class KeyboardStateInteractorImpl(
    private val configurationRepository: ConfigurationRepository,
) : KeyboardStateInteractor {

    private val logger = Logger.withTag(LOG_TAG)

    override val keyboardState: MutableStateFlow<KeyboardState> = MutableStateFlow(KeyboardState())
    override val currentLayout: MutableStateFlow<KeyboardLayout?> = MutableStateFlow(null)

    override val isShiftActive: Boolean
        get() = keyboardState.value.isShiftActive

    override val isCapsLockActive: Boolean
        get() = keyboardState.value.isCapsLockActive

    override suspend fun initialize(inputType: InputType, isPassword: Boolean) {
        logger.d { "initialize: inputType=$inputType, isPassword=$isPassword" }

        keyboardState.update {
            it.copy(
                inputType = inputType,
                isPassword = isPassword,
            )
        }

        switchLayout(
            layoutId = keyboardState.value.currentLayoutId ?: configurationRepository.globalConfig.defaultLayout
        )
    }

    override suspend fun switchLayout(layoutId: String) {
        logger.d { "switchLayout: layoutId=$layoutId" }

        val layout = configurationRepository.getLayout(layoutId = layoutId)
        currentLayout.value = layout
        keyboardState.update {
            it.copy(currentLayoutId = layoutId)
        }
    }

    override fun addPressedKey(keyId: String) {
        if (!keyboardState.value.pressedKeysIds.contains(keyId)) {
            keyboardState.update {
                it.copy(pressedKeysIds = it.pressedKeysIds + keyId)
            }
        }
    }

    override fun addLongPressedKey(keyId: String) {
        if (!keyboardState.value.longPressedKeysIds.contains(keyId)) {
            keyboardState.update {
                it.copy(longPressedKeysIds = it.longPressedKeysIds + keyId)
            }
        }
    }

    override fun removePressedKey(keyId: String) {
        if (keyboardState.value.pressedKeysIds.contains(keyId)) {
            keyboardState.update {
                it.copy(
                    pressedKeysIds = it.pressedKeysIds - keyId,
                    longPressedKeysIds = it.longPressedKeysIds + keyId
                )
            }
        }
    }

    override fun removePressedKeys(keysIds: Collection<String>) {
        keyboardState.update {
            val keyIdsSet = keysIds.toSet()

            it.copy(
                pressedKeysIds = it.pressedKeysIds - keyIdsSet,
                longPressedKeysIds = it.longPressedKeysIds - keyIdsSet,
            )
        }
    }

    override fun removePressedKeys() {
        keyboardState.value.apply {
            if (pressedKeysIds.isNotEmpty() || longPressedKeysIds.isNotEmpty()) {
                keyboardState.update {
                    it.copy(
                        pressedKeysIds = emptySet(),
                        longPressedKeysIds = emptySet(),
                    )
                }
            }
        }
    }

    override fun getPressedKeysIdsExcept(exceptKeyId: String): Sequence<String> {
        return keyboardState.value.pressedKeysIds
            .asSequence()
            .filter { keyId ->
                keyId != exceptKeyId
            }
    }

    override fun isKeyPressed(keyId: String): Boolean {
        return keyboardState.value.pressedKeysIds.contains(keyId)
    }

    override fun toggleModifier(keyboardModifier: KeyboardModifier) {
        logger.d { "toggleModifier: keyboardModifier=$keyboardModifier" }

        if (keyboardState.value.activeModifiers.contains(keyboardModifier)) {
            removeModifier(keyboardModifier)
        } else {
            addModifier(keyboardModifier)
        }
    }

    override fun addModifier(keyboardModifier: KeyboardModifier) {
        logger.d { "addModifier: keyboardModifier=$keyboardModifier" }

        keyboardState.update {
            it.copy(activeModifiers = it.activeModifiers + keyboardModifier)
        }
    }

    override fun removeModifier(keyboardModifier: KeyboardModifier) {
        logger.d { "removeModifier: keyboardModifier=$keyboardModifier" }

        keyboardState.update {
            it.copy(activeModifiers = it.activeModifiers - keyboardModifier)
        }
    }

    override fun turnOffShift() {
        // If shift is enabled (but not caps lock)
        if (isShiftActive && !isCapsLockActive) {
            toggleShiftInternal()
        }
    }

    override fun toggleShift() {
        if (isCapsLockActive) {
            toggleCapsLock()
        } else {
            toggleShiftInternal()
        }
    }

    private fun toggleShiftInternal() {
        logger.d { "toggleShift" }

        toggleModifier(keyboardModifier = KeyboardModifier.SHIFT)

        syncShiftState()
    }

    private fun syncShiftState() {
        logger.d { "syncShiftState" }
        val key = currentLayout.value?.findKey { it.id == Key.ID_SHIFT } ?: return

        if (keyboardState.value.isShiftActive) {
            logger.d { "Adding shift to active keys" }
            addPressedKey(key.id)
        } else {
            logger.d { "Removing shift from active keys" }
            removePressedKey(key.id)
        }
    }

    override fun toggleCapsLock() {
        logger.d { "toggleCapsLock" }

        // TODO: Optimize later to toggle at once
        toggleModifier(keyboardModifier = KeyboardModifier.CAPS_LOCK)
        if (keyboardState.value.isShiftActive) {
            removeModifier(keyboardModifier = KeyboardModifier.SHIFT)
        }
    }

    private companion object {
        private const val LOG_TAG = "KeyboardStateInteractor"
    }
}
