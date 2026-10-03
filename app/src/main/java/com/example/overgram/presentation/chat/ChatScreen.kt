package com.example.overgram.presentation.chat

import android.content.ClipData
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
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
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.MediaAttachment
import com.example.overgram.domain.model.MediaKind
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SystemEvent
import com.example.overgram.domain.model.SystemEventKind
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.chatlist.connectionStatusText
import com.example.overgram.presentation.chatlist.dayLabel
import com.example.overgram.presentation.chatlist.formatClock
import com.example.overgram.presentation.chatlist.isSameDay
import com.example.overgram.presentation.chatlist.joinNames
import com.example.overgram.presentation.chatlist.presenceText
import com.example.overgram.presentation.chatlist.systemEventText
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.presentation.groupinfo.GroupInfoScreen
import com.example.overgram.presentation.media.PhotoViewerScreen
import com.example.overgram.presentation.media.VideoPlayerScreen
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.BubbleStatus
import com.example.overgram.ui.components.ChatBubble
import com.example.overgram.ui.components.InputContext
import com.example.overgram.ui.components.MessageInputBar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.components.QuoteContent
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.AccentBright
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.ChatWallpaper
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OutgoingCheck
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.ServiceBackground
import com.example.overgram.ui.theme.SenderNameColors
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary
import java.util.Calendar
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** One screen for both DIRECT and GROUP chats; groups add sender names, system notes and admin actions. */
data class ChatScreen(
    val chatId: String,
    val type: ChatType,
    val peerUserId: String?,
    val title: String
) : Screen {

    override val key: ScreenKey = "chat:$chatId"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val args = ChatArgs(chatId, type, peerUserId, title)
        val viewModel = hiltViewModel<ChatViewModel, ChatViewModel.Factory>(
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
        LaunchedEffect(state.hasLeft) {
            if (state.hasLeft) navigator.pop()
        }

        ChatContent(
            state = state,
            actions = ChatActions(
                onBack = { navigator.pop() },
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::send,
                onRetryMessage = viewModel::retry,
                onLoadOlder = viewModel::loadOlder,
                onRetryLoad = viewModel::retryLoad,
                onErrorShown = viewModel::onErrorShown,
                onToggleMute = viewModel::toggleMute,
                onRename = viewModel::openRenameDialog,
                onGroupInfo = { navigator.push(GroupInfoScreen(chatId, state.title)) },
                onLeave = viewModel::leave,
                onSendMedia = viewModel::sendMedia,
                onCancelSending = viewModel::cancelSending,
                onOpenMedia = { media ->
                    navigator.push(
                        if (media.kind == MediaKind.VIDEO) VideoPlayerScreen(media.mediaId, media.localPath)
                        else PhotoViewerScreen(media.mediaId, media.localPath)
                    )
                },
                onMediaNotice = viewModel::showMediaNotice,
                onMediaNoticeShown = viewModel::onMediaNoticeShown,
                onReply = viewModel::startReply,
                onEdit = viewModel::startEdit,
                onDelete = viewModel::delete,
                onCancelComposeMode = viewModel::cancelComposeMode
            )
        )

        state.renameDialog?.let { dialog ->
            RenameDialog(
                state = dialog,
                onValueChange = viewModel::onRenameInput,
                onSave = viewModel::saveRename,
                onDismiss = viewModel::dismissRenameDialog
            )
        }
    }
}

