package io.actinis.remote.keyboard.demo

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import co.touchlab.kermit.Logger
import io.actinis.remote.keyboard.data.event.model.KeyboardEvent
import io.actinis.remote.keyboard.data.state.model.InputState
import io.actinis.remote.keyboard.presentation.KeyboardView
import org.jetbrains.compose.ui.tooling.preview.Preview

private const val LOG_TAG = "DemoApp"

private val logger = Logger.withTag(LOG_TAG)

@Composable
@Preview
fun MainApp() {
    MaterialTheme {
        var currentKeyboardType by remember { mutableStateOf(KeyboardType.Text) }
        var textFieldValue by remember { mutableStateOf(TextFieldValue("Click me!")) }
        var showKeyboard by remember { mutableStateOf(true) }

        val onCursorOrSelectionChange: (TextRange) -> Unit = { selection ->
            logger.i { "Cursor position: ${selection.start}, Selection: ${selection.start} to ${selection.end}" }
        }

        val onEditTextClick = {
            logger.i { "EditText click" }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { onEditTextClick() })
                        }
                ) {
                    TextField(
                        value = textFieldValue,
                        onValueChange = { newValue ->
                            textFieldValue = newValue
                            onCursorOrSelectionChange(newValue.selection)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = currentKeyboardType),
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    InputTypeButton("Text", KeyboardType.Text) { currentKeyboardType = it }
                    InputTypeButton("Number", KeyboardType.Number) { currentKeyboardType = it }
                    InputTypeButton("Phone", KeyboardType.Phone) { currentKeyboardType = it }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    InputTypeButton("Password", KeyboardType.Password) { currentKeyboardType = it }
                    InputTypeButton("Email", KeyboardType.Email) { currentKeyboardType = it }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Button(
                        onClick = {
                            showKeyboard = !showKeyboard
                        },
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(4.dp),
                    ) {
                        Text(text = if (showKeyboard) "Hide keyboard" else "Show keyboard")
                    }
                }
            }

            if (showKeyboard) {
                val inputState = when (currentKeyboardType) {
                    KeyboardType.Number -> {
                        InputState.Number(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = false,
                            actionType = InputState.ActionType.DONE,
                        )
                    }

                    KeyboardType.Phone -> {
                        InputState.Phone(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = false,
                            actionType = InputState.ActionType.NEXT,
                        )
                    }

                    KeyboardType.Email -> {
                        InputState.Text(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = false,
                            actionType = InputState.ActionType.SEND,
                            variation = InputState.Text.Variation.EMAIL_ADDRESS,
                        )
                    }

                    KeyboardType.Uri -> {
                        InputState.Text(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = false,
                            actionType = InputState.ActionType.GO,
                            variation = InputState.Text.Variation.URI,
                        )
                    }

                    KeyboardType.Password -> {
                        InputState.Text(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = false,
                            actionType = InputState.ActionType.NONE,
                            isPersonalizedLearningEnabled = false,
                            variation = InputState.Text.Variation.PASSWORD,
                            capitalizationMode = InputState.Text.CapitalizationMode.SENTENCES,
                        )
                    }

                    KeyboardType.NumberPassword -> {
                        InputState.Number(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = false,
                            actionType = InputState.ActionType.DONE,
                            isPersonalizedLearningEnabled = false,
                            variation = InputState.Number.Variation.PASSWORD,
                        )
                    }

                    else -> {
                        InputState.Text(
                            text = textFieldValue.text,
                            selectionStart = textFieldValue.selection.start,
                            selectionEnd = textFieldValue.selection.end,
                            isMultiline = true,
                            actionType = InputState.ActionType.NONE,
                            capitalizationMode = InputState.Text.CapitalizationMode.WORDS,
                        )
                    }
                }

                KeyboardView(
                    inputState = inputState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) { keyboardEvent ->
                    logger.d { "keyboardEvent: $keyboardEvent" }

                    when (keyboardEvent) {
                        KeyboardEvent.ActionClick -> {
                            logger.i { "ActionClick!" }
                        }

                        is KeyboardEvent.TextChange -> {
                            textFieldValue = textFieldValue.copy(
                                text = keyboardEvent.text,
                                selection = TextRange(
                                    start = keyboardEvent.selectionStart,
                                    end = keyboardEvent.selectionEnd,
                                )
                            )
                        }

                        is KeyboardEvent.SizeChanged -> {
                            logger.i { "Keyboard size changed: $keyboardEvent" }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InputTypeButton(label: String, keyboardType: KeyboardType, onClick: (KeyboardType) -> Unit) {
    Button(
        onClick = { onClick(keyboardType) },
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp),
    ) {
        Text(text = label)
    }
}
