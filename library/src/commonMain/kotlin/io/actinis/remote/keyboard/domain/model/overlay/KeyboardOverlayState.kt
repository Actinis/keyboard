package io.actinis.remote.keyboard.domain.model.overlay


data class KeyboardOverlayState(
    val isActive: Boolean = false,
    val showBackground: Boolean = false,
    val activeBubble: KeyboardOverlayBubble? = null,
)