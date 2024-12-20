package io.actinis.remote.keyboard.data.event.model

sealed interface KeyboardEvent {

    data class SizeChanged(
        val width: Int,
        val height: Int,
    ) : KeyboardEvent

    data class TextChange(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    ) : KeyboardEvent

    data object ActionClick : KeyboardEvent
}