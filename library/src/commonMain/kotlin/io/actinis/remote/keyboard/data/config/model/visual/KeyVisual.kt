package io.actinis.remote.keyboard.data.config.model.visual

import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeyVisual(
    @SerialName("icon")
    val icon: Icon? = null,
    @SerialName("activeIcon")
    val activeIcon: Icon? = null,
    @SerialName("label")
    val label: String? = null,
    @SerialName("adaptiveLabels")
    val actionKeyLabels: ActionKeyLabels? = null,
    @SerialName("modifiers")
    val modifiers: Map<KeyboardModifier, KeyVisual> = emptyMap(),
) {

    @Serializable
    enum class Icon {
        @SerialName("backspace")
        BACKSPACE,

        @SerialName("backspace_active")
        BACKSPACE_ACTIVE,

        @SerialName("shift")
        SHIFT,

        @SerialName("shift_active")
        SHIFT_ACTIVE,

        @SerialName("caps_lock_active")
        CAPS_LOCK_ACTIVE,

        @SerialName("layout_switch")
        LAYOUT_SWITCH,

        @SerialName("layout_switch_active")
        LAYOUT_SWITCH_ACTIVE,
    }

    @Serializable
    data class ActionKeyLabels(
        @SerialName("search")
        val search: String? = null,
        @SerialName("send")
        val send: String? = null,
        @SerialName("go")
        val go: String? = null,
        @SerialName("done")
        val done: String? = null,
    )

}