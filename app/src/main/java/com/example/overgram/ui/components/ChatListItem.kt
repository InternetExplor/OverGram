package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.R
import com.example.overgram.ui.theme.AccentBright
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.ChatName
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.ListDate
import com.example.overgram.ui.theme.MutedIcon
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.TextSecondary
import com.example.overgram.ui.theme.UnreadCounter
import com.example.overgram.ui.theme.UnreadCounterMuted

/** Delivery state of the caller's own last message, shown as ticks next to the time. */
enum class ListMessageStatus { Sent, Read }

private val AvatarSize = 54.dp
private val RowStartPadding = 10.dp
private val AvatarGap = 12.dp

/** Where the divider (and the text column) starts, as in Telegram. */
private val TextStart = RowStartPadding + AvatarSize + AvatarGap

/**
 * One chat in the list, Telegram layout: a 54 dp avatar, then name + time on the first line and
 * the message preview + unread counter on the second, with a hairline divider under the text.
 *
 * @param isPreviewHighlighted Shows [lastMessage] in the accent blue (e.g. "typing…").
 * @param ownMessageStatus Ticks before the time when the last message is the caller's own.
 * @param showDivider False for the last row.
 */
@Composable
fun ChatListItem(
    name: String,
    lastMessage: String,
    time: String,
    modifier: Modifier = Modifier,
    unreadCount: Int = 0,
    avatarUrl: String? = null,
    isOnline: Boolean = false,
    isMuted: Boolean = false,
    isPreviewHighlighted: Boolean = false,
    ownMessageStatus: ListMessageStatus? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(start = RowStartPadding, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(name = name, imageUrl = avatarUrl, size = AvatarSize, isOnline = isOnline)
            Spacer(Modifier.width(AvatarGap))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Name (+ mute icon) takes all the free space; ticks and time stay at the end.
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = ChatName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isMuted) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.VolumeOff,
                                contentDescription = stringResource(R.string.chats_muted),
                                tint = MutedIcon,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    if (ownMessageStatus != null) {
                        Icon(
                            imageVector = if (ownMessageStatus == ListMessageStatus.Read) Icons.Filled.DoneAll else Icons.Filled.Done,
                            contentDescription = stringResource(
                                if (ownMessageStatus == ListMessageStatus.Read) R.string.message_status_read else R.string.message_status_sent
                            ),
                            tint = AccentBright,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                    Text(text = time, style = MaterialTheme.typography.labelSmall, color = ListDate)
                }
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isPreviewHighlighted) AccentBright else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (unreadCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        // Muted chats still count, in grey, so they don't compete for attention.
                        UnreadBadge(
                            count = unreadCount,
                            backgroundColor = if (isMuted) UnreadCounterMuted else UnreadCounter
                        )
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = DividerColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = TextStart)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatListItemPreview() {
    OverGramTheme {
        Column(Modifier.background(BackgroundDark)) {
            ChatListItem(
                name = "Даша",
                lastMessage = "Хорошо, спасибо! 😊",
                time = "21:32",
                unreadCount = 3,
                isOnline = true
            )
            ChatListItem(
                name = "Учеба | TUIT",
                lastMessage = "Ибрагим: Кто сделал лабу?",
                time = "20:47",
                unreadCount = 12,
                isMuted = true
            )
            ChatListItem(
                name = "Максим",
                lastMessage = "Вы: Давай завтра",
                time = "19:23",
                ownMessageStatus = ListMessageStatus.Read,
                showDivider = false
            )
        }
    }
}
