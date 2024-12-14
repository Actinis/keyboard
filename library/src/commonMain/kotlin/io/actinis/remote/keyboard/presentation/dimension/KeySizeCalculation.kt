package io.actinis.remote.keyboard.presentation.dimension

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout

internal data class BaseKeyDimensions(
    val baseWidth: Dp = 0.dp,
    val baseHeight: Dp = 0.dp,
    val columnSpacing: Dp = 0.dp,
    val rowSpacing: Dp = 0.dp,
)

internal fun calculateBaseKeyDimensions(
    keyboardLayout: KeyboardLayout,
    containerSizeInPixels: IntSize,
    density: Density,
): BaseKeyDimensions {
    val baseKeyWidthInPixels = containerSizeInPixels.width * keyboardLayout.dimensions.baseKeyWidthMultiplier
    val baseKeyWidth = with(density) {
        baseKeyWidthInPixels.toInt().toDp()
    }
    val baseKeyHeight = baseKeyWidth * keyboardLayout.dimensions.baseKeyHeightToWidthMultiplier

    val columnSpacingInPixels = containerSizeInPixels.width * keyboardLayout.dimensions.columnsSpacingMultiplier
    val columnSpacing = with(density) {
        columnSpacingInPixels.toInt().toDp()
    }
    val rowSpacing = baseKeyHeight * keyboardLayout.dimensions.rowsSpacingToHeightMultiplier

    return BaseKeyDimensions(
        baseWidth = baseKeyWidth,
        baseHeight = baseKeyHeight,
        columnSpacing = columnSpacing,
        rowSpacing = rowSpacing,
    )
}