/** Everything the chat UI can ask for, so previews and the screen share one signature. */
data class ChatActions(
    val onBack: () -> Unit = {},
    val onInputChange: (String) -> Unit = {},
    val onSend: () -> Unit = {},
    val onRetryMessage: (String) -> Unit = {},
    val onLoadOlder: () -> Unit = {},
    val onRetryLoad: () -> Unit = {},
    val onErrorShown: () -> Unit = {},
    val onToggleMute: () -> Unit = {},
    val onRename: () -> Unit = {},
    val onGroupInfo: () -> Unit = {},
    val onLeave: () -> Unit = {},
    val onSendMedia: (uris: List<String>, caption: String?, asFile: Boolean) -> Unit = { _, _, _ -> },
    val onCancelSending: (String) -> Unit = {},
    /** A photo or video tapped: open it full screen. */
    val onOpenMedia: (MediaAttachment) -> Unit = {},
    val onMediaNotice: (MediaNotice) -> Unit = {},
    val onMediaNoticeShown: () -> Unit = {},
    /** Long-press menu → Reply: the next message answers this one. */
    val onReply: (ChatMessageItem) -> Unit = {},
    /** Long-press menu → Edit (our own messages). */
    val onEdit: (ChatMessageItem) -> Unit = {},
    /** Long-press menu → Delete, after confirmation. */
    val onDelete: (ChatMessageItem) -> Unit = {},
    /** ✕ on the reply / edit strip, or Back while it's shown. */
    val onCancelComposeMode: () -> Unit = {}
)

/** A row of the message list: a bubble, a system note, or a day separator. */
private sealed interface ChatRow {
    val key: String

    data class Bubble(val item: ChatMessageItem, val showSender: Boolean) : ChatRow {
        override val key get() = item.clientMessageId
    }

    data class System(val item: ChatMessageItem) : ChatRow {
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
 * its oldest message (which renders it above that day's bubbles). In groups the sender's
 * name is shown on the first bubble of each run of messages from the same person.
 */
private fun buildRows(messages: List<ChatMessageItem>, isGroup: Boolean): List<ChatRow> = buildList {
    messages.forEachIndexed { index, item ->
        val older = messages.getOrNull(index + 1)
        val newDay = older == null || !isSameDay(older.createdAt, item.createdAt)
        if (item.type == MessageType.SYSTEM) {
            add(ChatRow.System(item))
        } else {
            val startsRun = newDay || older?.type == MessageType.SYSTEM || older?.senderId != item.senderId
            add(ChatRow.Bubble(item, showSender = isGroup && !item.isOutgoing && startsRun))
        }
        if (newDay) add(ChatRow.Day(item.createdAt))
    }
}

@Composable
fun ChatContent(state: ChatUiState, actions: ChatActions) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = state.error?.let { authErrorMessage(it) }
    LaunchedEffect(state.error) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            actions.onErrorShown()
        }
    }
    val mediaNoticeText = state.mediaNotice?.let { mediaNoticeMessage(it) }
    LaunchedEffect(state.mediaNotice) {
        if (mediaNoticeText != null) {
            snackbarHostState.showSnackbar(mediaNoticeText)
            actions.onMediaNoticeShown()
        }
    }

