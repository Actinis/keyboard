package io.actinis.remote.keyboard.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.key.KeysRow
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.config.model.modifier.KeyboardModifier
import io.actinis.remote.keyboard.data.config.model.visual.KeyVisual
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputState
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.di.name.DispatchersNames
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayBubble
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayState
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

private const val LOG_TAG = "KeyboardView"
private val logger = Logger.withTag(LOG_TAG)

private const val ANIMATION_DURATION_MS = 150

@Composable
fun KeyboardView(
    viewModel: KeyboardViewModel = koinViewModel(),
    inputState: InputState,
    modifier: Modifier = Modifier,
    onEvent: (keyboardEvent: KeyboardEvent) -> Unit,
) {
    val density = LocalDensity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val defaultDispatcher = getKoin().get<CoroutineDispatcher>(named(DispatchersNames.DEFAULT))

    val currentLayout by viewModel.currentLayout.collectAsState()
    val keyboardState by viewModel.keyboardState.collectAsState()
    val overlayState by viewModel.overlayState.collectAsState()

    viewModel.updateInputState(inputState)

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
        var viewState by remember { mutableStateOf(KeyboardViewState()) }
        val localInputState by remember(inputState::class, inputState.actionType) { mutableStateOf(inputState) }

        LaunchedEffect(layout.metadata.id) {
            viewState = viewState.copy(keyBoundaries = emptySet())
        }

        AnimatedContent(
            targetState = layout,
            transitionSpec = {
                fadeIn(animationSpec = tween(ANIMATION_DURATION_MS)) togetherWith
                        fadeOut(animationSpec = tween(ANIMATION_DURATION_MS))
            },
            modifier = modifier
        ) { currentLayout ->
            Box(
                modifier = Modifier
                    .onSizeChanged { size ->
                        onEvent(
                            KeyboardEvent.SizeChanged(
                                width = size.width,
                                height = size.height,
                            )
                        )
                    }
            ) {
                KeyboardLayout(
                    layout = currentLayout,
                    keyboardState = keyboardState,
                    inputState = localInputState,
                    viewState = viewState,
                    overlayState = overlayState,
                    onViewStateChange = { viewState = it },
                    onHandleActiveKey = viewModel::handleActiveKey,
                    onHandleKeysReleased = viewModel::handleKeysReleased,
                    onLongPressMove = viewModel::handleMovementInLongPressMode,
                    density = density,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                )

                if (overlayState.isActive) {
                    KeyboardOverlay(
                        overlayState = overlayState,
                        viewState = viewState,
                        keyboardState = keyboardState,
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyboardLayout(
    layout: KeyboardLayout,
    inputState: InputState,
    keyboardState: KeyboardState,
    viewState: KeyboardViewState,
    overlayState: KeyboardOverlayState,
    onViewStateChange: (KeyboardViewState) -> Unit,
    onHandleActiveKey: (Key) -> Unit,
    onHandleKeysReleased: () -> Unit,
    onLongPressMove: (deltaX: Float, deltaY: Float) -> Unit,
    density: Density,
    modifier: Modifier = Modifier,
) {
    var size by remember(layout) { mutableStateOf(IntSize.Zero) }
    var position by remember(layout) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(layout) {
        onViewStateChange(viewState.copy(keyBoundaries = emptySet()))
    }

    LaunchedEffect(layout.metadata.id, size, position) {
        if (size != IntSize.Zero) {
            val newDimensions = calculateBaseKeyDimensions(
                keyboardLayout = layout,
                containerSizeInPixels = size,
                density = density,
            )
            onViewStateChange(
                viewState.copy(
                    baseKeyDimensions = newDimensions,
                    keyboardOffset = position,
                    keyBoundaries = emptySet()
                )
            )
        }
    }

    Column(
        modifier = modifier
            .padding(bottom = 16.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .onSizeChanged {
                size = it
            }
            .onGloballyPositioned { coordinates ->
                position = coordinates.positionInRoot()
            }
            .animateContentSize(
                animationSpec = tween(300)
            )
            .pointerInput(
                viewState.keyBoundaries,
                overlayState.activeBubble is KeyboardOverlayBubble.LongPressedKey
            ) {
                detectTouchGestures(
                    keyBoundaries = viewState.keyBoundaries,
                    onPress = onHandleActiveKey,
                    onRelease = onHandleKeysReleased,
                    isLongPressOverlayActive = overlayState.activeBubble is KeyboardOverlayBubble.LongPressedKey,
                    onLongPressMove = onLongPressMove,
                )
            }
    ) {
        layout.rows.forEach { row ->
            KeyboardRow(
                keysRow = row,
                keyboardState = keyboardState,
                inputState = inputState,
                baseKeyDimensions = viewState.baseKeyDimensions,
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
    keysRow: KeysRow,
    keyboardState: KeyboardState,
    inputState: InputState,
    baseKeyDimensions: BaseKeyDimensions,
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
                inputState = inputState,
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
    inputState: InputState,
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
            inputState = inputState,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun KeyboardKey(
    key: Key,
    keyboardState: KeyboardState,
    inputState: InputState,
    modifier: Modifier = Modifier,
) {
    val isKeyPressed = keyboardState.pressedKeyId == key.id
    val activeModifierForKey = keyboardState.getActiveModifierForKey(key)
    val colors = rememberKeyboardColors(
        background = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        activeBackground = MaterialTheme.colorScheme.primaryContainer
    )

    val backgroundColor = remember(isKeyPressed, activeModifierForKey) {
        if (isKeyPressed || activeModifierForKey != null) colors.activeBackground else colors.background
    }

    val icon = remember(isKeyPressed, activeModifierForKey) {
        key.getIcon(isActive = isKeyPressed, activeModifierForKey = activeModifierForKey)
    }

    val isCustomActionKeyLabel = key.actions.press.command == Actions.Action.CommandType.ACTION
            && key.visual?.actionKeyLabels != null

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
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            isCustomActionKeyLabel -> {
                val text = remember(key.actions.press.command, inputState.actionType) {
                    val actionKeyLabels = key.visual?.actionKeyLabels

                    when (inputState.actionType) {
                        InputState.ActionType.GO -> actionKeyLabels?.go ?: key.visual?.label.orEmpty()
                        InputState.ActionType.NEXT -> actionKeyLabels?.next ?: key.visual?.label.orEmpty()
                        InputState.ActionType.PREVIOUS -> actionKeyLabels?.previous ?: key.visual?.label.orEmpty()
                        InputState.ActionType.SEARCH -> actionKeyLabels?.search ?: key.visual?.label.orEmpty()
                        InputState.ActionType.SEND -> actionKeyLabels?.send ?: key.visual?.label.orEmpty()
                        InputState.ActionType.DONE -> actionKeyLabels?.done ?: key.visual?.label.orEmpty()
                        InputState.ActionType.NONE -> key.visual?.label.orEmpty()
                    }
                }

                // TODO: Calculate text size for all chars basing on settings
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            key.visual?.label != null -> {
                val text = remember(keyboardState.areLettersUppercase) {
                    when {
                        keyboardState.areLettersUppercase && key.type == Key.Type.CHARACTER -> {
                            key.visual.label.uppercase()
                        }

                        else -> key.visual.label
                    }
                }

                // TODO: Calculate text size for all chars basing on settings
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            else -> {
                logger.e { "No visual to display for key $key: visual=${key.visual}" }
            }
        }
    }
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