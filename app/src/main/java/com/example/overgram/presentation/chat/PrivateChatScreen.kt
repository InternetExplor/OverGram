package com.example.overgram.presentation.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.chatlist.dayLabel
import com.example.overgram.presentation.chatlist.formatClock
import com.example.overgram.presentation.chatlist.isSameDay
import com.example.overgram.presentation.chatlist.presenceText
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.BubbleStatus
import com.example.overgram.ui.components.ChatBubble
import com.example.overgram.ui.components.MessageInputBar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Calendar

data class PrivateChatScreen(
    val chatId: String,
    val peerUserId: String?,
    val title: String
) : Screen {

    override val key: ScreenKey = "chat:$chatId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val args = PrivateChatArgs(chatId, peerUserId, title)
        val viewModel = hiltViewModel<PrivateChatViewModel, PrivateChatViewModel.Factory>(
            key = key,
            creationCallback = { factory -> factory.create(args) }
        )
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleStartEffect(viewModel) {
            viewModel.startPolling()
            onStopOrDispose { viewModel.stopPolling() }
        }

        LaunchedEffect(state.isSessionEnded) {
            if (state.isSessionEnded) navigator.replaceAll(PhoneEntryScreen)
        }

        PrivateChatContent(
            state = state,
            onBack = { navigator.pop() },
            onInputChange = viewModel::onInputChange,
            onSend = viewModel::send,
            onRetryMessage = viewModel::retry,
            onLoadOlder = viewModel::loadOlder,
            onRetryLoad = viewModel::retryLoad,
            onErrorShown = viewModel::onErrorShown
        )
    }
}

/** A row of the message list: a bubble or a day separator. */
private sealed interface ChatRow {
    val key: String

    data class Bubble(val item: ChatMessageItem) : ChatRow {
        override val key get() = item.clientMessageId
    }

    data class Day(val timestamp: Long) : ChatRow {
        override val key: String
            get() = Calendar.getInstance().apply { timeInMillis = timestamp }
                .let { "day:${it.get(Calendar.YEAR)}-${it.get(Calendar.DAY_OF_YEAR)}" }
    }
}

/**
 * [messages] are newest first for a reversed list, so a day's separator goes *after*
 * its oldest message (which renders it above that day's bubbles).
 */
private fun buildRows(messages: List<ChatMessageItem>): List<ChatRow> = buildList {
    messages.forEachIndexed { index, item ->
        add(ChatRow.Bubble(item))
        val older = messages.getOrNull(index + 1)
        if (older == null || !isSameDay(older.createdAt, item.createdAt)) {
            add(ChatRow.Day(item.createdAt))
        }
    }
}

@Composable
fun PrivateChatContent(
    state: PrivateChatUiState,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onRetryMessage: (String) -> Unit,
    onLoadOlder: () -> Unit,
    onRetryLoad: () -> Unit,
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

    val focusManager = LocalFocusManager.current
    var isEmojiPanelOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isEmojiPanelOpen) { isEmojiPanelOpen = false }

    val title = state.peer?.displayName ?: state.title
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            OverGramTopBar(
                title = title,
                subtitle = state.peer?.let { presenceText(it) },
                onBackClick = onBack,
                leadContent = {
                    Avatar(
                        name = title,
                        size = Dimens.AvatarSmall,
                        isOnline = state.peer?.isOnline == true
                    )
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                value = state.input,
                onValueChange = onInputChange,
                onSendClick = onSend,
                modifier = Modifier.imePadding(),
                onEmojiClick = {
                    if (!isEmojiPanelOpen) focusManager.clearFocus() // hides the keyboard
                    isEmojiPanelOpen = !isEmojiPanelOpen
                },
                isEmojiPanelOpen = isEmojiPanelOpen,
                onEmojiSelected = { onInputChange(state.input + it) },
                onInputFocused = { isEmojiPanelOpen = false },
                placeholderText = stringResource(R.string.chat_input_placeholder)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.messages.isNotEmpty() -> MessageList(
                    state = state,
                    onRetryMessage = onRetryMessage,
                    onLoadOlder = onLoadOlder
                )
                state.isLoading -> CircularProgressIndicator(
                    color = PrimaryViolet,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.loadError != null -> Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Dimens.SpacingXxl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = authErrorMessage(state.loadError),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(Dimens.SpacingLg))
                    TextButton(onClick = onRetryLoad) {
                        Text(stringResource(R.string.chats_retry), color = PrimaryViolet)
                    }
                }
                else -> Text(
                    text = stringResource(R.string.chat_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(Dimens.SpacingXxl)
                )
            }
        }
    }
}

