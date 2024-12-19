package io.actinis.remote.keyboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayBubble
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayState
import io.actinis.remote.keyboard.presentation.touch.KeyBoundary
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.KeyboardOverlay(
    overlayState: KeyboardOverlayState,
    viewState: KeyboardViewState,
    keyboardState: KeyboardState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.matchParentSize()
    ) {
        when (val bubble = overlayState.activeBubble) {
            is KeyboardOverlayBubble.PressedKey -> {
                val pressedKeyId = keyboardState.pressedKeyId
                val keyBoundary = viewState.keyBoundaries.find { it.key.id == pressedKeyId }
                if (keyBoundary != null) {
                    PressedKeyBubble(
                        keyboardState = keyboardState,
                        keyBoundary = keyBoundary,
                        bubble = bubble,
                    )
                }
            }

            is KeyboardOverlayBubble.LongPressedKey -> {
                val longPressedKeyId = keyboardState.longPressedKeyId
                val keyBoundary = viewState.keyBoundaries.find { it.key.id == longPressedKeyId }
                if (keyBoundary != null) {
                    LongPressedKeyBubble(
                        keyboardState = keyboardState,
                        keyBoundary = keyBoundary,
                        bubble = bubble,
                    )
                }
            }

            null -> {}
        }
    }
}

@Composable
private fun PressedKeyBubble(
    keyboardState: KeyboardState,
    keyBoundary: KeyBoundary,
    bubble: KeyboardOverlayBubble.PressedKey,
) {
    KeyBubble(
        keyBoundary = keyBoundary,
    ) {
        KeyBubbleText(
            keyboardState = keyboardState,
            keyText = bubble.text,
        )
    }
}

@Composable
private fun LongPressedKeyBubble(
    keyboardState: KeyboardState,
    keyBoundary: KeyBoundary,
    bubble: KeyboardOverlayBubble.LongPressedKey,
) {
    KeyBubble(
        keyBoundary = keyBoundary,
    ) {
        Column {
            bubble.items.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    row.forEachIndexed { columnIndex, item ->
                        val isSelected = rowIndex == bubble.selectedItemRow &&
                                columnIndex == bubble.selectedItemColumn

                        Box(
                            modifier = Modifier
                                .defaultMinSize(48.dp, 48.dp)
                                .background(
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = MaterialTheme.shapes.medium,
                                )
                        ) {
                            KeyBubbleText(
                                keyboardState = keyboardState,
                                keyText = item.text,
                                modifier = Modifier
                                    .align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyBubble(
    keyBoundary: KeyBoundary,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    var left = (keyBoundary.centerX - placeable.width / 2).roundToInt()
                    val right = left + placeable.width

                    if (left < 0) {
                        left = 16.dp.roundToPx()
                    } else if (right > constraints.maxWidth) {
                        left = constraints.maxWidth - placeable.width
                    }

                    val top = keyBoundary.top.roundToInt() - placeable.height - 16.dp.roundToPx()

                    placeable.place(
                        x = left,
                        y = top,
                    )
                }
            }
            .defaultMinSize(48.dp, 48.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shape = MaterialTheme.shapes.medium,
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun KeyBubbleText(
    keyboardState: KeyboardState,
    keyText: String,
    modifier: Modifier = Modifier,
) {
    val text = remember(keyboardState.areLettersUppercase) {
        when {
            keyboardState.areLettersUppercase -> keyText.uppercase()
            else -> keyText
        }
    }

    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}