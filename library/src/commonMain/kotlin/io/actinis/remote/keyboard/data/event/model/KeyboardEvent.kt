package io.actinis.remote.keyboard.data.event.model

sealed interface KeyboardEvent {

    data class TextInput(
        val text: String,
    ) : KeyboardEvent

    data object Backspace : KeyboardEvent
    data object ActionClick : KeyboardEvent
}