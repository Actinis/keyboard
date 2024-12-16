package io.actinis.remote.keyboard.presentation.touch

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.key.Key
import kotlin.math.sqrt

private const val LOG_TAG = "TouchDetection"
private val logger = Logger.withTag(LOG_TAG)

// TODO: Respect density
private const val MAX_NEAREST_KEY_DISTANCE = 70f

internal data class KeyBoundary(
    val key: Key,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val centerX: Float by lazy(LazyThreadSafetyMode.NONE) { right - (right - left) / 2f }

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
    isLongPressOverlayActive: Boolean = false,
    onLongPressMove: ((deltaX: Float, deltaY: Float) -> Unit),
) {
    var initialPosition: Offset? = null

    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            val position = event.changes.first().position

            when (event.type) {
                PointerEventType.Press -> {
                    initialPosition = position
                    if (!isLongPressOverlayActive) {
                        keyBoundaries
                            .findKeyAtPosition(position)
                            ?.let(onPress)
                    }
                }

                PointerEventType.Move -> {
                    if (initialPosition == null) {
                        initialPosition = position
                    }

                    val localInitialPosition = initialPosition
                    if (isLongPressOverlayActive && localInitialPosition != null) {
                        val deltaX = position.x - localInitialPosition.x
                        val deltaY = position.y - localInitialPosition.y
                        onLongPressMove.invoke(deltaX, deltaY)
                    } else {
                        logger.d { "Proceeding with normal detection: isLongPressOverlayActive=$isLongPressOverlayActive, localInitialPosition=$localInitialPosition" }
                        keyBoundaries
                            .findKeyAtPosition(position)
                            ?.let(onPress)
                    }
                }

                PointerEventType.Release, PointerEventType.Exit, PointerEventType.Unknown -> {
                    initialPosition = null
                    onRelease()
                }

                else -> {}
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