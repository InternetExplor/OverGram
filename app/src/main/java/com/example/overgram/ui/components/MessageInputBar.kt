package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.R
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.InputBarShape
import com.example.overgram.ui.theme.LocalHazeState
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.PrimaryVioletLight
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary
import com.example.overgram.ui.theme.frosted
import com.example.overgram.ui.theme.glass
import com.example.overgram.ui.theme.hairline

/**
 * Message input bar featuring attach icon, text field, emoji button, and round violet send button.
 *
 * @param value Current input text value.
 * @param onValueChange Callback when text value changes.
 * @param onSendClick Callback when send button is clicked.
 * @param onAttachClick Optional callback when attachment icon is clicked.
 * @param onEmojiClick Optional callback when emoji icon is clicked (toggles the panel).
 * @param isEmojiPanelOpen Shows [EmojiPanel] under the bar, in place of the keyboard.
 * @param onEmojiSelected Called with the emoji tapped in the panel.
 * @param onInputFocused Called when the text field gains focus, e.g. to close the panel.
 * @param placeholderText Hint text displayed when input is empty.
 */
@Composable
fun MessageInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAttachClick: (() -> Unit)? = null,
    onEmojiClick: (() -> Unit)? = null,
    isEmojiPanelOpen: Boolean = false,
    onEmojiSelected: (String) -> Unit = {},
    onInputFocused: () -> Unit = {},
    placeholderText: String = "Сообщение..."
) {
    Surface(
        // Glass strip when the screen provides a haze state (messages scroll under it).
        modifier = modifier
            .fillMaxWidth()
            .glass(LocalHazeState.current, edge = false)
            .hairline(atBottom = false),
        color = if (LocalHazeState.current != null) Color.Transparent else BackgroundDark
    ) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Optional buttons are hidden rather than shown dead when there is no handler.
                if (onAttachClick != null) {
                    IconButton(onClick = onAttachClick) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = stringResource(R.string.input_attach),
                            tint = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))
                }

                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .frosted(InputBarShape, tint = SurfaceElevatedDark.copy(alpha = 0.6f))
                        .onFocusChanged { if (it.isFocused) onInputFocused() },
                    placeholder = {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    },
                    trailingIcon = onEmojiClick?.let { onClick ->
                        {
                            IconButton(onClick = onClick) {
                                Icon(
                                    imageVector = if (isEmojiPanelOpen) Icons.Default.Keyboard else Icons.Default.Face,
                                    contentDescription = stringResource(if (isEmojiPanelOpen) R.string.input_show_keyboard else R.string.input_emoji),
                                    tint = if (isEmojiPanelOpen) PrimaryViolet else TextSecondary
                                )
                            }
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryViolet
                    ),
                    shape = InputBarShape,
                    singleLine = false,
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank()) {
                                onSendClick()
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(Dimens.SpacingSm))

                IconButton(
                    onClick = onSendClick,
                    modifier = Modifier
                        .size(Dimens.SendButtonSize)
                        .background(SendButtonBrush, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = TextPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.input_send),
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isEmojiPanelOpen) {
                EmojiPanel(onEmojiSelected = onEmojiSelected)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MessageInputBarPreview() {
    OverGramTheme {
        var text by remember { mutableStateOf("Сообщение") }
        MessageInputBar(
            value = text,
            onValueChange = { text = it },
            onSendClick = {}
        )
    }
}

private val SendButtonBrush = Brush.linearGradient(listOf(PrimaryVioletLight, PrimaryViolet))
