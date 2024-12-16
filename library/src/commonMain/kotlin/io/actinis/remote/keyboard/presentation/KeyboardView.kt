package io.actinis.remote.keyboard.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.key.KeysRow
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier
import io.actinis.remote.keyboard.data.config.model.visual.KeyVisual
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputType
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.presentation.dimension.BaseKeyDimensions
import io.actinis.remote.keyboard.presentation.dimension.calculateBaseKeyDimensions
import io.actinis.remote.keyboard.presentation.touch.KeyBoundary
import io.actinis.remote.keyboard.presentation.touch.detectTouchGestures
import io.actinis.remote.library.generated.resources.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.getKoin
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.qualifier.named
import kotlin.math.roundToInt

private const val LOG_TAG = "KeyboardView"
private val logger = Logger.withTag(LOG_TAG)

internal data class KeyboardViewState(
    val baseKeyDimensions: BaseKeyDimensions = BaseKeyDimensions(),
    val keyboardOffset: Offset = Offset.Zero,
    val keyBoundaries: Set<KeyBoundary> = emptySet(),
)

data class KeyboardColors(
    val background: Color,
    val activeBackground: Color,
)

@Composable
fun rememberKeyboardColors(
    background: Color = Color.LightGray,
    activeBackground: Color = Color.DarkGray,
) = KeyboardColors(background, activeBackground)

@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel = koinViewModel(),
    inputType: InputType,
    isPassword: Boolean,
    modifier: Modifier = Modifier,
    onEvent: (keyboardEvent: KeyboardEvent) -> Unit,
) {
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val defaultDispatcher = getKoin().get<CoroutineDispatcher>(named(DispatchersNames.DEFAULT))

    val currentLayout by viewModel.currentLayout.collectAsState()
    val keyboardState by viewModel.keyboardState.collectAsState()
    var viewState by remember { mutableStateOf(KeyboardViewState()) }

    LaunchedEffect(inputType, isPassword) {
        viewModel.initialize(
            inputType = inputType,
            isPassword = isPassword,
        )
    }

    LaunchedEffect(Unit) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(defaultDispatcher) {
                viewModel
                    .keyboardEvents
                    .flowOn(defaultDispatcher)
                    .collect(onEvent)
            }
        }
    }

    currentLayout?.let { layout ->
        Box(modifier = modifier) {
            KeyboardLayout(
                layout = layout,
                keyboardState = keyboardState,
                viewState = viewState,
                onViewStateChange = { viewState = it },
                onHandleActiveKey = viewModel::handleActiveKey,
                onHandleKeysReleased = viewModel::handleKeysReleased,
                density = density,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            )

            KeyboardOverlay(
                viewState = viewState,
                keyboardState = keyboardState,
                keyboardLayout = layout,
            )
        }
    }
}

