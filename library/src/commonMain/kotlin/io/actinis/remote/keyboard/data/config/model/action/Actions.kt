package io.actinis.remote.keyboard.data.config.model.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Actions(
    @SerialName("press")
    val press: PressAction,
    @SerialName("longPress")
    val longPress: LongPressAction? = null,
    @SerialName("doubleTap")
    val doubleTap: DoubleTapAction? = null,
    @SerialName("repeat")
    val repeat: Boolean = false,
) {

    @Serializable
    data class DoubleTapAction(
        @SerialName("command")
        val command: Command,
    )

    @Serializable
    data class LongPressAction(
        @SerialName("popup")
        val popup: Boolean = false,
        @SerialName("values")
        val values: List<String> = emptyList(),
        @SerialName("command")
        val command: String? = null,
    )

    @Serializable
    data class PressAction(
        @SerialName("output")
        val output: String? = null,
        @SerialName("command")
        val command: Command? = null,
        @SerialName("params")
        val params: Map<String, String> = emptyMap(),
    )

    @Serializable
    enum class Command {
        @SerialName("DELETE_BACKWARD")
        DELETE_BACKWARD,

        @SerialName("ACTION")
        ACTION,

        @SerialName("SWITCH_LAYOUT")
        SWITCH_LAYOUT,

        @SerialName("SHOW_LAYOUTS")
        SHOW_LAYOUTS,

        @SerialName("TOGGLE_SHIFT")
        TOGGLE_SHIFT,

        @SerialName("CAPS_LOCK")
        CAPS_LOCK,
    }
}