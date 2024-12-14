package io.actinis.remote.keyboard.presentation.touch

import io.actinis.remote.keyboard.data.config.model.key.Key

sealed interface KeyInteractionEvent {

    val key: Key

    data class Down(override val key: Key) : KeyInteractionEvent
    data class Up(override val key: Key) : KeyInteractionEvent
    data class DoubleTap(override val key: Key) : KeyInteractionEvent
    data class LongPress(override val key: Key) : KeyInteractionEvent
    data class Repeat(override val key: Key) : KeyInteractionEvent
}
