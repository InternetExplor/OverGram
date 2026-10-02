package com.example.overgram.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.R
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.InputPanel
import com.example.overgram.ui.theme.InputPanelIcon
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.SendBlue
import com.example.overgram.ui.theme.TextPrimary

/**
 * Telegram's message panel: a flat strip (`chat_messagePanelBackground`) with the emoji button on
 * the left, a borderless field, the attach clip and a plain send arrow on the right — blue when
 * there's something to send, grey otherwise.
 *
 * @param onAttachClick Shown only when set (no dead buttons).
 * @param onEmojiClick Toggles the emoji panel; shown only when set.
 * @param isEmojiPanelOpen Shows [EmojiPanel] under the bar, in place of the keyboard.
 * @param onEmojiSelected Called with the emoji tapped in the panel.
 * @param onInputFocused Called when the field gains focus, e.g. to close the panel.
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
    placeholderText: String = ""
) {
    val canSend = value.isNotBlank()
    Surface(modifier = modifier.fillMaxWidth(), color = InputPanel) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onEmojiClick != null) {
                    IconButton(onClick = onEmojiClick) {
                        Icon(
                            imageVector = if (isEmojiPanelOpen) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                            contentDescription = stringResource(
                                if (isEmojiPanelOpen) R.string.input_show_keyboard else R.string.input_emoji
                            ),
                            tint = InputPanelIcon
                        )
                    }
                }

                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { if (it.isFocused) onInputFocused() },
                    placeholder = {
                        Text(
                            text = placeholderText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = InputPanelIcon
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent
                    ),
                    maxLines = 6,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    keyboardActions = KeyboardActions(onSend = { if (canSend) onSendClick() })
                )

                if (onAttachClick != null) {
                    IconButton(onClick = onAttachClick) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = stringResource(R.string.input_attach),
                            tint = InputPanelIcon
                        )
                    }
                }

                IconButton(onClick = onSendClick, enabled = canSend) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.input_send),
                        tint = if (canSend) SendBlue else InputPanelIcon.copy(alpha = 0.5f)
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
        var text by remember { mutableStateOf("Привет") }
        MessageInputBar(
            value = text,
            onValueChange = { text = it },
            onSendClick = {},
            onEmojiClick = {},
            placeholderText = "Message"
        )
    }
}
