package io.actinis.remote.keyboard.data.config.model.layout

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class LayoutType {
    @SerialName("alphabetic")
    ALPHABETIC,

    @SerialName("emoji")
    EMOJI,

    @SerialName("numeric")
    NUMERIC,

    @SerialName("symbols")
    SYMBOLS;

    override fun toString(): String {
        return when (this) {
            ALPHABETIC -> "alphabetic"
            EMOJI -> "emoji"
            NUMERIC -> "numeric"
            SYMBOLS -> "symbols"
        }
    }
}