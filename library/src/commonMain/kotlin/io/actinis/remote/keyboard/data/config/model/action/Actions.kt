package io.actinis.remote.keyboard.data.config.model.action

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Actions(
    @SerialName("press")
    val press: Action.PressAction,
    @SerialName("longPress")
    val longPress: Action.LongPressAction? = null,
    @SerialName("doubleTap")
    val doubleTap: Action.DoubleTapAction? = null,
    @SerialName("repeat")
    val repeat: Boolean = false,
) {

    sealed interface Action {

        val command: CommandType?
        val params: Map<ParameterType, String?>

        @Serializable
        data class DoubleTapAction(
            @SerialName("command")
            override val command: CommandType,
            @SerialName("params")
            override val params: Map<ParameterType, String?> = emptyMap(),
        ) : Action

        @Serializable
        data class LongPressAction(
            @SerialName("popup")
            val popup: Boolean = false,
            @SerialName("values")
            val values: List<String> = emptyList(),
            @SerialName("command")
            override val command: CommandType? = null,
            @SerialName("params")
            override val params: Map<ParameterType, String?> = emptyMap(),
        ) : Action

        @Serializable
        data class PressAction(
            @SerialName("command")
            override val command: CommandType? = null,
            @SerialName("params")
            override val params: Map<ParameterType, String?> = emptyMap(),
        ) : Action

        @Serializable
        enum class CommandType {
            @SerialName("DELETE_BACKWARD")
            DELETE_BACKWARD,

            @SerialName("DELETE_WORD")
            DELETE_WORD,

            @SerialName("ACTION")
            ACTION,

            @SerialName("SWITCH_LAYOUT")
            SWITCH_LAYOUT,

            @SerialName("SHOW_LAYOUTS")
            SHOW_LAYOUTS,

            @SerialName("MANAGE_LAYOUTS")
            MANAGE_LAYOUTS,

            @SerialName("TOGGLE_SHIFT")
            TOGGLE_SHIFT,

            @SerialName("CAPS_LOCK")
            CAPS_LOCK,

            @SerialName("SHOW_CURSOR_CONTROLS")
            SHOW_CURSOR_CONTROLS,

            @SerialName("OUTPUT_VALUE")
            OUTPUT_VALUE,
        }

        @Serializable
        enum class ParameterType {
            @SerialName("layout")
            LAYOUT,

            @SerialName("value")
            VALUE,
        }

        companion object {
            const val NEXT_LAYOUT_ID = "next"
            const val ALPHABETIC_LAYOUT_ID = "alphabetic"
        }
    }
}