    val focusManager = LocalFocusManager.current
    var isEmojiPanelOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isEmojiPanelOpen) { isEmojiPanelOpen = false }
    var isLeaveDialogOpen by rememberSaveable { mutableStateOf(false) }
    var isAttachMenuOpen by rememberSaveable { mutableStateOf(false) }

    // Replying or editing: focus the field (opens the keyboard); Back leaves the mode first.
    val inputFocus = remember { FocusRequester() }
    val composeTarget = state.editing ?: state.replyingTo
    LaunchedEffect(composeTarget?.clientMessageId) {
        if (composeTarget != null) runCatching { inputFocus.requestFocus() }
    }
    BackHandler(enabled = composeTarget != null && !isEmojiPanelOpen) { actions.onCancelComposeMode() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ChatWallpaper,
        topBar = {
            OverGramTopBar(
                title = state.title,
                subtitle = chatSubtitle(state),
                highlightSubtitle = state.isLive && (state.typingUserIds.isNotEmpty() || state.peer?.isOnline == true),
                onBackClick = actions.onBack,
                onTitleClick = if (state.isGroup) actions.onGroupInfo else null,
                leadContent = {
                    Avatar(
                        name = state.title,
                        imageUrl = mediaUrl(state.peer?.avatarMediaId),
                        size = Dimens.AvatarSmall,
                        isOnline = state.isLive && state.peer?.isOnline == true
                    )
                },
                actions = {
                    if (state.isMuted) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = stringResource(R.string.chats_muted),
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    ChatMenu(
                        state = state,
                        actions = actions,
                        onLeave = { isLeaveDialogOpen = true }
                    )
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                value = state.input,
                onValueChange = actions.onInputChange,
                onSendClick = actions.onSend,
                modifier = Modifier.imePadding(),
                onAttachClick = {
                    focusManager.clearFocus()
                    isAttachMenuOpen = true
                },
                onEmojiClick = {
                    if (!isEmojiPanelOpen) focusManager.clearFocus() // hides the keyboard
                    isEmojiPanelOpen = !isEmojiPanelOpen
                },
                isEmojiPanelOpen = isEmojiPanelOpen,
                onEmojiSelected = { actions.onInputChange(state.input + it) },
                onInputFocused = { isEmojiPanelOpen = false },
                placeholderText = stringResource(R.string.chat_input_placeholder),
                context = when {
                    state.editing != null -> InputContext(
                        icon = Icons.Default.Edit,
                        quote = QuoteContent(stringResource(R.string.message_editing), messageText(state.editing), Accent),
                        onDismiss = actions.onCancelComposeMode
                    )
                    state.replyingTo != null -> InputContext(
                        icon = Icons.AutoMirrored.Filled.Reply,
                        quote = quoteOf(state.replyingTo, state).copy(color = Accent),
                        onDismiss = actions.onCancelComposeMode
                    )
                    else -> null
                },
                focusRequester = inputFocus
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
                state.messages.isNotEmpty() -> MessageList(state = state, actions = actions)
                state.isLoading -> CircularProgressIndicator(
                    color = Accent,
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
                    TextButton(onClick = actions.onRetryLoad) {
                        Text(stringResource(R.string.chats_retry), color = Accent)
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

    AttachFlow(
        isMenuOpen = isAttachMenuOpen,
        onDismissMenu = { isAttachMenuOpen = false },
        onSend = actions.onSendMedia
    )

    if (isLeaveDialogOpen) {
        LeaveDialog(
            title = state.title,
            isLeaving = state.isLeaving,
            onConfirm = {
                isLeaveDialogOpen = false
                actions.onLeave()
            },
            onDismiss = { isLeaveDialogOpen = false }
        )
    }
}

/** DIRECT: the peer's presence. GROUP: who's in it, as far as the loaded history tells. */
@Composable
private fun chatSubtitle(state: ChatUiState): String? {
    connectionStatusText(state.connectionState)?.let { return it }
    typingText(state)?.let { return it }
    if (!state.isGroup) return state.peer?.let { presenceText(it) }
    val ids = state.knownMemberIds
    if (ids.isEmpty()) return null
    val names = ids.sortedBy { it == state.currentUserId } // "you" last
        .take(MAX_SUBTITLE_NAMES)
        .map { id ->
            if (id == state.currentUserId) stringResource(R.string.system_you_object)
            else state.profiles[id]?.displayName ?: return null
        }
    val hidden = ids.size - names.size
    // "A, B and C" when everyone fits; "A, B, C and 2 more" otherwise (never two "and"s).
    return if (hidden > 0) {
        stringResource(R.string.chat_members_more, names.joinToString(", "), hidden)
    } else {
        joinNames(names)
    }
}

/** "typing…" in a DIRECT chat; "Ada is typing…" / "Ada and Bob are typing…" / "3 people…" in a group. */
@Composable
private fun typingText(state: ChatUiState): String? {
    val ids = state.typingUserIds.toList()
    if (ids.isEmpty()) return null
    if (!state.isGroup) return stringResource(R.string.typing_short)
    val names = ids.map { state.profiles[it]?.displayName ?: stringResource(R.string.system_someone) }
    return when (names.size) {
        1 -> stringResource(R.string.typing_one, names[0])
        2 -> stringResource(R.string.typing_two, names[0], names[1])
        else -> pluralStringResource(R.plurals.typing_many, names.size, names.size)
    }
}

@Composable
private fun ChatMenu(state: ChatUiState, actions: ChatActions, onLeave: () -> Unit) {
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
            fun close(action: () -> Unit): () -> Unit = {
                expanded = false
                action()
            }
            DropdownMenuItem(
                text = { Text(stringResource(if (state.isMuted) R.string.chat_unmute else R.string.chat_mute)) },
                leadingIcon = {
                    Icon(
                        if (state.isMuted) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null
                    )
                },
                onClick = close(actions.onToggleMute)
            )
            if (state.isGroup) {
                // Members (list, add, remove) live on the group info screen.
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_group_info)) },
                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
                    onClick = close(actions.onGroupInfo)
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_rename_group)) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    onClick = close(actions.onRename)
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_leave_group), color = ErrorRed) },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = ErrorRed) },
                    onClick = close(onLeave)
                )
            }
        }
    }
}