@Composable
private fun KeyboardLayout(
    layout: KeyboardLayout,
    keyboardState: KeyboardState,
    viewState: KeyboardViewState,
    onViewStateChange: (KeyboardViewState) -> Unit,
    onHandleActiveKey: (Key) -> Unit,
    onHandleKeysReleased: () -> Unit,
    density: Density,
    modifier: Modifier = Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var position by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(size) {
        if (size != IntSize.Zero) {
            val newDimensions = calculateBaseKeyDimensions(
                keyboardLayout = layout,
                containerSizeInPixels = size,
                density = density,
            )
            if (newDimensions != viewState.baseKeyDimensions) {
                logger.d { "Base key dimensions = $newDimensions dp" }
                onViewStateChange(viewState.copy(baseKeyDimensions = newDimensions))
            }
        }
    }

    LaunchedEffect(position) {
        if (position != viewState.keyboardOffset) {
            onViewStateChange(viewState.copy(keyboardOffset = position))
        }
    }

    Column(
        modifier = modifier
            .padding(bottom = 16.dp)
            .onSizeChanged {
                size = it
            }
            .onGloballyPositioned { coordinates ->
                position = coordinates.positionInRoot()
            }
            .pointerInput(Unit) {
                detectTouchGestures(
                    keyBoundaries = viewState.keyBoundaries,
                    onPress = onHandleActiveKey,
                    onRelease = onHandleKeysReleased,
                )
            }
    ) {
        layout.rows.forEach { row ->
            KeyboardRow(
                key = row.hashCode(),
                keysRow = row,
                keyboardState = keyboardState,
                baseKeyDimensions = viewState.baseKeyDimensions,
                keyboardOffset = viewState.keyboardOffset,
                onKeyBoundaryUpdate = { boundary ->
                    val newBoundaries = (viewState.keyBoundaries
                        .filterNot { it.key.id == boundary.key.id } + (boundary - viewState.keyboardOffset))
                        .toSet()
                    if (newBoundaries != viewState.keyBoundaries) {
                        onViewStateChange(viewState.copy(keyBoundaries = newBoundaries))
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(
                        top = viewState.baseKeyDimensions.rowSpacing,
                        bottom = viewState.baseKeyDimensions.rowSpacing,
                    )
            )
        }
    }
}

@Composable
private fun KeyboardRow(
    key: Int,
    keysRow: KeysRow,
    keyboardState: KeyboardState,
    baseKeyDimensions: BaseKeyDimensions,
    keyboardOffset: Offset,
    onKeyBoundaryUpdate: (KeyBoundary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(baseKeyDimensions.columnSpacing)
    ) {
        keysRow.keys.forEach { key ->
            KeyboardKeyContainer(
                key = key,
                keyboardState = keyboardState,
                onKeyBoundaryUpdate = onKeyBoundaryUpdate,
                modifier = Modifier
                    .size(
                        width = baseKeyDimensions.baseWidth * key.width,
                        height = baseKeyDimensions.baseHeight,
                    )
            )
        }
    }
}

@Composable
private fun KeyboardKeyContainer(
    key: Key,
    keyboardState: KeyboardState,
    onKeyBoundaryUpdate: (KeyBoundary) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                val position = coordinates.positionInRoot()
                val size = coordinates.size

                onKeyBoundaryUpdate(
                    KeyBoundary(
                        key = key,
                        left = position.x,
                        top = position.y,
                        right = position.x + size.width,
                        bottom = position.y + size.height
                    )
                )
            }
    ) {
        KeyboardKey(
            key = key,
            keyboardState = keyboardState,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun KeyboardKey(
    key: Key,
    keyboardState: KeyboardState,
    modifier: Modifier = Modifier,
) {
    val isKeyActive = keyboardState.isKeyActive(key)
    val activeModifierForKey = keyboardState.getActiveModifierForKey(key)
    val colors = rememberKeyboardColors()

    val backgroundColor = remember(isKeyActive, activeModifierForKey) {
        if (isKeyActive || activeModifierForKey != null) colors.activeBackground else colors.background
    }

    val icon = remember(isKeyActive, activeModifierForKey) {
        key.getIcon(isActive = isKeyActive, activeModifierForKey = activeModifierForKey)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        when {
            icon != null -> {
                Icon(
                    imageVector = getKeyIcon(icon),
                    contentDescription = null
                )
            }

            key.visual?.label != null -> {
                // TODO: Calculate text size for all chars basing on settings
                Text(
                    text = key.visual.label,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                val text = remember(keyboardState.areLettersUppercase) {
                    when {
                        keyboardState.areLettersUppercase -> key.actions.press.output?.uppercase() ?: ""
                        else -> key.actions.press.output ?: ""
                    }
                }
                // TODO: Calculate text size for all chars basing on settings
                Text(
                    text = text,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun BoxScope.KeyboardOverlay(
    viewState: KeyboardViewState,
    keyboardState: KeyboardState,
    keyboardLayout: KeyboardLayout,
    modifier: Modifier = Modifier,
) {
    val hasPressedKeys = keyboardState.pressedKeysIds.isNotEmpty()
    val hasLongPressedKeys = keyboardState.longPressedKeysIds.isNotEmpty()

    logger.d { "Show overlay: $hasPressedKeys" }

    if (hasPressedKeys) {
        Box(
            modifier = modifier
                .matchParentSize()
        ) {
            keyboardState.pressedKeysIds.forEach { keyId ->
                // TODO: Use Map instead of searching
                val key = keyboardLayout.findKey { it.id == keyId }
                if (key == null) {
                    logger.e { "Can not find key with id $keyId in overlay" }
                    return@forEach
                }

                // TODO: Use Map instead of searching
                val keyBoundary = viewState.keyBoundaries.find { it.key.id == keyId }
                if (keyBoundary == null) {
                    logger.e { "Can not find boundary for key with id $keyId in overlay" }
                    return@forEach
                }

                val popupOnLongPress = key.actions.longPress?.popup == true
                val isLongPressed = keyboardState.longPressedKeysIds.contains(key.id)

                when {
                    isLongPressed && popupOnLongPress -> {
                        key.actions.longPress?.let { longPressAction ->
                            if (longPressAction.values.isNotEmpty()) {
                                LongPressedKeyBubble(
                                    keyboardState = keyboardState,
                                    key = key,
                                    values = longPressAction.values,
                                    keyBoundary = keyBoundary,
                                )
                            } else {
                                logger.w { "Empty values for long press for key $key" }
                            }
                        }
                    }

                    key.type == Key.Type.CHARACTER -> {
                        PressedKeyBubble(
                            keyboardState = keyboardState,
                            key = key,
                            keyBoundary = keyBoundary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PressedKeyBubble(
    keyboardState: KeyboardState,
    key: Key,
    keyBoundary: KeyBoundary,
) {
    KeyBubble(
        keyboardState = keyboardState,
        key = key,
        keyBoundary = keyBoundary,
    ) {
        KeyBubbleText(
            keyboardState = keyboardState,
            keyText = key.actions.press.output.orEmpty()
        )
    }
}

@Composable
private fun LongPressedKeyBubble(
    keyboardState: KeyboardState,
    key: Key,
    values: Collection<String>,
    keyBoundary: KeyBoundary,
) {
    KeyBubble(
        keyboardState = keyboardState,
        key = key,
        keyBoundary = keyBoundary,
    ) {
        Column {
            values.chunked(4).forEach { rowValues -> // TODO: Get number from settings
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    rowValues.forEach { value ->
                        KeyBubbleText(
                            keyboardState = keyboardState,
                            keyText = value,
                            modifier = Modifier
                                .padding(start = 16.dp, end = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyBubble(
    keyboardState: KeyboardState,
    key: Key,
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

                    // TODO: Check if out of screen

                    placeable.place(
                        x = left,
                        y = top,
                    )
                }
            }
            .defaultMinSize(48.dp, 48.dp)
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = MaterialTheme.shapes.medium.copy(
                    all = CornerSize(8.dp)
                )
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
        style = MaterialTheme.typography.titleLarge, // TODO: Calculate text size for all chars basing on settings
        color = Color.Black,
        modifier = modifier,
    )
}

private fun KeyboardState.isKeyActive(key: Key): Boolean {
    return pressedKeysIds.contains(key.id)
}

private fun KeyboardState.getActiveModifierForKey(key: Key): KeyboardModifier? {
    if (key.type != Key.Type.MODIFIER || activeModifiers.isEmpty()) {
        return null
    }

    val keyModifiers = key.visual?.modifiers
    return keyModifiers?.let {
        activeModifiers.find { activeModifier -> activeModifier in keyModifiers }
    }
}

private fun Key.getIcon(
    isActive: Boolean,
    activeModifierForKey: KeyboardModifier?,
): KeyVisual.Icon? {
    if (visual == null) {
        return null
    }

    if (activeModifierForKey != null) {
        val modifierVisual = visual.modifiers[activeModifierForKey]
        if (modifierVisual != null) {
            if (isActive && modifierVisual.activeIcon != null) {
                return modifierVisual.activeIcon
            } else if (!isActive && modifierVisual.icon != null) {
                return modifierVisual.icon
            }
        }
    }

    if (isActive && visual.activeIcon != null) {
        return visual.activeIcon
    } else if (!isActive && visual.activeIcon != null) {
        return visual.icon
    }

    return null
}

@Composable
private fun getKeyIcon(keyIcon: KeyVisual.Icon): ImageVector {
    return when (keyIcon) {
        KeyVisual.Icon.BACKSPACE -> vectorResource(Res.drawable.ic_backspace_normal)
        KeyVisual.Icon.BACKSPACE_ACTIVE -> vectorResource(Res.drawable.ic_backspace_pressed)
        KeyVisual.Icon.SHIFT -> vectorResource(Res.drawable.ic_shift_normal)
        KeyVisual.Icon.SHIFT_ACTIVE -> vectorResource(Res.drawable.ic_shift_pressed)
        KeyVisual.Icon.CAPS_LOCK_ACTIVE -> vectorResource(Res.drawable.ic_caps_lock_pressed)
        KeyVisual.Icon.LAYOUT_SWITCH -> vectorResource(Res.drawable.ic_layout_switch_normal)
        KeyVisual.Icon.LAYOUT_SWITCH_ACTIVE -> vectorResource(Res.drawable.ic_layout_switch_pressed)
    }
}
