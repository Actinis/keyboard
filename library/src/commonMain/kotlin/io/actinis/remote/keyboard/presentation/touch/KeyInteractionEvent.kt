package io.actinis.remote.keyboard.presentation.touch

import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.domain.model.command.KeyboardCommand

internal sealed interface KeyInteractionEvent {

    val key: Key

    data class Down(
        override val key: Key,
    ) : KeyInteractionEvent

    data class Up(
        override val key: Key,
        val command: KeyboardCommand,
    ) : KeyInteractionEvent

    data class DoubleTap(
        override val key: Key,
    ) : KeyInteractionEvent

    data class LongPress(
        override val key: Key,
    ) : KeyInteractionEvent

    data class Repeat(
        override val key: Key,
    ) : KeyInteractionEvent
}