@Composable
private fun MessageList(state: ChatUiState, actions: ChatActions) {
    val listState = rememberLazyListState()
    val rows = remember(state.messages, state.isGroup) { buildRows(state.messages, state.isGroup) }
    val scope = rememberCoroutineScope()

    LoadOlderWhenNearTop(listState, actions.onLoadOlder)
    StickToNewest(listState, newest = state.messages.firstOrNull())

    LazyColumn(
        state = listState,
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = Dimens.SpacingSm)
    ) {
        items(rows, key = { it.key }, contentType = { it::class }) { row ->
            when (row) {
                is ChatRow.Bubble -> MessageRow(
                    item = row.item,
                    senderName = if (row.showSender) {
                        state.profiles[row.item.senderId]?.displayName
                            ?: stringResource(R.string.system_someone)
                    } else null,
                    quote = row.item.replyTo?.let { reply ->
                        quoteOf(reply.message, state, onOutgoingBubble = row.item.isOutgoing)
                    },
                    uploadProgress = state.uploadProgress[row.item.clientMessageId],
                    actions = actions,
                    onQuoteClick = { id ->
                        // Jump to the quoted message if it's loaded.
                        val index = rows.indexOfFirst { it.key == id }
                        if (index >= 0) scope.launch { listState.animateScrollToItem(index) }
                    }
                )
                is ChatRow.System -> SystemNote(
                    text = row.item.systemEvent?.let { event ->
                        systemEventText(event, state.currentUserId) { state.profiles[it] }
                    } ?: stringResource(R.string.chats_preview_system)
                )
                is ChatRow.Day -> CenteredPill(dayLabel(row.timestamp))
            }
        }
        if (state.isLoadingOlder) {
            item(key = "loading-older") {
                Box(Modifier.fillMaxWidth().padding(Dimens.SpacingMd), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
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

/**
 * One message: its bubble (text or media), the quote of what it answers, and — on long press —
 * Telegram's message menu: Reply, Copy, Edit and Delete (the last two for our own messages).
 */
@Composable
private fun MessageRow(
    item: ChatMessageItem,
    senderName: String?,
    quote: QuoteContent?,
    uploadProgress: Float?,
    actions: ChatActions,
    onQuoteClick: (String) -> Unit
) {
    val failed = item.outgoingState == OutgoingState.Failed
    val status = when (item.outgoingState) {
        OutgoingState.Sending -> BubbleStatus.Sending
        OutgoingState.Failed -> BubbleStatus.Failed
        OutgoingState.Delivered -> BubbleStatus.Delivered
        OutgoingState.Read -> BubbleStatus.Read
        OutgoingState.Sent, null -> BubbleStatus.Sent
    }
    val copyText = item.body?.takeIf { it.isNotBlank() && !item.isDeleted && item.type != MessageType.FILE }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isDeleteDialogOpen by remember { mutableStateOf(false) }
    val onLongClick = if (item.canReply || item.canDelete || copyText != null) ({ isMenuOpen = true }) else null
    val onReplyClick = item.replyTo?.let { reply -> { onQuoteClick(reply.clientMessageId) } }

    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            val media = item.media
            if (media != null && !item.isDeleted) {
                MediaMessageBubble(
                    item = item,
                    media = media,
                    senderName = senderName,
                    senderColor = senderColor(item.senderId),
                    status = status,
                    uploadProgress = uploadProgress,
                    actions = actions,
                    reply = quote,
                    onReplyClick = onReplyClick,
                    onLongClick = onLongClick
                )
            } else {
                ChatBubble(
                    message = messageText(item),
                    timestamp = formatClock(item.createdAt),
                    isSent = item.isOutgoing,
                    status = status,
                    isEdited = item.isEdited && !item.isDeleted,
                    isPlaceholder = item.isDeleted,
                    senderName = senderName,
                    senderColor = senderColor(item.senderId),
                    reply = quote.takeIf { !item.isDeleted },
                    onReplyClick = onReplyClick,
                    onClick = if (failed) ({ actions.onRetryMessage(item.clientMessageId) }) else null,
                    onLongClick = onLongClick
                )
            }
            // Anchored to the bubble's side, so the menu opens next to it.
            Box(
                Modifier
                    .align(if (item.isOutgoing) Alignment.TopEnd else Alignment.TopStart)
                    .padding(horizontal = Dimens.SpacingSm)
            ) {
                MessageMenu(
                    expanded = isMenuOpen,
                    item = item,
                    copyText = copyText,
                    onDismiss = { isMenuOpen = false },
                    onReply = { actions.onReply(item) },
                    onEdit = { actions.onEdit(item) },
                    onDelete = { isDeleteDialogOpen = true }
                )
            }
        }
        if (failed) {
            Text(
                text = item.failureReason?.let { stringResource(R.string.chat_failed_with_reason, it) }
                    ?: stringResource(R.string.chat_tap_to_retry),
                style = MaterialTheme.typography.labelSmall,
                color = ErrorRed,
                modifier = Modifier.padding(horizontal = Dimens.ScreenPadding)
            )
        }
    }

    if (isDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { isDeleteDialogOpen = false },
            containerColor = SurfaceDark,
            title = { Text(stringResource(R.string.message_delete_title), color = TextPrimary) },
            text = { Text(stringResource(R.string.message_delete_text), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    isDeleteDialogOpen = false
                    actions.onDelete(item)
                }) { Text(stringResource(R.string.message_action_delete), color = ErrorRed) }
            },
            dismissButton = {
                TextButton(onClick = { isDeleteDialogOpen = false }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            }
        )
    }
}

/** Telegram's long-press menu: only the actions that apply to [item]. */
@Composable
private fun MessageMenu(
    expanded: Boolean,
    item: ChatMessageItem,
    copyText: String?,
    onDismiss: () -> Unit,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = SurfaceElevatedDark) {
        fun close(action: () -> Unit): () -> Unit = {
            onDismiss()
            action()
        }
        if (item.canReply) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_action_reply), color = TextPrimary) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, tint = TextSecondary) },
                onClick = close(onReply)
            )
        }
        if (copyText != null) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_action_copy), color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextSecondary) },
                onClick = close {
                    scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(null, copyText))) }
                }
            )
        }
        if (item.canEdit) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_action_edit), color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary) },
                onClick = close(onEdit)
            )
        }
        if (item.canDelete) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.message_action_delete), color = ErrorRed) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                onClick = close(onDelete)
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
    // Media without attachment metadata (shouldn't happen): say what it is, plus the caption.
    return if (body != null) "[$label] $body" else "[$label]"
}

