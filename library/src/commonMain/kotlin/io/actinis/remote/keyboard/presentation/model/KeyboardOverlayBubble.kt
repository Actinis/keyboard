package io.actinis.remote.keyboard.presentation.model

sealed interface KeyboardOverlayBubble {
    data class PressedKey(
        val text: String,
    ) : KeyboardOverlayBubble

    data class LongPressedKey(
        val items: List<List<Item>>,
        val selectedItemRow: Int = 0,
        val selectedItemColumn: Int = 0,
    ) : KeyboardOverlayBubble {
        data class Item(
            val id: String,
            val text: String,
        )
    }
}