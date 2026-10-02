package com.example.overgram.presentation.groupinfo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.R
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.chatlist.ChatListScreen
import com.example.overgram.presentation.chatlist.presenceText
import com.example.overgram.presentation.newchat.SelectMembersScreen
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/** Group name, member list (owner can remove people), add members, leave. */
data class GroupInfoScreen(val chatId: String, val title: String) : Screen {

    override val key: ScreenKey = "group-info:$chatId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = hiltViewModel<GroupInfoViewModel, GroupInfoViewModel.Factory>(
            key = key,
            creationCallback = { factory -> factory.create(chatId, title) }
        )
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleStartEffect(viewModel) {
            viewModel.refresh()
            onStopOrDispose { }
        }
        LaunchedEffect(state.hasLeft) {
            if (state.hasLeft) navigator.popUntil { it is ChatListScreen }
        }
        LaunchedEffect(state.isSessionEnded) {
            if (state.isSessionEnded) navigator.replaceAll(PhoneEntryScreen)
        }

        GroupInfoContent(
            state = state,
            onBack = { navigator.pop() },
            onAddMembers = { navigator.push(SelectMembersScreen(addToChatId = chatId)) },
            onRemove = viewModel::askToRemove,
            onRetry = viewModel::refresh,
            onLeave = viewModel::leave,
            onErrorShown = viewModel::onErrorShown
        )

        state.confirmRemoval?.let { user ->
            AlertDialog(
                onDismissRequest = viewModel::dismissRemoval,
                containerColor = SurfaceDark,
                title = { Text(stringResource(R.string.group_remove_title), color = TextPrimary) },
                text = {
                    Text(stringResource(R.string.group_remove_text, user.displayName, state.title), color = TextSecondary)
                },
                confirmButton = {
                    TextButton(onClick = viewModel::confirmRemoval) {
                        Text(stringResource(R.string.group_remove_confirm), color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissRemoval) {
                        Text(stringResource(R.string.action_cancel), color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun GroupInfoContent(
    state: GroupInfoUiState,
    onBack: () -> Unit,
    onAddMembers: () -> Unit,
    onRemove: (UserProfile) -> Unit,
    onRetry: () -> Unit,
    onLeave: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = state.error?.let { authErrorMessage(it) }
    LaunchedEffect(state.error) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onErrorShown()
        }
    }
    var isLeaveDialogOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = { OverGramTopBar(title = stringResource(R.string.group_info_title), onBackClick = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item(key = "header") { Header(state) }
            item(key = "add") {
                ActionRow(
                    icon = Icons.Default.PersonAdd,
                    text = stringResource(R.string.chat_add_members),
                    color = PrimaryViolet,
                    onClick = onAddMembers
                )
                HorizontalDivider(color = DividerColor)
            }

            when {
                state.isLoading && state.members.isEmpty() -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(Dimens.SpacingXl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryViolet)
                    }
                }
                state.loadError != null -> item(key = "error") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(Dimens.SpacingXl),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            authErrorMessage(state.loadError),
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.chats_retry), color = PrimaryViolet)
                        }
                    }
                }
                else -> {
                    item(key = "members-header") {
                        Text(
                            text = pluralStringResource(R.plurals.group_members_count, state.memberCount, state.memberCount),
                            style = MaterialTheme.typography.labelLarge,
                            color = PrimaryViolet,
                            modifier = Modifier.padding(
                                start = Dimens.ScreenPadding,
                                end = Dimens.ScreenPadding,
                                top = Dimens.SpacingLg,
                                bottom = Dimens.SpacingXs
                            )
                        )
                    }
                    items(state.members, key = { it.id }) { member ->
                        MemberRow(
                            member = member,
                            isMe = member.id == state.currentUserId,
                            isOwner = member.id == state.ownerId,
                            canRemove = state.isOwner && member.id != state.currentUserId,
                            isRemoving = state.removingUserId == member.id,
                            isLive = state.isLive,
                            onRemove = { onRemove(member) }
                        )
                    }
                    if (!state.isComplete) {
                        item(key = "incomplete") {
                            Text(
                                text = stringResource(R.string.group_members_incomplete),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingSm)
                            )
                        }
                    }
                }
            }

            item(key = "leave") {
                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(top = Dimens.SpacingSm))
                ActionRow(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    text = stringResource(R.string.chat_leave_group),
                    color = ErrorRed,
                    onClick = { isLeaveDialogOpen = true }
                )
            }
        }
    }

    if (isLeaveDialogOpen) {
        AlertDialog(
            onDismissRequest = { isLeaveDialogOpen = false },
            containerColor = SurfaceDark,
            title = { Text(stringResource(R.string.chat_leave_confirm_title), color = TextPrimary) },
            text = { Text(stringResource(R.string.chat_leave_confirm_text, state.title), color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        isLeaveDialogOpen = false
                        onLeave()
                    },
                    enabled = !state.isLeaving
                ) {
                    Text(stringResource(R.string.chat_leave_group), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { isLeaveDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun Header(state: GroupInfoUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingXl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Avatar(name = state.title, size = Dimens.AvatarExtraLarge)
        Spacer(Modifier.height(Dimens.SpacingMd))
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
        )
        if (!state.isLoading || state.members.isNotEmpty()) {
            val online = if (state.isLive) state.members.count { it.isOnline } else 0
            Text(
                text = buildString {
                    append(pluralStringResource(R.plurals.group_members_count, state.memberCount, state.memberCount))
                    if (online > 0) append(", ").append(stringResource(R.string.group_online_count, online))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, text: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingLg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(Modifier.width(Dimens.SpacingXl))
        Text(text, style = MaterialTheme.typography.titleMedium, color = color)
    }
}

@Composable
private fun MemberRow(
    member: UserProfile,
    isMe: Boolean,
    isOwner: Boolean,
    canRemove: Boolean,
    isRemoving: Boolean,
    isLive: Boolean,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, end = Dimens.SpacingSm, top = Dimens.SpacingSm, bottom = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val showOnline = isLive && member.isOnline
        Avatar(name = member.displayName, size = Dimens.AvatarMedium, isOnline = showOnline)
        Spacer(Modifier.width(Dimens.SpacingLg))
        Column(Modifier.weight(1f)) {
            Text(
                text = if (isMe) stringResource(R.string.group_member_you, member.displayName) else member.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                // Without a connection presence is stale: show just the username.
                text = listOfNotNull(member.username?.let { "@$it" }, if (isLive) presenceText(member) else null)
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = if (showOnline) PrimaryViolet else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isOwner) {
            Text(
                text = stringResource(R.string.group_role_owner),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryViolet,
                modifier = Modifier
                    .padding(horizontal = Dimens.SpacingSm)
                    .background(SurfaceElevatedDark, RoundedCornerShape(8.dp))
                    .padding(horizontal = Dimens.SpacingSm, vertical = 2.dp)
            )
        }
        when {
            isRemoving -> CircularProgressIndicator(
                color = PrimaryViolet,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(Dimens.SpacingMd)
                    .size(20.dp)
            )
            canRemove -> IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.PersonRemove,
                    contentDescription = stringResource(R.string.group_remove_member, member.displayName),
                    tint = TextSecondary
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GroupInfoContentPreview() {
    val me = UserProfile("me", "Tadashi", "tadashi", null, isOnline = true, lastSeenAt = null)
    val ada = UserProfile("u1", "Ada Lovelace", "ada", null, isOnline = true, lastSeenAt = null)
    val bob = UserProfile("u2", "Bob", "bob", null, isOnline = false, lastSeenAt = null)
    OverGramTheme {
        GroupInfoContent(
            state = GroupInfoUiState(
                title = "Учеба | TUIT",
                currentUserId = "me",
                ownerId = "me",
                members = listOf(me, ada, bob),
                isLoading = false
            ),
            onBack = {}, onAddMembers = {}, onRemove = {}, onRetry = {}, onLeave = {}, onErrorShown = {}
        )
    }
}