/**
 * The quote of [message] (null: not loaded on this device): its author and a line of it. On our
 * own (blue) bubble the quote uses the light check color; elsewhere the author's name color —
 * per person in groups, the accent in a private chat — like Telegram.
 */
@Composable
private fun quoteOf(message: ChatMessageItem?, state: ChatUiState, onOutgoingBubble: Boolean = false): QuoteContent {
    if (message == null) {
        return QuoteContent(
            stringResource(R.string.message_reply_unavailable),
            "…",
            if (onOutgoingBubble) OutgoingCheck else AccentBright
        )
    }
    val author = when {
        message.senderId == state.currentUserId -> stringResource(R.string.system_you_subject)
        !state.isGroup -> state.peer?.displayName ?: state.title
        else -> state.profiles[message.senderId]?.displayName ?: stringResource(R.string.system_someone)
    }
    val color = when {
        onOutgoingBubble -> OutgoingCheck
        state.isGroup -> senderColor(message.senderId)
        else -> AccentBright
    }
    return QuoteContent(author, messageText(message), color)
}

@Composable
private fun mediaNoticeMessage(notice: MediaNotice): String = stringResource(
    when (notice) {
        MediaNotice.TooLarge -> R.string.media_too_large
        MediaNotice.Unreadable -> R.string.media_unreadable
        MediaNotice.NoAppToOpen -> R.string.media_no_app
        MediaNotice.DownloadFailed -> R.string.media_download_failed
    }
)

