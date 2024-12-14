package io.actinis.remote.keyboard.data.config.model.behavior

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Behaviors(
    @SerialName("keyRepeat")
    val keyRepeat: KeyRepeat,
    @SerialName("longPress")
    val longPress: LongPressBehavior,
    @SerialName("doubleTap")
    val doubleTap: DoubleTapBehavior,
) {

    @Serializable
    data class KeyRepeat(
        @SerialName("initialDelay")
        val initialDelay: Long,
        @SerialName("repeatInterval")
        val repeatInterval: Long,
    )

    @Serializable
    data class LongPressBehavior(
        @SerialName("delay")
        val delay: Long,
    )

    @Serializable
    data class DoubleTapBehavior(
        @SerialName("maxDelay")
        val maxDelay: Long,
    )
}