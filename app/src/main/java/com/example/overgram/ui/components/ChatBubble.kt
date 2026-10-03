package com.example.overgram.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overgram.R
import com.example.overgram.ui.theme.AccentBright
import com.example.overgram.ui.theme.ChatWallpaper
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.IncomingMeta
import com.example.overgram.ui.theme.OutgoingCheck
import com.example.overgram.ui.theme.OutgoingMeta
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.ReceivedBubbleColor
import com.example.overgram.ui.theme.ReceivedBubbleShape
import com.example.overgram.ui.theme.SentBubbleColor
import com.example.overgram.ui.theme.SentBubbleShape
import com.example.overgram.ui.theme.TextPrimary

/** Delivery state of an outgoing message, shown as an icon next to the timestamp. */
enum class BubbleStatus { Sending, Sent, Read, Failed }

internal val MetaSize = 12.sp

/**
 * A message bubble, Telegram style: flat colors from the theme (`chat_outBubble` /
 * `chat_inBubble`) and the time + ticks tucked into the bottom-right corner on the same line as
 * the end of the text when it fits (the text reserves that space with an invisible copy of the
 * meta), instead of on a line of their own.
 *
 * @param status Delivery ticks for sent messages.
 * @param isEdited Shows "edited" before the time.
 * @param isPlaceholder Renders [message] as a muted italic note (e.g. "Message deleted").
 * @param senderName Shown above the text, for incoming messages in group chats.
 * @param senderColor Color of [senderName].
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
    senderColor: Color = AccentBright,
    onClick: (() -> Unit)? = null
) {
    val metaColor = if (isSent) OutgoingMeta else IncomingMeta
    val edited = if (isEdited) stringResource(R.string.message_edited) + " " else ""
    // What the meta row will show; reserved (transparently) at the end of the text.
    val metaText = edited + timestamp + if (isSent) "  " else ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = if (isSent) SentBubbleShape else ReceivedBubbleShape,
            color = if (isSent) SentBubbleColor else ReceivedBubbleColor,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        ) {
            Box(Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)) {
                Column {
                    if (senderName != null) {
                        Text(
                            text = senderName,
                            style = MaterialTheme.typography.titleSmall,
                            color = senderColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            append(message)
                            withStyle(SpanStyle(color = Color.Transparent, fontSize = MetaSize)) {
                                append("  $metaText")
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isPlaceholder) metaColor else TextPrimary,
                        fontStyle = if (isPlaceholder) FontStyle.Italic else FontStyle.Normal
                    )
                }
                BubbleMeta(
                    text = edited + timestamp,
                    isSent = isSent,
                    status = status,
                    color = metaColor,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 1.dp)
                )
            }
        }
    }
}

/** The text a bubble's meta shows, so captions can reserve room for it at the end of their last line. */
@Composable
internal fun bubbleMetaText(timestamp: String, isEdited: Boolean, isSent: Boolean): String {
    val edited = if (isEdited) stringResource(R.string.message_edited) + " " else ""
    return edited + timestamp + if (isSent) "  " else ""
}

/** "edited 16:45 ✓✓": time and delivery ticks, in the bottom-right corner of every bubble. */
@Composable
internal fun BubbleMeta(
    text: String,
    isSent: Boolean,
    status: BubbleStatus,
    color: Color,
    modifier: Modifier = Modifier,
    checkColor: Color = OutgoingCheck
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontSize = MetaSize,
            color = color
        )
        if (isSent) {
            Spacer(Modifier.width(3.dp))
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
                tint = when (status) {
                    BubbleStatus.Failed -> ErrorRed
                    BubbleStatus.Sending -> color
                    else -> checkColor
                },
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatBubblePreview() {
    OverGramTheme {
        Surface(color = ChatWallpaper) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                ChatBubble(message = "Привет! Как дела?", timestamp = "16:45", isSent = true, status = BubbleStatus.Read)
                ChatBubble(
                    message = "Привет! Всё нормально, а у тебя? Давно не виделись, может встретимся на выходных?",
                    timestamp = "16:46",
                    isSent = false,
                    senderName = "Ada Lovelace",
                    senderColor = Color(0xFFB9A2F5),
                    isEdited = true
                )
                ChatBubble(message = "Ок 👍", timestamp = "16:47", isSent = true, status = BubbleStatus.Sending)
                ChatBubble(message = "Message deleted", timestamp = "16:47", isSent = false, isPlaceholder = true)
                ChatBubble(message = "Не дошло", timestamp = "16:48", isSent = true, status = BubbleStatus.Failed)
            }
        }
    }
}
