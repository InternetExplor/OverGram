package com.example.overgram.ui.components

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.ReceivedBubbleColor
import com.example.overgram.ui.theme.ReceivedBubbleShape
import com.example.overgram.ui.theme.SentBubbleColor
import com.example.overgram.ui.theme.SentBubbleShape
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * Chat message bubble supporting sent and received message variants.
 *
 * @param message Message body text (16sp).
 * @param timestamp Time string (11sp), e.g. "16:45".
 * @param isSent True if sent by current user (right side, violet background), false if received.
 * @param isRead True if message has been read (shows double check mark for sent messages).
 */
@Composable
fun ChatBubble(
    message: String,
    timestamp: String,
    isSent: Boolean,
    modifier: Modifier = Modifier,
    isRead: Boolean = true
) {
    val bubbleShape = if (isSent) SentBubbleShape else ReceivedBubbleShape
    val backgroundColor = if (isSent) SentBubbleColor else ReceivedBubbleColor
    val alignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenPadding, vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.size(4.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSent) Color.White.copy(alpha = 0.7f) else TextSecondary,
                        fontSize = 11.sp
                    )

                    if (isSent) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (isRead) "Read" else "Sent",
                            tint = Color.White.copy(alpha = 0.85f),
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
                    isRead = true
                )
                ChatBubble(
                    message = "Привет! Всё нормально а у тебя?",
                    timestamp = "16:46",
                    isSent = false
                )
                ChatBubble(
                    message = "Тоже нормально, просто хочу кое-что обсудить",
                    timestamp = "16:47",
                    isSent = true,
                    isRead = true
                )
                ChatBubble(
                    message = "Слушаю",
                    timestamp = "16:47",
                    isSent = false
                )
            }
        }
    }
}
