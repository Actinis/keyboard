package io.actinis.remote.keyboard.domain.text.suggestion

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class TextSuggestionsInteractorImpl : BaseTextSuggestionsInteractor() {

    override suspend fun updateCurrentLanguage(languageCode: String) {
        // No-op for now
    }

    override suspend fun updateCurrentText(text: String) {
        // No-op for now
    }
}