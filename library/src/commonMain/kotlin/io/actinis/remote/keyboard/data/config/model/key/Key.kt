package io.actinis.remote.keyboard.data.config.model.key

import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.visual.KeyVisual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Key(
    @SerialName("key")
    val id: String,
    @SerialName("type")
    val type: Type,
    @SerialName("width")
    val width: Float,
    @SerialName("actions")
    val actions: Actions,
    @SerialName("visual")
    val visual: KeyVisual? = null,
) {

    override fun toString(): String {
        return "Key(key='$id', type=$type)"
    }

    @Serializable
    enum class Type {
        @SerialName("character")
        CHARACTER,

        @SerialName("system")
        SYSTEM,

        @SerialName("modifier")
        MODIFIER
    }

    companion object {
        const val ID_SHIFT = "shift"
    }
}