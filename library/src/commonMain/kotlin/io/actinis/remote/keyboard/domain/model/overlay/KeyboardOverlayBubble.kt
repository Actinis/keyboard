package io.actinis.remote.keyboard.domain.model.overlay

sealed interface KeyboardOverlayBubble {
    data class PressedKey(
        val text: String,
    ) : KeyboardOverlayBubble

    data class LongPressedKey(
        val items: List<List<Item>>,
        val selectedItemRow: Int = 0,
        val selectedItemColumn: Int = 0,
    ) : KeyboardOverlayBubble {

        // TODO: Move this class somewhere out
        data class Item(
            val id: String,
            val text: String,
        ) {

            companion object {
                const val MANAGE_KEYBOARD_LAYOUTS_ID = "manage_keyboard_layouts"
            }
        }
    }
}