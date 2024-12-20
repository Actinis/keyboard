package io.actinis.remote.keyboard.domain.text.suggestion

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface TextSuggestionsInteractor {
    val suggestions: StateFlow<Set<String>>

    suspend fun updateCurrentLanguage(languageCode: String)
    suspend fun updateCurrentText(text: String)
}

internal abstract class BaseTextSuggestionsInteractor : TextSuggestionsInteractor {
    protected val _suggestions: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    override val suggestions = _suggestions.asStateFlow()
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect class TextSuggestionsInteractorImpl : BaseTextSuggestionsInteractor