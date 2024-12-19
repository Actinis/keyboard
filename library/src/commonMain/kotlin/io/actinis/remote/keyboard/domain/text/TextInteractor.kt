package io.actinis.remote.keyboard.domain.text

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.state.model.InputState
import io.actinis.remote.keyboard.domain.keyboard.KeyboardStateInteractor
import io.actinis.remote.keyboard.domain.model.text.TextModificationEvent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

internal interface TextInteractor {
    val textModificationEvents: Flow<TextModificationEvent>

    suspend fun insertText(text: String)
    suspend fun insertNewLine(): Boolean

    suspend fun deleteBackward()
    suspend fun deleteWordBackward()
}

internal class TextInteractorImpl(
    private val keyboardStateInteractor: KeyboardStateInteractor,
    private val defaultDispatcher: CoroutineDispatcher,
) : TextInteractor {
    private val logger = Logger.withTag(LOG_TAG)

    private val _textModificationEvents = MutableSharedFlow<TextModificationEvent>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
        extraBufferCapacity = 1
    )

    override val textModificationEvents: Flow<TextModificationEvent>
        get() = _textModificationEvents

    override suspend fun insertText(text: String) {
        updateInputText { _, currentText, currentSelectionStart, currentSelectionEnd ->
            val resultingText = buildString {
                if (currentSelectionStart > 0) {
                    append(currentText.substring(0, currentSelectionStart))
                }

                append(text)

                append(currentText.substring(currentSelectionEnd))
            }

            val newCursorPosition = currentSelectionStart + text.length

            TextModificationEvent(
                text = resultingText,
                selectionStart = newCursorPosition,
                selectionEnd = newCursorPosition,
            )
        }
    }

    private suspend fun updateInputText(
        block: suspend (
            inputState: InputState,
            currentText: String,
            currentSelectionStart: Int,
            currentSelectionEnd: Int,
        ) -> TextModificationEvent?,
    ) {
        val inputState = keyboardStateInteractor.inputState.value
        if (inputState == null) {
            logger.d { "No input state - will not output text" }
            return
        }

        val currentText = inputState.text
        val currentSelectionStart = inputState.selectionStart
        val currentSelectionEnd = inputState.selectionEnd

        block(
            inputState,
            currentText,
            currentSelectionStart,
            currentSelectionEnd
        )?.let { event ->
            _textModificationEvents.emit(event)
        }
    }

    override suspend fun insertNewLine(): Boolean {
        if (keyboardStateInteractor.inputState.value?.isMultiline == true) {
            insertText("\n")
            return true
        }

        return false
    }

    override suspend fun deleteBackward() {
        updateInputText { _, currentText, currentSelectionStart, currentSelectionEnd ->
            if (currentSelectionStart == 0 && currentSelectionEnd == 0 || currentText.isEmpty()) {
                return@updateInputText null
            }

            val text = buildString {
                if (currentSelectionStart != currentSelectionEnd) {
                    // If there's a selection, we only need text before selection start and after selection end
                    if (currentSelectionStart > 0) {
                        append(currentText.substring(0, currentSelectionStart))
                    }
                    if (currentSelectionEnd < currentText.length) {
                        append(currentText.substring(currentSelectionEnd))
                    }
                } else {
                    // If no selection, we delete one character before cursor
                    // Handle the case when cursor is at the start
                    if (currentSelectionStart > 0) {
                        append(currentText.substring(0, currentSelectionStart - 1))
                    }
                    if (currentSelectionStart < currentText.length) {
                        append(currentText.substring(currentSelectionStart))
                    }
                }
            }

            // Calculate new cursor position:
            // - If there was a selection: place cursor at selection start
            // - If there was no selection: place cursor one character before current position
            val newCursorPosition = if (currentSelectionStart != currentSelectionEnd) {
                currentSelectionStart
            } else {
                (currentSelectionStart - 1).coerceAtLeast(0)
            }

            TextModificationEvent(
                text = text,
                selectionStart = newCursorPosition,
                selectionEnd = newCursorPosition
            )
        }
    }

    override suspend fun deleteWordBackward() {
        TODO("Not yet implemented")
    }

    private companion object {
        private const val LOG_TAG = "TextInteractor"
    }
}