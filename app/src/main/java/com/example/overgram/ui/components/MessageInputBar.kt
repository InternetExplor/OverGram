package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Face
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.InputBarShape
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * Message input bar featuring attach icon, text field, emoji button, and round violet send button.
 *
 * @param value Current input text value.
 * @param onValueChange Callback when text value changes.
 * @param onSendClick Callback when send button is clicked.
 * @param onAttachClick Optional callback when attachment icon is clicked.
 * @param onEmojiClick Optional callback when emoji icon is clicked.
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
    placeholderText: String = "Сообщение..."
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BackgroundDark
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onAttachClick?.invoke() }
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach file",
                    tint = TextSecondary
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceDark, InputBarShape),
                placeholder = {
                    Text(
                        text = placeholderText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { onEmojiClick?.invoke() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "Emoji picker",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    disabledContainerColor = SurfaceDark,
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
                modifier = Modifier.size(Dimens.SendButtonSize),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = PrimaryViolet,
                    contentColor = TextPrimary
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send message",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
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
