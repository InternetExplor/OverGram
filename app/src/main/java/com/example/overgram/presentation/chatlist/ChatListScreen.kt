package com.example.overgram.presentation.chatlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.R
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.MessagePreview
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.chat.PrivateChatScreen
import com.example.overgram.presentation.newchat.NewChatScreen
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.ChatListItem
import com.example.overgram.ui.components.OverGramBottomBar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

data object ChatListScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: ChatListViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleStartEffect(viewModel) {
            viewModel.startPolling()
            onStopOrDispose { viewModel.stopPolling() }
        }

        LaunchedEffect(state.isSessionEnded) {
            if (state.isSessionEnded) navigator.replaceAll(PhoneEntryScreen)
        }

        ChatListContent(
            state = state,
            onRefresh = viewModel::refresh,
            onRetry = viewModel::retry,
            onErrorShown = viewModel::onErrorShown,
            onLogout = viewModel::logout,
            onNewChat = { navigator.push(NewChatScreen) },
            onChatClick = { chat ->
                // Group chats get their own screen (separate card on the board).
                if (chat.type == ChatType.DIRECT) {
                    navigator.push(
                        PrivateChatScreen(
                            chatId = chat.id,
                            peerUserId = chat.peer?.id,
                            title = chat.peer?.displayName.orEmpty()
                        )
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListContent(
    state: ChatListUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onErrorShown: () -> Unit,
    onLogout: () -> Unit,
    onChatClick: (ChatSummary) -> Unit,
    onNewChat: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // With a list on screen, errors are transient: show them as a snackbar.
    val snackbarError = state.error?.takeIf { state.chats.isNotEmpty() }
    val snackbarMessage = snackbarError?.let { authErrorMessage(it) }
    LaunchedEffect(snackbarError) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            OverGramTopBar(
                title = stringResource(R.string.app_name),
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.chats_search),
                            tint = TextPrimary
                        )
                    }
                    OverflowMenu(onLogout = onLogout)
                }
            )
        },
        bottomBar = {
            OverGramBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNewChat,
                    containerColor = PrimaryViolet,
                    contentColor = TextPrimary,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.chats_new_message)
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.chats.isNotEmpty() -> ChatList(
                    chats = state.chats,
                    onlineUsers = state.onlineUsers,
                    currentUserId = state.currentUserId,
                    onChatClick = onChatClick
                )
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryViolet)
                }
                state.error != null -> CenteredMessage(
                    title = authErrorMessage(state.error),
                    actionLabel = stringResource(R.string.chats_retry),
                    onAction = onRetry
                )
                else -> CenteredMessage(
                    title = stringResource(R.string.chats_empty_title),
                    subtitle = stringResource(R.string.chats_empty_subtitle)
                )
            }
        }
    }
}

@Composable
private fun OverflowMenu(onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.chats_more_options),
                tint = TextPrimary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chats_logout)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                onClick = {
                    expanded = false
                    onLogout()
                }
            )
        }
    }
}

@Composable
private fun ChatList(
    chats: List<ChatSummary>,
    onlineUsers: List<UserProfile>,
    currentUserId: String?,
    onChatClick: (ChatSummary) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (onlineUsers.isNotEmpty()) {
            item(key = "online", contentType = "online") {
                OnlineUsersRow(users = onlineUsers)
            }
        }
        items(chats, key = { it.id }, contentType = { "chat" }) { chat ->
            ChatListItem(
                name = chatTitle(chat),
                lastMessage = chatSubtitle(chat, currentUserId),
                time = chatTime(chat.lastMessage?.createdAt ?: chat.lastActivityAt),
                unreadCount = chat.unreadCount,
                isOnline = chat.peer?.isOnline == true,
                onClick = { onChatClick(chat) }
            )
        }
    }
}

@Composable
private fun OnlineUsersRow(users: List<UserProfile>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.chats_online_now),
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            modifier = Modifier.padding(
                start = Dimens.ScreenPadding,
                end = Dimens.ScreenPadding,
                top = Dimens.SpacingMd,
                bottom = Dimens.SpacingSm
            )
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingLg)
        ) {
            items(users, key = { it.id }) { user ->
                Column(
                    modifier = Modifier.width(Dimens.AvatarLarge),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(name = user.displayName, size = Dimens.AvatarMedium, isOnline = true)
                    Spacer(Modifier.height(Dimens.SpacingXs))
                    Text(
                        text = user.displayName.substringBefore(' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = Dimens.SpacingMd),
            color = DividerColor
        )
    }
}

/** Scrollable so pull-to-refresh still works on empty and error states. */
@Composable
private fun CenteredMessage(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.SpacingXxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        if (subtitle != null) {
            Spacer(Modifier.height(Dimens.SpacingSm))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
        if (actionLabel != null) {
            Spacer(Modifier.height(Dimens.SpacingLg))
            TextButton(onClick = onAction) {
                Text(actionLabel, color = PrimaryViolet)
            }
        }
    }
}

private val previewChats = listOf(
    ChatSummary(
        id = "1", type = ChatType.DIRECT, title = null,
        peer = UserProfile("u1", "Даша", "dasha", null, isOnline = true, lastSeenAt = null),
        lastMessage = MessagePreview("u1", MessageType.TEXT, "Хорошо, спасибо! 😊", System.currentTimeMillis(), false),
        lastActivityAt = System.currentTimeMillis(), unreadCount = 2, isMuted = false
    ),
    ChatSummary(
        id = "2", type = ChatType.GROUP, title = "Учеба | TUIT", peer = null,
        lastMessage = MessagePreview("me", MessageType.IMAGE, null, System.currentTimeMillis() - 86_400_000L, false),
        lastActivityAt = System.currentTimeMillis() - 86_400_000L, unreadCount = 0, isMuted = false
    ),
    ChatSummary(
        id = "3", type = ChatType.DIRECT, title = null,
        peer = UserProfile("u3", "Максим", null, null, isOnline = false, lastSeenAt = System.currentTimeMillis() - 600_000L),
        lastMessage = null,
        lastActivityAt = System.currentTimeMillis() - 3 * 86_400_000L, unreadCount = 0, isMuted = false
    )
)

@Preview(showBackground = true)
@Composable
fun ChatListContentPreview() {
    OverGramTheme {
        ChatListContent(
            state = ChatListUiState(chats = previewChats, currentUserId = "me", isLoading = false),
            onRefresh = {}, onRetry = {}, onErrorShown = {}, onLogout = {}, onChatClick = {}, onNewChat = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatListEmptyPreview() {
    OverGramTheme {
        ChatListContent(
            state = ChatListUiState(isLoading = false),
            onRefresh = {}, onRetry = {}, onErrorShown = {}, onLogout = {}, onChatClick = {}, onNewChat = {}
        )
    }
}
