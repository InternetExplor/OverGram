package com.example.overgram.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overgram.R
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryVioletLight
import com.example.overgram.ui.theme.ReceivedBubbleColor
import com.example.overgram.ui.theme.ReceivedBubbleShape
import com.example.overgram.ui.theme.SentBubbleColor
import com.example.overgram.ui.theme.SentBubbleShape
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/** Delivery state of an outgoing message, shown as an icon next to the timestamp. */
enum class BubbleStatus { Sending, Sent, Read, Failed }

/**
 * Chat message bubble supporting sent and received message variants.
 *
 * @param message Message body text (16sp).
 * @param timestamp Time string (11sp), e.g. "16:45".
 * @param isSent True if sent by current user (right side, violet background), false if received.
 * @param status Delivery state icon for sent messages.
 * @param isEdited Shows an "edited" label before the timestamp.
 * @param isPlaceholder Renders [message] as a muted italic note (e.g. "Message deleted").
 * @param senderName Shown above the text, for incoming messages in group chats.
 * @param senderColor Colour of [senderName].
 * @param onClick Optional click callback, e.g. to retry a failed message.
 */
@Composable
fun ChatBubble(
    message: String,
    timestamp: String,
    isSent: Boolean,
    modifier: Modifier = Modifier,
    status: BubbleStatus = BubbleStatus.Read,
    isEdited: Boolean = false,
    isPlaceholder: Boolean = false,
    senderName: String? = null,
    senderColor: Color = PrimaryVioletLight,
    onClick: (() -> Unit)? = null
) {
    val bubbleShape = if (isSent) SentBubbleShape else ReceivedBubbleShape
    val backgroundColor = if (isSent) SentBubbleColor else ReceivedBubbleColor
    val alignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    val metaColor = if (isSent) Color.White.copy(alpha = 0.7f) else TextSecondary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                if (senderName != null) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = senderColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.size(2.dp))
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isPlaceholder) metaColor else TextPrimary,
                    fontStyle = if (isPlaceholder) FontStyle.Italic else FontStyle.Normal
                )

                Spacer(modifier = Modifier.size(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (isEdited) {
                        Text(
                            text = stringResource(R.string.message_edited),
                            style = MaterialTheme.typography.labelSmall,
                            color = metaColor,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = metaColor,
                        fontSize = 11.sp
                    )

                    if (isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (status) {
                                BubbleStatus.Sending -> Icons.Default.Schedule
                                BubbleStatus.Sent -> Icons.Default.Done
                                BubbleStatus.Read -> Icons.Default.DoneAll
                                BubbleStatus.Failed -> Icons.Default.ErrorOutline
                            },
                            contentDescription = stringResource(
                                when (status) {
                                    BubbleStatus.Sending -> R.string.message_status_sending
                                    BubbleStatus.Sent -> R.string.message_status_sent
                                    BubbleStatus.Read -> R.string.message_status_read
                                    BubbleStatus.Failed -> R.string.message_status_failed
                                }
                            ),
                            tint = if (status == BubbleStatus.Failed) ErrorRed else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatBubblePreview() {
    OverGramTheme {
        Surface(color = BackgroundDark) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                ChatBubble(
                    message = "Привет! Как дела?",
                    timestamp = "16:45",
                    isSent = true,
                    status = BubbleStatus.Read
                )
                ChatBubble(
                    message = "Привет! Всё нормально а у тебя?",
                    timestamp = "16:46",
                    isSent = false,
                    isEdited = true
                )
                ChatBubble(
                    message = "Тоже нормально, просто хочу кое-что обсудить",
                    timestamp = "16:47",
                    isSent = true,
                    status = BubbleStatus.Sending
                )
                ChatBubble(
                    message = "Message deleted",
                    timestamp = "16:47",
                    isSent = false,
                    isPlaceholder = true
                )
                ChatBubble(
                    message = "Не дошло",
                    timestamp = "16:48",
                    isSent = true,
                    status = BubbleStatus.Failed
                )
            }
        }
    }
}
