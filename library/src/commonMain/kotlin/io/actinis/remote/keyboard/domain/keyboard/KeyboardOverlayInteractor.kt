package io.actinis.remote.keyboard.domain.keyboard

import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.config.model.action.Actions
import io.actinis.remote.keyboard.data.config.model.key.Key
import io.actinis.remote.keyboard.data.config.model.layout.KeyboardLayout
import io.actinis.remote.keyboard.data.state.model.KeyboardState
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayBubble
import io.actinis.remote.keyboard.domain.model.overlay.KeyboardOverlayState
import io.actinis.remote.keyboard.domain.preferences.PreferencesInteractor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

internal interface KeyboardOverlayInteractor {
    val overlayState: StateFlow<KeyboardOverlayState>

    fun updateSelection(deltaX: Float, deltaY: Float)
    fun updateOverlayState(keyboardState: KeyboardState, keyboardLayout: KeyboardLayout?)
    fun reset()

    fun getCurrentLongPressItem(): KeyboardOverlayBubble.LongPressedKey.Item?
}

internal class KeyboardOverlayInteractorImpl(
    private val preferencesInteractor: PreferencesInteractor,
    private val keyboardStateInteractor: KeyboardStateInteractor,
) : KeyboardOverlayInteractor {
    private val logger = Logger.withTag(LOG_TAG)

    private val _overlayState = MutableStateFlow(KeyboardOverlayState())
    override val overlayState: StateFlow<KeyboardOverlayState> = _overlayState.asStateFlow()

    private var lastLongPressPosition: Pair<Float, Float>? = null

    override fun updateSelection(deltaX: Float, deltaY: Float) {
        val currentState = _overlayState.value
        val currentBubble = currentState.activeBubble as? KeyboardOverlayBubble.LongPressedKey ?: return

        val items = currentBubble.items
        if (items.isEmpty()) return

        val movement = calculateMovement(deltaX, deltaY)
        val (newRow, newColumn) = calculateNewPosition(
            movement = movement,
            currentRow = currentBubble.selectedItemRow,
            currentColumn = currentBubble.selectedItemColumn,
            items = items
        )

        if (newRow != currentBubble.selectedItemRow || newColumn != currentBubble.selectedItemColumn) {
            logger.d {
                "Long-press selected item changed: ${currentBubble.selectedItemRow}x${currentBubble.selectedItemColumn} -> ${newRow}x$newColumn"
            }

            _overlayState.value = currentState.copy(
                activeBubble = currentBubble.copy(
                    selectedItemRow = newRow,
                    selectedItemColumn = newColumn
                )
            )
        }
    }

    // TODO: Refactor this method, it's too long and has many responsibilities
    override fun updateOverlayState(keyboardState: KeyboardState, keyboardLayout: KeyboardLayout?) {
        val isActive = keyboardState.pressedKeyId != null
        if (!isActive) {
            _overlayState.value = KeyboardOverlayState()
            return
        }

        val pressedKeyId = keyboardState.pressedKeyId
        val key = keyboardLayout?.findKey { it.id == pressedKeyId }
        if (key == null) {
            logger.e { "Can not find key with id $pressedKeyId in overlay" }
            _overlayState.value = KeyboardOverlayState()
            return
        }

//        val layouts =

        val showBackground = keyboardState.longPressedKeyId != null
        val popupOnLongPress = key.actions.longPress?.popup == true
        val isLongPressed = keyboardState.longPressedKeyId == pressedKeyId

        val bubble = when {
            isLongPressed && popupOnLongPress -> {
                key.actions.longPress?.let { longPressAction ->
                    val hasValues = longPressAction.values.isNotEmpty()
                    val commandType = longPressAction.command

                    if (commandType == null) {
                        logger.w { "Key $key doesn't have long press command" }
                        return@let null
                    }

                    val itemsPerRow = when (commandType) {
                        Actions.Action.CommandType.SHOW_LAYOUTS -> SHOW_LAYOUTS_ITEMS_PER_ROW
                        else -> DEFAULT_ITEMS_PER_ROW
                    }

                    val values = if (hasValues) {
                        longPressAction.values.map { PopupValue(id = it, text = it) }
                    } else {
                        when (commandType) {
                            Actions.Action.CommandType.SHOW_LAYOUTS -> {
                                logger.w { "Available layouts: ${preferencesInteractor.availableKeyboardLayouts.value}" }
                                val currentLayoutId = keyboardStateInteractor.currentLayout.value?.metadata?.id

                                preferencesInteractor.availableKeyboardLayouts.value
                                    .asSequence()
                                    .filter { it.isEnabled }
                                    .map { PopupValue(id = it.id, text = it.name) }
                                    .sortedBy { it.id == currentLayoutId } // Current layout should be last
                                    .toList()

                                // TODO: Uncomment when layouts management will be ready
//                                    .let { values ->
//                                        listOf(
//                                            PopupValue(
//                                                id = KeyboardOverlayBubble.LongPressedKey.Item.MANAGE_KEYBOARD_LAYOUTS_ID,
//                                                text = "Manage" // FIXME: Replace with a string
//                                            )
//                                        ) + values
//                                    }
                            }

                            else -> emptyList()
                        }
                    }

                    if (values.isEmpty()) {
                        logger.w { "Key $key doesn't have long press values" }
                        return@let null
                    }

                    val items = values
                        .chunked(itemsPerRow)
                        .map { chunk ->
                            chunk.map { value ->
                                KeyboardOverlayBubble.LongPressedKey.Item(
                                    id = value.id,
                                    text = value.text,
                                )
                            }
                        }

                    val (selectedItemRow, selectedItemColumn) = when (commandType) {
                        Actions.Action.CommandType.SHOW_LAYOUTS -> {
                            items.count() - 1 to 0
                        }

                        else -> 0 to 0
                    }

                    KeyboardOverlayBubble.LongPressedKey(
                        items = items,
                        selectedItemRow = selectedItemRow,
                        selectedItemColumn = selectedItemColumn,
                    )
                }
            }

            key.type == Key.Type.CHARACTER -> {
                if (key.visual?.label != null) {
                    KeyboardOverlayBubble.PressedKey(
                        text = key.visual.label,
                    )
                } else {
                    logger.e { "Key $key doesn't have visual label to display, visual=${key.visual}" }
                    null
                }

            }

            else -> {
                logger.w {
                    "Unknown key state: key=$key, isLongPressed=$isLongPressed, popupOnLongPress=$popupOnLongPress"
                }
                null
            }
        }

        _overlayState.value = KeyboardOverlayState(
            isActive = isActive,
            showBackground = showBackground,
            activeBubble = bubble,
        )
    }

    private fun calculateMovement(deltaX: Float, deltaY: Float): Movement {
        val (lastX, lastY) = lastLongPressPosition ?: run {
            lastLongPressPosition = 0f to 0f
            return Movement.None
        }

        val currentDeltaX = deltaX - lastX
        val currentDeltaY = deltaY - lastY

        return when {
            abs(currentDeltaX) > abs(currentDeltaY) -> {
                if (abs(currentDeltaX) >= LONG_PRESS_OVERLAY_MOVEMENT_THRESHOLD_PX) {
                    if (currentDeltaX > 0) Movement.Right(deltaX, deltaY) else Movement.Left(deltaX, deltaY)
                } else Movement.None
            }

            abs(currentDeltaY) >= LONG_PRESS_OVERLAY_MOVEMENT_THRESHOLD_PX -> {
                if (currentDeltaY > 0) Movement.Down(deltaX, deltaY) else Movement.Up(deltaX, deltaY)
            }

            else -> Movement.None
        }
    }

    private fun calculateNewPosition(
        movement: Movement,
        currentRow: Int,
        currentColumn: Int,
        items: List<List<KeyboardOverlayBubble.LongPressedKey.Item>>,
    ): Selection {
        return when (movement) {
            is Movement.Left -> {
                val newColumn = (currentColumn - 1).coerceIn(0, items[currentRow].size - 1)
                if (newColumn != currentColumn) {
                    lastLongPressPosition = movement.x to movement.y
                }
                Selection(currentRow, newColumn)
            }

            is Movement.Right -> {
                val newColumn = (currentColumn + 1).coerceIn(0, items[currentRow].size - 1)
                if (newColumn != currentColumn) {
                    lastLongPressPosition = movement.x to movement.y
                }
                Selection(currentRow, newColumn)
            }

            is Movement.Up -> {
                val newRow = (currentRow - 1).coerceIn(0, items.size - 1)
                val maxColumn = items[newRow].size - 1
                val newColumn = currentColumn.coerceIn(0, maxColumn)
                if (newRow != currentRow) {
                    lastLongPressPosition = movement.x to movement.y
                }
                Selection(newRow, newColumn)
            }

            is Movement.Down -> {
                val newRow = (currentRow + 1).coerceIn(0, items.size - 1)
                val maxColumn = items[newRow].size - 1
                val newColumn = currentColumn.coerceIn(0, maxColumn)
                if (newRow != currentRow) {
                    lastLongPressPosition = movement.x to movement.y
                }
                Selection(newRow, newColumn)
            }

            Movement.None -> Selection(currentRow, currentColumn)
        }
    }


    override fun reset() {
        lastLongPressPosition = null
        _overlayState.value = KeyboardOverlayState()
    }

    override fun getCurrentLongPressItem(): KeyboardOverlayBubble.LongPressedKey.Item? {
        val activeBubble = overlayState.value.activeBubble as? KeyboardOverlayBubble.LongPressedKey
            ?: return null

        return activeBubble.items[activeBubble.selectedItemRow][activeBubble.selectedItemColumn]
    }

    private sealed class Movement {
        data object None : Movement()

        data class Left(
            val x: Float,
            val y: Float,
        ) : Movement()

        data class Right(
            val x: Float,
            val y: Float,
        ) : Movement()

        data class Up(
            val x: Float,
            val y: Float,
        ) : Movement()

        data class Down(
            val x: Float,
            val y: Float,
        ) : Movement()
    }

    private data class Selection(
        val row: Int,
        val column: Int,
    )

    private data class PopupValue(
        val id: String,
        val text: String,
    )

    private companion object {
        private const val LOG_TAG = "KeyboardOverlayInteractor"

        // TODO: Should be density-dependent
        private const val LONG_PRESS_OVERLAY_MOVEMENT_THRESHOLD_PX = 30f

        private const val DEFAULT_ITEMS_PER_ROW = 4
        private const val SHOW_LAYOUTS_ITEMS_PER_ROW = 1
    }
}