/** Stable per-user name colour, bright enough for the dark received bubble. */
private fun senderColor(userId: String): Color =
    SenderNameColors[abs(userId.hashCode()) % SenderNameColors.size]

@Composable
private fun SystemNote(text: String) = CenteredPill(text, horizontalPadding = Dimens.SpacingXl)

@Composable
private fun CenteredPill(text: String, horizontalPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingSm, horizontal = horizontalPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .background(ServiceBackground, RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun RenameDialog(
    state: RenameDialogState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        containerColor = SurfaceDark,
        title = { Text(stringResource(R.string.chat_rename_group), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                OutlinedTextField(
                    value = state.input,
                    onValueChange = onValueChange,
                    singleLine = true,
                    enabled = !state.isSaving,
                    isError = state.error != null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSave() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = DividerColor,
                        cursorColor = Accent
                    )
                )
                state.error?.let {
                    Text(authErrorMessage(it), style = MaterialTheme.typography.bodySmall, color = ErrorRed)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSaving && state.input.isNotBlank()) {
                if (state.isSaving) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_save), color = Accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(stringResource(R.string.action_cancel), color = TextSecondary)
            }
        }
    )
}

@Composable
private fun LeaveDialog(title: String, isLeaving: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(stringResource(R.string.chat_leave_confirm_title), color = TextPrimary) },
        text = { Text(stringResource(R.string.chat_leave_confirm_text, title), color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLeaving) {
                Text(stringResource(R.string.chat_leave_group), color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel), color = TextSecondary)
            }
        }
    )
}

private const val LOAD_OLDER_THRESHOLD = 5
private const val MAX_SUBTITLE_NAMES = 3

