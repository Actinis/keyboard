package io.actinis.remote.keyboard.data.event.model

sealed interface KeyboardEvent {

    data class TextChange(
        val text: String,
        val selectionStart: Int,
        val selectionEnd: Int,
    ) : KeyboardEvent

    data object ActionClick : KeyboardEvent
}