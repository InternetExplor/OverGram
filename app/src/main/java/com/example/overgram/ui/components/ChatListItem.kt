package com.example.overgram.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * Single item row in the chats list.
 *
 * @param name Chat / contact title.
 * @param lastMessage Text preview of the last message (14sp).
 * @param time Timestamp string (11sp), e.g. "21:32".
 * @param unreadCount Number of unread messages (if > 0, displays UnreadBadge).
 * @param avatarUrl Optional avatar image URL.
 * @param isOnline Whether to display online indicator dot on the avatar.
 * @param isMuted Shows a muted icon and a grey unread badge.
 * @param onClick Click callback when tapping the chat item.
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
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                name = name,
                imageUrl = avatarUrl,
                size = Dimens.AvatarMedium,
                isOnline = isOnline
            )

            Spacer(modifier = Modifier.width(Dimens.SpacingLg))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isMuted) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = "Muted",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingSm))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Muted chats still count unread, but without drawing attention.
                    UnreadBadge(
                        count = unreadCount,
                        backgroundColor = if (isMuted) SurfaceElevatedDark else PrimaryViolet
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatListItemPreview() {
    OverGramTheme {
        Surface(color = BackgroundDark) {
            Column {
                ChatListItem(
                    name = "ChatGPT",
                    lastMessage = "Хорошо, понял! Если будут ещё вопросы — обращайся.",
                    time = "21:32",
                    unreadCount = 3,
                    isOnline = true
                )
                ChatListItem(
                    name = "Максим",
                    lastMessage = "Давай завтра встретимся?",
                    time = "20:47",
                    unreadCount = 1
                )
                ChatListItem(
                    name = "Артём",
                    lastMessage = "Скинул тебе файл",
                    time = "19:23",
                    unreadCount = 2
                )
            }
        }
    }
}