@Composable
private fun MessageList(
    state: PrivateChatUiState,
    onRetryMessage: (String) -> Unit,
    onLoadOlder: () -> Unit
) {
    val listState = rememberLazyListState()
    val rows = remember(state.messages) { buildRows(state.messages) }

    LoadOlderWhenNearTop(listState, onLoadOlder)
    StickToNewest(listState, newest = state.messages.firstOrNull())

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimens.SpacingSm)
    ) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            when (row) {
                is ChatRow.Bubble -> MessageRow(row.item, onRetryMessage)
                is ChatRow.Day -> DaySeparator(row.timestamp)
            }
        }
        if (state.isLoadingOlder) {
            item(key = "loading-older") {
                Box(Modifier.fillMaxWidth().padding(Dimens.SpacingMd), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryViolet, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

/** In a reversed list the oldest rows are the last indices. */
@Composable
private fun LoadOlderWhenNearTop(listState: LazyListState, onLoadOlder: () -> Unit) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - LOAD_OLDER_THRESHOLD
        }
            .distinctUntilChanged()
            .collect { nearTop -> if (nearTop) onLoadOlder() }
    }
}

/**
 * The list keeps its position by key, so a new message would land off-screen below.
 * Follow it when the user is already at the bottom, or when it's their own message.
 */
@Composable
private fun StickToNewest(listState: LazyListState, newest: ChatMessageItem?) {
    LaunchedEffect(newest?.clientMessageId) {
        if (newest == null) return@LaunchedEffect
        val ownJustSent = newest.outgoingState == OutgoingState.Sending
        if (ownJustSent || listState.firstVisibleItemIndex <= 1) {
            listState.animateScrollToItem(0)
        }
    }
}

@Composable
private fun MessageRow(item: ChatMessageItem, onRetryMessage: (String) -> Unit) {
    val failed = item.outgoingState == OutgoingState.Failed
    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        ChatBubble(
            message = messageText(item),
            timestamp = formatClock(item.createdAt),
            isSent = item.isOutgoing,
            status = when (item.outgoingState) {
                OutgoingState.Sending -> BubbleStatus.Sending
                OutgoingState.Failed -> BubbleStatus.Failed
                OutgoingState.Sent, null -> BubbleStatus.Sent
            },
            isEdited = item.isEdited && !item.isDeleted,
            isPlaceholder = item.isDeleted,
            onClick = if (failed) ({ onRetryMessage(item.clientMessageId) }) else null
        )
        if (failed) {
            Text(
                text = stringResource(R.string.chat_tap_to_retry),
                style = MaterialTheme.typography.labelSmall,
                color = ErrorRed,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
            )
        }
    }
}

@Composable
private fun messageText(item: ChatMessageItem): String {
    if (item.isDeleted) return stringResource(R.string.chats_preview_deleted)
    val body = item.body?.takeIf { it.isNotBlank() }
    val label = when (item.type) {
        MessageType.TEXT -> return body.orEmpty()
        MessageType.IMAGE -> stringResource(R.string.chats_preview_photo)
        MessageType.VIDEO -> stringResource(R.string.chats_preview_video)
        MessageType.FILE -> stringResource(R.string.chats_preview_file)
        MessageType.SYSTEM -> return stringResource(R.string.chats_preview_system)
        MessageType.UNKNOWN -> stringResource(R.string.chats_preview_unsupported)
    }
    // Media isn't rendered yet: show what it is, plus the caption.
    return if (body != null) "[$label] $body" else "[$label]"
}

@Composable
private fun DaySeparator(timestamp: Long) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingSm),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dayLabel(timestamp),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier
                .background(SurfaceElevatedDark, RoundedCornerShape(50))
                .padding(horizontal = Dimens.SpacingMd, vertical = Dimens.SpacingXs)
        )
    }
}

private const val LOAD_OLDER_THRESHOLD = 5

@Preview(showBackground = true)
@Composable
fun PrivateChatContentPreview() {
    val now = System.currentTimeMillis()
    OverGramTheme {
        PrivateChatContent(
            state = PrivateChatUiState(
                title = "Даша",
                peer = UserProfile("u1", "Даша", "dasha", null, isOnline = true, lastSeenAt = null),
                isLoading = false,
                messages = listOf(
                    ChatMessageItem("4", null, MessageType.TEXT, "Не дошло", now, true, outgoingState = OutgoingState.Failed),
                    ChatMessageItem("3", null, MessageType.TEXT, "Отправляю…", now, true, outgoingState = OutgoingState.Sending),
                    ChatMessageItem("2", 2, MessageType.TEXT, "Хорошо, спасибо! 😊", now - 60_000, false, isEdited = true),
                    ChatMessageItem("1", 1, MessageType.TEXT, "Привет! Как дела?", now - 86_400_000, true, outgoingState = OutgoingState.Sent)
                )
            ),
            onBack = {}, onInputChange = {}, onSend = {}, onRetryMessage = {},
            onLoadOlder = {}, onRetryLoad = {}, onErrorShown = {}
        )
    }
}
