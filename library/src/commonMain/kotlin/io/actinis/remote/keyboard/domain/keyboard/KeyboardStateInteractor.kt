package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.config.model.layout.LayoutType
import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier
import io.actinis.remote.keyboard.data.config.repository.KeyboardLayoutsRepository
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.domain.preferences.PreferencesInteractor
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

    fun setPressedKey(keyId: String)
    fun setLongPressedKey(keyId: String)

    fun removePressedKey()

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
    private val keyboardLayoutsRepository: KeyboardLayoutsRepository,
    private val preferencesInteractor: PreferencesInteractor,
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
            layoutId = keyboardState.value.currentLayoutId ?: Actions.Action.ALPHABETIC_LAYOUT_ID
        )
    }

    private fun getDefaultLayoutId(): String {
        return keyboardLayoutsRepository.globalConfig.defaultLayout
    }

    override suspend fun switchLayout(layoutId: String) {
        logger.d { "switchLayout: layoutId=$layoutId" }

        val realLayoutId = getRealLayoutId(layoutId)

        val layout = keyboardLayoutsRepository.getLayout(layoutId = realLayoutId)
        currentLayout.value = layout
        keyboardState.update {
            it.copy(
                pressedKeyId = null,
                longPressedKeyId = null,
                currentLayoutId = layoutId,
                activeModifiers = emptySet(),
            )
        }

        if (layout.metadata.type in layoutTypesToSaveOnSwitch) {
            preferencesInteractor.setLastKeyboardLayoutId(id = realLayoutId)
        }
    }

    private suspend fun getRealLayoutId(layoutId: String): String {
        var realLayoutId = when (layoutId) {
            Actions.Action.NEXT_LAYOUT_ID -> getNextLayoutId()
            Actions.Action.ALPHABETIC_LAYOUT_ID -> preferencesInteractor.getLastKeyboardLayoutId()
            else -> layoutId
        }

        if (realLayoutId == null) {
            logger.w { "Can not determine real layout id for $layoutId, will switch to default" }
            realLayoutId = getDefaultLayoutId()
        }

        return realLayoutId
    }

    private suspend fun getNextLayoutId(): String? {
        val enabledLayoutsIds = preferencesInteractor.availableKeyboardLayouts.value
            .filter { it.isEnabled }
            .map { it.id }

        if (enabledLayoutsIds.isEmpty()) {
            logger.w { "getNextLayoutId: no enabled layouts: ${preferencesInteractor.availableKeyboardLayouts.value}" }
            return null
        }

        val currentLayoutId = currentLayout.value?.metadata
            ?.takeIf { it.type in layoutTypesToSaveOnSwitch }
            ?.id
            ?: preferencesInteractor.getLastKeyboardLayoutId()

        return when {
            currentLayoutId == null -> enabledLayoutsIds.first()

            else -> {
                when (val currentIndex = enabledLayoutsIds.indexOf(currentLayoutId)) {
                    -1 -> enabledLayoutsIds.first()
                    enabledLayoutsIds.lastIndex -> enabledLayoutsIds.first()
                    else -> enabledLayoutsIds[currentIndex + 1]
                }
            }
        }
    }

    override fun setPressedKey(keyId: String) {
        if (keyboardState.value.pressedKeyId != keyId) {
            keyboardState.update {
                it.copy(pressedKeyId = keyId)
            }
        }
    }

    override fun setLongPressedKey(keyId: String) {
        if (keyboardState.value.longPressedKeyId != keyId) {
            keyboardState.update {
                it.copy(longPressedKeyId = keyId)
            }
        }
    }


    override fun removePressedKey() {
        keyboardState.value.apply {
            if (pressedKeyId != null || longPressedKeyId != null) {
                keyboardState.update {
                    it.copy(
                        pressedKeyId = null,
                        longPressedKeyId = null,
                    )
                }
            }
        }
    }

    override fun isKeyPressed(keyId: String): Boolean {
        return keyboardState.value.pressedKeyId == keyId
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

        private val layoutTypesToSaveOnSwitch = setOf(
            LayoutType.ALPHABETIC,
            LayoutType.EMOJI,
        )
    }
}
