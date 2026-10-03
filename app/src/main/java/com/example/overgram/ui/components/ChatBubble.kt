package com.example.overgram.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import com.example.overgram.ui.theme.SenderNameColors
import com.example.overgram.ui.theme.TextPrimary

/**
 * Delivery state of an outgoing message, shown next to the timestamp: a clock, one tick (on the
 * server), two muted ticks (on the other side's device), two bright ticks (read), or an error.
 */
enum class BubbleStatus { Sending, Sent, Delivered, Read, Failed }

/** Tap and long press on a bubble (the long press opens the message menu). */
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.bubbleClicks(onClick: (() -> Unit)?, onLongClick: (() -> Unit)?): Modifier =
    if (onClick == null && onLongClick == null) this
    else combinedClickable(onClick = onClick ?: {}, onLongClick = onLongClick)

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
 * @param reply The message this one answers, quoted above the text.
 * @param onReplyClick Tap on the quote, e.g. to scroll to the original.
 * @param onClick Optional click callback, e.g. to retry a failed message.
 * @param onLongClick Opens the message menu (reply / copy / edit / delete).
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
    reply: QuoteContent? = null,
    onReplyClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
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
                .bubbleClicks(onClick, onLongClick)
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
                    if (reply != null) {
                        MessageQuote(
                            quote = reply,
                            onClick = onReplyClick,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
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
                    BubbleStatus.Delivered, BubbleStatus.Read -> Icons.Default.DoneAll
                    BubbleStatus.Failed -> Icons.Default.ErrorOutline
                },
                contentDescription = stringResource(
                    when (status) {
                        BubbleStatus.Sending -> R.string.message_status_sending
                        BubbleStatus.Sent -> R.string.message_status_sent
                        BubbleStatus.Delivered -> R.string.message_status_delivered
                        BubbleStatus.Read -> R.string.message_status_read
                        BubbleStatus.Failed -> R.string.message_status_failed
                    }
                ),
                tint = when (status) {
                    BubbleStatus.Failed -> ErrorRed
                    // Delivered but not read: muted, like the time; read: the bright check color.
                    BubbleStatus.Sending, BubbleStatus.Delivered -> color
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
                // Normal, sent (one tick)
                ChatBubble(message = "Привет! Как дела?", timestamp = "16:45", isSent = true, status = BubbleStatus.Sent)
                // Delivered (two muted ticks) vs read (two bright ticks)
                ChatBubble(message = "Дошло, но не прочитано", timestamp = "16:45", isSent = true, status = BubbleStatus.Delivered)
                ChatBubble(message = "Прочитано", timestamp = "16:45", isSent = true, status = BubbleStatus.Read)
                // A reply in a group, edited
                ChatBubble(
                    message = "Всё нормально, а у тебя? Давно не виделись, может встретимся на выходных?",
                    timestamp = "16:46",
                    isSent = false,
                    senderName = "Ada Lovelace",
                    senderColor = SenderNameColors[2],
                    reply = QuoteContent("Вы", "Привет! Как дела?", OutgoingCheck),
                    isEdited = true
                )
                // Our reply to someone
                ChatBubble(
                    message = "Давай в субботу",
                    timestamp = "16:47",
                    isSent = true,
                    status = BubbleStatus.Sending,
                    reply = QuoteContent("Ada Lovelace", "Может встретимся на выходных?", OutgoingCheck)
                )
                // Deleted
                ChatBubble(message = "Сообщение удалено", timestamp = "16:47", isSent = false, isPlaceholder = true)
                ChatBubble(message = "Не дошло", timestamp = "16:48", isSent = true, status = BubbleStatus.Failed)
            }
        }
    }
}
