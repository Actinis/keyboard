package io.actinis.remote.keyboard.domain.text

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.state.model.InputState
import io.actinis.remote.keyboard.data.text.model.LanguageRules
import io.actinis.remote.keyboard.data.text.repository.LanguagesRepository

internal interface TextAnalyzer {
    suspend fun isNewWordPosition(inputState: InputState, language: String): Boolean
    suspend fun isNewSentencePosition(inputState: InputState, language: String): Boolean
}

internal class TextAnalyzerImpl(
    private val languagesRepository: LanguagesRepository,
) : TextAnalyzer {
    private val logger = Logger.withTag(LOG_TAG)

    override suspend fun isNewWordPosition(
        inputState: InputState,
        language: String,
    ): Boolean {
        val languageRules = languagesRepository.getLanguageRules(language)

        val position = inputState.selectionStart
        val text = inputState.text

        // Handle empty text case
        if (text.isEmpty()) return true

        // Handle cursor at the start
        if (position == 0) return true

        // Handle cursor beyond text length (shouldn't happen normally)
        if (position > text.length) return true

        val prevChar = text.getOrNull(position - 1)
        val nextChar = text.getOrNull(position)

        // If there's a non-boundary character after cursor,
        // it's not a new word position
        if (nextChar != null && !isWordBoundary(languageRules, prevChar, nextChar)) {
            return false
        }

        // If previous character is a word separator, it's a new word position
        return prevChar?.let {
            isWordBoundary(languageRules, it, nextChar)
        } ?: true
    }

    override suspend fun isNewSentencePosition(
        inputState: InputState,
        language: String,
    ): Boolean {
        val languageRules = languagesRepository.getLanguageRules(language)

        val position = inputState.selectionStart
        val text = inputState.text

        // Handle empty text case
        if (text.isEmpty()) return true

        // Handle cursor at the start
        if (position == 0) return true

        // Handle cursor beyond text length
        if (position > text.length) return true

        return isSentenceBoundary(languageRules, text, position)
    }

    private fun isWordBoundary(
        languageRules: LanguageRules,
        prevChar: Char?,
        nextChar: Char?,
    ): Boolean {
        // If previous char is null, it's start of text - consider it a boundary
        if (prevChar == null) return true

        // If previous char is a word separator
        if (prevChar in languageRules.wordSeparators) return true

        // If previous char is whitespace
        if (prevChar.isWhitespace()) {
            // If next char is null (end of text) or not whitespace, it's a boundary
            return nextChar == null || !nextChar.isWhitespace()
        }

        // If we're at the end and previous char wasn't a separator/whitespace
        if (nextChar == null) {
            // It's a boundary only if previous char was a separator
            return prevChar in languageRules.wordSeparators
        }

        return false
    }

    private fun isSentenceBoundary(
        languageRules: LanguageRules,
        text: String,
        position: Int,
    ): Boolean {
        if (position <= 0) return false

        // Find last non-whitespace character before cursor
        var lastNonWhitespace = position - 1
        while (lastNonWhitespace > 0 && text[lastNonWhitespace].isWhitespace()) {
            lastNonWhitespace--
        }

        val prevChar = text[lastNonWhitespace]

        if (prevChar in languageRules.sentenceEndings) {
            // Look back to get potential abbreviation
            val startLookBack = maxOf(0, lastNonWhitespace - MAX_ABBREVIATION_LENGTH + 1)
            val textBeforeCursor = text.substring(startLookBack, lastNonWhitespace + 1)

            // Check if this ending is not part of an abbreviation
            return !languageRules.abbreviations.any {
                textBeforeCursor.endsWith(it, ignoreCase = true)
            }
        }

        return false
    }

    private companion object {
        private const val LOG_TAG = "TextAnalyzer"

        private const val MAX_ABBREVIATION_LENGTH = 10
    }
}