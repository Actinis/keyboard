package io.actinis.remote.keyboard.data.config.model.modifier

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class KeyboardModifier {
    @SerialName("SHIFT")
    SHIFT,

    @SerialName("CAPS_LOCK")
    CAPS_LOCK,
}