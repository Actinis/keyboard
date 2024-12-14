package io.actinis.remote.keyboard.data.config.model.visual

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FontSize(
    @SerialName("primary")
    val primary: Float,
    @SerialName("secondary")
    val secondary: Double,
)