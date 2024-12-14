package io.actinis.remote.keyboard.presentation.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import io.actinis.remote.keyboard.data.config.model.key.Key
import kotlin.math.sqrt

// TODO: Respect density
private const val MAX_NEAREST_KEY_DISTANCE = 70f

internal data class KeyBoundary(
    val key: Key,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {

    fun distanceTo(position: Offset): Float {
        val dx = when {
            position.x < left -> left - position.x
            position.x > right -> position.x - right
            else -> 0f
        }

        val dy = when {
            position.y < top -> top - position.y
            position.y > bottom -> position.y - bottom
            else -> 0f
        }

        if (dx == 0f && dy == 0f) return 0f

        return sqrt(dx * dx + dy * dy)
    }

    operator fun minus(offset: Offset): KeyBoundary {
        return copy(
            left = left - offset.x,
            top = top - offset.y,
            right = right - offset.x,
            bottom = bottom - offset.y,
        )
    }

    override fun toString(): String {
        return "key=${key.id}, ${left}x${top}x${right}x${bottom}"
    }
}

internal suspend fun PointerInputScope.detectTouchGestures(
    keyBoundaries: Set<KeyBoundary>,
    onPress: (key: Key) -> Unit,
    onRelease: () -> Unit,
) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()

            when (event.type) {
                PointerEventType.Press, PointerEventType.Move -> {
                    keyBoundaries
                        .findKeyAtPosition(position = event.changes.first().position)
                        ?.let(onPress)
                }

                PointerEventType.Release, PointerEventType.Exit, PointerEventType.Unknown -> {
                    onRelease()
                }

                else -> { /* Handle other events if needed */
                }
            }
        }
    }
}

private fun Set<KeyBoundary>.findKeyAtPosition(position: Offset): Key? {
    val exactMatch = firstOrNull { boundary ->
        position.x in boundary.left..boundary.right &&
                position.y in boundary.top..boundary.bottom
    }

    if (exactMatch != null) {
        return exactMatch.key
    }

    return minByOrNull { it.distanceTo(position) }
        ?.takeIf { it.distanceTo(position) <= MAX_NEAREST_KEY_DISTANCE }
        ?.key
}