package io.actinis.remote.keyboard.presentation.model


data class KeyboardOverlayState(
    val isActive: Boolean = false,
    val showBackground: Boolean = false,
    val activeBubble: KeyboardOverlayBubble? = null,
)