package io.actinis.remote.keyboard.data.state.model

import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier

data class KeyboardState(
    val pressedKeyId: String? = null,
    val longPressedKeyId: String? = null,
    val currentLayoutId: String? = null,
    val activeModifiers: Set<KeyboardModifier> = emptySet(),
) {
    val isShiftActive
        get() = activeModifiers.contains(KeyboardModifier.SHIFT)

    val isCapsLockActive
        get() = activeModifiers.contains(KeyboardModifier.CAPS_LOCK)

    val areLettersUppercase
        get() = isShiftActive || isCapsLockActive
}