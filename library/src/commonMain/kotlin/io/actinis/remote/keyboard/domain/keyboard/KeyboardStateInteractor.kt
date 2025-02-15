package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.config.model.layout.LayoutType
import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier
import io.actinis.remote.keyboard.data.config.repository.KeyboardLayoutsRepository
import io.actinis.remote.keyboard.data.state.model.InputState
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.domain.input.InputStateInteractor
import io.actinis.remote.keyboard.domain.preferences.PreferencesInteractor
import io.actinis.remote.keyboard.domain.text.TextAnalyzer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

internal interface KeyboardStateInteractor {
    val inputState: StateFlow<InputState?>
    val keyboardState: StateFlow<KeyboardState>
    val currentLayout: StateFlow<KeyboardLayout?>

    val isShiftActive: Boolean
    val isCapsLockActive: Boolean

    suspend fun updateInputState(inputState: InputState)
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
    private val inputStateInteractor: InputStateInteractor,
    private val preferencesInteractor: PreferencesInteractor,
    private val textAnalyzer: TextAnalyzer,
) : KeyboardStateInteractor {

    private val logger = Logger.withTag(LOG_TAG)

    override val inputState = inputStateInteractor.inputState
    override val keyboardState: MutableStateFlow<KeyboardState> = MutableStateFlow(KeyboardState())
    override val currentLayout: MutableStateFlow<KeyboardLayout?> = MutableStateFlow(null)

    override val isShiftActive: Boolean
        get() = keyboardState.value.isShiftActive

    override val isCapsLockActive: Boolean
        get() = keyboardState.value.isCapsLockActive

    val isShiftOrCapsLockActive: Boolean
        get() = isShiftActive || isCapsLockActive

    override suspend fun updateInputState(inputState: InputState) {
        logger.d { "updateInputState: inputState=$inputState" }

        val currentInputState = this.inputState.value

        if (currentInputState == null || inputState::class != currentInputState::class) {
            logger.d { "Input state type changed: $currentInputState -> $inputState" }
            switchLayout(getLayoutIdForInputState(inputState))
        }

        inputStateInteractor.updateInputState(inputState)
        updateShiftState(inputState)
    }

    private fun getLayoutIdForInputState(inputState: InputState): String {
        val layoutType = getLayoutTypeForInputState(inputState)

        val currentLayoutValue = currentLayout.value

        if (currentLayoutValue?.metadata?.type == layoutType) {
            return currentLayoutValue.metadata.id
        }

        return when (layoutType) {
            LayoutType.ALPHABETIC, LayoutType.SYMBOLS -> Actions.Action.ALPHABETIC_LAYOUT_ID
            else -> findLayoutIdForType(layoutType) ?: Actions.Action.ALPHABETIC_LAYOUT_ID
        }
    }

    private fun getLayoutTypeForInputState(inputState: InputState): LayoutType {
        return when (inputState) {
            is InputState.Text -> LayoutType.ALPHABETIC
            is InputState.Number -> LayoutType.NUMERIC
            is InputState.Phone -> LayoutType.PHONE
        }
    }

    private fun findLayoutIdForType(layoutType: LayoutType): String? {
        val layoutConfig = keyboardLayoutsRepository.globalConfig.availableLayouts.values.find { layoutConfig ->
            layoutConfig.type == layoutType
        }

        if (layoutConfig == null) {
            logger.e { "Failed to find layout key for type=$layoutType" }
        }

        return layoutConfig?.defaultId
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

        updateLanguage(layout)
    }

    private suspend fun updateLanguage(layout: KeyboardLayout) {
        inputStateInteractor.updateLanguage(layout.metadata.language)
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

    private suspend fun updateShiftState(inputState: InputState) {
        if (inputState !is InputState.Text) return

        val language = inputStateInteractor.language.value ?: return

        val isShiftActive = this.isShiftActive
        val isCapsLockActive = this.isCapsLockActive
        val isShiftOrCapsLockActive = isShiftActive || isCapsLockActive

        val shouldToggleShift = when (inputState.capitalizationMode) {
            InputState.Text.CapitalizationMode.NONE -> false
            InputState.Text.CapitalizationMode.ALL_CHARACTERS -> !isShiftOrCapsLockActive
            InputState.Text.CapitalizationMode.WORDS -> {
                val isNewWord = textAnalyzer.isNewWordPosition(inputState = inputState, language = language)
                isNewWord && !isShiftOrCapsLockActive || !isNewWord && isShiftActive
            }

            InputState.Text.CapitalizationMode.SENTENCES -> {
                val isNewSentence = textAnalyzer.isNewSentencePosition(inputState = inputState, language = language)
                isNewSentence && !isShiftOrCapsLockActive || !isNewSentence && isShiftActive
            }
        }

        if (shouldToggleShift) {
            toggleShift()
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