@Preview(showBackground = true)
@Composable
fun ChatContentPreview() {
    val now = System.currentTimeMillis()
    OverGramTheme {
        ChatContent(
            state = ChatUiState(
                type = ChatType.DIRECT,
                title = "Даша",
                currentUserId = "me",
                peer = UserProfile("u1", "Даша", "dasha", null, isOnline = true, lastSeenAt = null),
                isLoading = false,
                messages = previewDirectMessages(now)
            ),
            actions = ChatActions()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GroupChatContentPreview() {
    val now = System.currentTimeMillis()
    val ada = UserProfile("u1", "Ada Lovelace", "ada", null, isOnline = true, lastSeenAt = null)
    val bob = UserProfile("u2", "Bob", "bob", null, isOnline = false, lastSeenAt = null)
    OverGramTheme {
        ChatContent(
            state = ChatUiState(
                type = ChatType.GROUP,
                title = "Учеба | TUIT",
                currentUserId = "me",
                isMuted = true,
                isLoading = false,
                profiles = mapOf(ada.id to ada, bob.id to bob),
                messages = listOf(
                    ChatMessageItem("5", 5, "me", MessageType.TEXT, "Я сделал 👍", now, true, outgoingState = OutgoingState.Sent),
                    ChatMessageItem("4", 4, "u2", MessageType.TEXT, "А вторую?", now - 30_000, false),
                    ChatMessageItem("3", 3, "u1", MessageType.TEXT, "Кто сделал лабу?", now - 60_000, false),
                    ChatMessageItem("2", 2, "u1", MessageType.TEXT, "Всем привет", now - 70_000, false),
                    ChatMessageItem(
                        "1", 1, "u1", MessageType.SYSTEM, null, now - 80_000, false,
                        systemEvent = SystemEvent(SystemEventKind.GROUP_CREATED, "u1", listOf("u2", "me"), "Учеба | TUIT")
                    )
                )
            ),
            actions = ChatActions()
        )
    }
}

/** Normal, reply, edited, deleted, sent / delivered / read, sending and failed messages. */
private fun previewDirectMessages(now: Long): List<ChatMessageItem> {
    val question = ChatMessageItem("1", 1, "me", MessageType.TEXT, "Привет! Как дела?", now - 86_400_000, true, outgoingState = OutgoingState.Read)
    val answer = ChatMessageItem("2", 2, "u1", MessageType.TEXT, "Хорошо, спасибо! 😊 Встретимся в субботу?", now - 60_000, false, isEdited = true)
    return listOf(
        ChatMessageItem("8", null, "me", MessageType.TEXT, "Не дошло", now, true, outgoingState = OutgoingState.Failed),
        ChatMessageItem("7", null, "me", MessageType.TEXT, "Отправляю…", now, true, outgoingState = OutgoingState.Sending),
        ChatMessageItem("6", 6, "me", MessageType.TEXT, "Доставлено, не прочитано", now, true, outgoingState = OutgoingState.Delivered),
        ChatMessageItem("5", 5, "me", MessageType.TEXT, "На сервере", now, true, outgoingState = OutgoingState.Sent),
        ChatMessageItem("4", 4, "u1", MessageType.TEXT, null, now - 30_000, false, isDeleted = true),
        ChatMessageItem(
            "3", 3, "me", MessageType.TEXT, "Давай в субботу", now - 40_000, true,
            outgoingState = OutgoingState.Read,
            replyTo = ReplyQuote("2", answer)
        ),
        answer.copy(replyTo = ReplyQuote("1", question)),
        question
    )
}

@Preview(showBackground = true)
@Composable
fun ChatContentReplyingPreview() {
    val now = System.currentTimeMillis()
    val messages = previewDirectMessages(now)
    OverGramTheme {
        ChatContent(
            state = ChatUiState(
                type = ChatType.DIRECT,
                title = "Даша",
                currentUserId = "me",
                peer = UserProfile("u1", "Даша", "dasha", null, isOnline = true, lastSeenAt = null),
                isLoading = false,
                messages = messages,
                replyingTo = messages.first { it.clientMessageId == "2" }
            ),
            actions = ChatActions()
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ChatContentEditingPreview() {
    val now = System.currentTimeMillis()
    val messages = previewDirectMessages(now)
    val editing = messages.first { it.clientMessageId == "3" }
    OverGramTheme {
        ChatContent(
            state = ChatUiState(
                type = ChatType.DIRECT,
                title = "Даша",
                currentUserId = "me",
                isLoading = false,
                messages = messages,
                editing = editing,
                input = editing.body.orEmpty()
            ),
            actions = ChatActions()
        )
    }
}
