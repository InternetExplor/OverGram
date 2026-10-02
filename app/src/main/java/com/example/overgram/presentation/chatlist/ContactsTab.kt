package com.example.overgram.presentation.chatlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.overgram.R
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.GlassScreen
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * People you have a direct chat with (Relay has no address book): online first, then by name.
 * Tapping one opens that chat; "Find people" searches by username.
 */
@Composable
fun ContactsTab(
    chats: List<ChatSummary>,
    isLive: Boolean,
    currentUserId: String?,
    onChatClick: (ChatSummary) -> Unit,
    onFindPeople: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val contacts = remember(chats, isLive) {
        chats.filter { it.type == ChatType.DIRECT && it.peer != null && it.peer.id != currentUserId }
            .distinctBy { it.peer?.id }
            .sortedWith(
                compareByDescending<ChatSummary> { isLive && it.peer?.isOnline == true }
                    .thenBy { it.peer?.displayName?.lowercase() }
            )
    }

    GlassScreen(
        topBar = { OverGramTopBar(title = stringResource(R.string.tab_contacts)) },
        bottomBar = bottomBar
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item(key = "find") {
                FindPeopleRow(onClick = onFindPeople)
                HorizontalDivider(color = DividerColor)
            }
            if (contacts.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.contacts_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.SpacingXxl)
                    )
                }
            }
            items(contacts, key = { it.id }) { chat ->
                val peer = chat.peer ?: return@items
                ContactRow(peer = peer, isLive = isLive, onClick = { onChatClick(chat) })
            }
        }
    }
}

@Composable
private fun FindPeopleRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.AvatarMedium)
                .background(PrimaryViolet, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PersonSearch, contentDescription = null, tint = TextPrimary)
        }
        Spacer(Modifier.width(Dimens.SpacingLg))
        Text(
            text = stringResource(R.string.contacts_find_people),
            style = MaterialTheme.typography.titleMedium,
            color = PrimaryViolet
        )
    }
}

@Composable
private fun ContactRow(peer: UserProfile, isLive: Boolean, onClick: () -> Unit) {
    val online = isLive && peer.isOnline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = peer.displayName,
            imageUrl = mediaUrl(peer.avatarMediaId),
            size = Dimens.AvatarMedium,
            isOnline = online
        )
        Spacer(Modifier.width(Dimens.SpacingLg))
        Column(Modifier.weight(1f)) {
            Text(
                text = peer.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                // Offline the last known presence is stale: show the username instead.
                text = if (isLive) presenceText(peer) else peer.username?.let { "@$it" }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = if (online) PrimaryViolet else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContactsTabPreview() {
    val ada = UserProfile("u1", "Ada Lovelace", "ada", null, isOnline = true, lastSeenAt = null)
    val bob = UserProfile("u2", "Bob", "bob", null, isOnline = false, lastSeenAt = System.currentTimeMillis() - 600_000)
    OverGramTheme {
        ContactsTab(
            chats = listOf(
                ChatSummary("1", ChatType.DIRECT, null, bob, null, null, 0, 0, false),
                ChatSummary("2", ChatType.DIRECT, null, ada, null, null, 0, 0, false)
            ),
            isLive = true,
            currentUserId = "me",
            onChatClick = {},
            onFindPeople = {}
        )
    }
}
