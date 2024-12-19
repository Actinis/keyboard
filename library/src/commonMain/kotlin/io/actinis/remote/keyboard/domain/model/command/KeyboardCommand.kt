package io.actinis.remote.keyboard.domain.model.command

internal sealed interface KeyboardCommand {

    data object DeleteBackward : KeyboardCommand
    data object DeleteWord : KeyboardCommand

    data object Action : KeyboardCommand

    data object ShowLayouts : KeyboardCommand
    data object ManageLayouts : KeyboardCommand

    data class SwitchLayout(
        val layoutId: String,
    ) : KeyboardCommand

    data object ToggleShift : KeyboardCommand

    data object ToggleCapsLock : KeyboardCommand

    data object ShowCursorControls : KeyboardCommand

    data class OutputValue(
        val value: String,
    ) : KeyboardCommand

}