package io.actinis.remote.keyboard.data.text.repository

import io.actinis.remote.keyboard.data.text.model.LanguageRules
import io.actinis.remote.keyboard.util.resources.loadJsonFromResources
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json

interface LanguagesRepository {
    suspend fun getLanguageRules(language: String): LanguageRules
}

internal class LanguagesRepositoryImpl(
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
) : LanguagesRepository {

    private val languagesRules = mutableMapOf<String, LanguageRules>()

    override suspend fun getLanguageRules(language: String): LanguageRules {
        var languageRules = languagesRules[language]
        if (languageRules == null) {
            languageRules = loadJsonFromResources<LanguageRules>(
                path = "files/languages/rules/$language.json",
                json = json,
                ioDispatcher = ioDispatcher,
                defaultDispatcher = defaultDispatcher,
            )
            languagesRules[language] = languageRules
        }

        return languageRules
    }
}