package io.actinis.remote.keyboard.data.config.model.key

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class KeysRow(
    @SerialName("id")
    val id: String,
    @SerialName("keys")
    val keys: List<Key> = emptyList(),
)