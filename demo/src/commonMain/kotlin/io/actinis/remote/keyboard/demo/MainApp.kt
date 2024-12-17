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
import io.actinis.remote.keyboard.data.state.model.InputType
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
                            // Update the state
                            textFieldValue = newValue
                            // Trigger the callback with the new selection
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
                var isPassword = false

                val keyboardInputType = when (currentKeyboardType) {
                    KeyboardType.Number -> InputType.NUMERIC
                    KeyboardType.Phone -> InputType.PHONE
                    KeyboardType.Email -> InputType.EMAIL
                    KeyboardType.Uri -> InputType.URL
                    KeyboardType.Password -> {
                        isPassword = true
                        InputType.TEXT
                    }

                    KeyboardType.NumberPassword -> {
                        isPassword = true
                        InputType.NUMERIC
                    }

                    else -> InputType.TEXT
                }

                KeyboardView(
                    inputType = keyboardInputType,
                    isPassword = isPassword,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) { keyboardEvent ->
                    logger.d { "keyboardEvent: $keyboardEvent" }

                    when (keyboardEvent) {
                        KeyboardEvent.ActionClick -> {
                        }

                        KeyboardEvent.Backspace -> {
                            if (textFieldValue.text.isNotEmpty()) {
                                textFieldValue = if (textFieldValue.selection.length > 0) {
                                    // If there's selected text, remove the selection
                                    val start = textFieldValue.selection.min
                                    val end = textFieldValue.selection.max
                                    val newText = textFieldValue.text.removeRange(start, end)
                                    TextFieldValue(
                                        text = newText,
                                        selection = TextRange(start)
                                    )
                                } else {
                                    // If no selection, remove character before cursor
                                    val cursorPosition = textFieldValue.selection.start
                                    if (cursorPosition > 0) {
                                        val newText = textFieldValue.text.removeRange(cursorPosition - 1, cursorPosition)
                                        TextFieldValue(
                                            text = newText,
                                            selection = TextRange(cursorPosition - 1)
                                        )
                                    } else {
                                        textFieldValue
                                    }
                                }
                            }
                        }

                        is KeyboardEvent.TextInput -> {
                            val start = textFieldValue.selection.min
                            val end = textFieldValue.selection.max
                            val newText = if (start != end) {
                                // Replace selected text with input
                                textFieldValue.text.replaceRange(start, end, keyboardEvent.text)
                            } else {
                                // Insert text at cursor position
                                textFieldValue.text.substring(0, start) +
                                        keyboardEvent.text +
                                        textFieldValue.text.substring(start)
                            }
                            val newCursorPosition = start + keyboardEvent.text.length
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newCursorPosition)
                            )
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
