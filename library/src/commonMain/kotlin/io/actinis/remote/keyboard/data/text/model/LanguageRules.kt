package io.actinis.remote.keyboard.data.text.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LanguageRules(
    @SerialName("wordSeparators")
    val wordSeparators: Set<Char> = emptySet(),
    @SerialName("sentenceEndings")
    val sentenceEndings: Set<Char> = emptySet(),
    @SerialName("abbreviations")
    val abbreviations: Set<String> = emptySet(),
)
