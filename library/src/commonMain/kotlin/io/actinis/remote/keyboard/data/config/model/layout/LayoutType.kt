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
    SYMBOLS,
}