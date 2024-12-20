package io.actinis.remote.keyboard.domain.text.suggestion

import android.view.textservice.*
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal actual class TextSuggestionsInteractorImpl(
    private val textServicesManager: TextServicesManager,
) : BaseTextSuggestionsInteractor(), SpellCheckerSession.SpellCheckerSessionListener {

    private val logger = Logger.withTag(LOG_TAG)

    private var lastLanguageCode: String? = null
    private var lastTextInfo: TextInfo? = null

    private var spellCheckerSession: SpellCheckerSession? = null

    override fun onGetSuggestions(results: Array<SuggestionsInfo>) {
        logger.d { "onGetSuggestions: count=${results.size}" }

        val suggestions = mutableListOf<String>()

        for (result in results) {
            logger.d { "Next result: count=${result.suggestionsCount}, attrs=${result.suggestionsAttributes}" }

            for (i in 0 until result.suggestionsCount) {
                logger.d { "Suggestion #$i: ${result.getSuggestionAt(i)}" }
                suggestions.add(result.getSuggestionAt(i))
            }

            logger.d { "=========" }
        }
    }

    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        logger.d { "onGetSentenceSuggestions: count=${results?.size}" }
        results?.forEach { result ->
            for (i in 0 until result.suggestionsCount) {
                val suggestionInfo = result.getSuggestionsInfoAt(i)
                if (suggestionInfo.suggestionsCount > 0) {
                    logger.d {
                        "Suggestion #$i: ${suggestionInfo.suggestionsCount} suggestions, " +
                                "attrs=${suggestionInfo.suggestionsAttributes}, " +
                                "length=${result.getLengthAt(i)}, " +
                                "offset=${result.getOffsetAt(i)}"
                    }

                    for (j in 0 until suggestionInfo.suggestionsCount) {
                        logger.d { "\tSuggestion #$j: ${suggestionInfo.getSuggestionAt(j)}" }
                    }
                }
            }

            logger.d { "=========" }
        }
    }

    override suspend fun updateCurrentLanguage(languageCode: String) {
        if (languageCode != lastLanguageCode) {
            closeSession()
            lastLanguageCode = languageCode
            startSession()

            val lastTextInfo = this.lastTextInfo
            if (lastTextInfo != null) {
                spellCheckerSession?.getSuggestions(
                    lastTextInfo,
                    SUGGESTIONS_LIMIT
                )
            }
        }
    }

    private suspend fun startSession() {
        withContext(Dispatchers.Main) {
            spellCheckerSession = textServicesManager.newSpellCheckerSession(
                null,
                getLocale(),
                this@TextSuggestionsInteractorImpl,
                true
            )
        }
    }

    private fun getLocale(): Locale? {
        val languageCode = this.lastLanguageCode
        if (languageCode == null) {
            logger.d { "Language code is not set, will not create a locale" }
            return null
        }

        val splittedLanguageCode = languageCode.split("_")
        if (splittedLanguageCode.size != 2) {
            logger.e { "Invalid language code = $languageCode" }
            return null
        }

        return Locale(splittedLanguageCode[0], splittedLanguageCode[1])
    }

    private fun closeSession() {
        spellCheckerSession?.close()
        spellCheckerSession = null
    }

    override suspend fun updateCurrentText(text: String) {
        // No-op for now
//        if (text.isEmpty()) return
//
//        if (spellCheckerSession == null || spellCheckerSession?.isSessionDisconnected == true) {
//            logger.d { "Spell checker session is disconnected, will re-start" }
//            closeSession()
//            startSession()
//        }
//
//        val textInfo = TextInfo(text)
//        lastTextInfo = textInfo
//
//        spellCheckerSession?.getSuggestions(
//            textInfo,
//            SUGGESTIONS_LIMIT
//        )
//
//        spellCheckerSession?.getSentenceSuggestions(arrayOf(textInfo), SUGGESTIONS_LIMIT)
    }

    private companion object {
        private const val LOG_TAG = "TextSuggestionsInteractor"

        private const val SUGGESTIONS_LIMIT = 3
    }
}