package io.actinis.remote.keyboard.domain.model.text

internal data class TextModificationEvent(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)
