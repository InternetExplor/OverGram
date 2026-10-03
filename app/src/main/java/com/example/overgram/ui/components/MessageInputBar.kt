package com.example.overgram.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
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
 * What the next Send does differently, shown as a strip above the field: answer [quote]'s message
 * ([icon] = reply arrow) or save an edit of it ([icon] = pencil). ✕ calls [onDismiss].
 */
data class InputContext(
    val icon: ImageVector,
    val quote: QuoteContent,
    val onDismiss: () -> Unit
)

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
 * @param context The reply / edit strip; null for a plain new message.
 * @param focusRequester Lets the screen focus the field (e.g. when Reply is chosen).
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
    placeholderText: String = "",
    context: InputContext? = null,
    focusRequester: FocusRequester? = null
) {
    val canSend = value.isNotBlank()
    Surface(modifier = modifier.fillMaxWidth(), color = InputPanel) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
            if (context != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(context.icon, contentDescription = null, tint = Accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    MessageQuote(quote = context.quote, modifier = Modifier.weight(1f))
                    IconButton(onClick = context.onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_cancel),
                            tint = InputPanelIcon
                        )
                    }
                }
            }
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
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
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

@Preview(showBackground = true)
@Composable
fun MessageInputBarReplyPreview() {
    OverGramTheme {
        MessageInputBar(
            value = "",
            onValueChange = {},
            onSendClick = {},
            onAttachClick = {},
            placeholderText = "Message",
            context = InputContext(
                icon = Icons.AutoMirrored.Filled.Reply,
                quote = QuoteContent("Ada Lovelace", "Может встретимся на выходных?"),
                onDismiss = {}
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MessageInputBarEditPreview() {
    OverGramTheme {
        MessageInputBar(
            value = "Давай в субботу",
            onValueChange = {},
            onSendClick = {},
            placeholderText = "Message",
            context = InputContext(
                icon = Icons.Default.Edit,
                quote = QuoteContent("Редактирование", "Давай в субботу"),
                onDismiss = {}
            )
        )
    }
}
