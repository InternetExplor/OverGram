package com.example.overgram.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.RealtimeEvent
import com.example.overgram.domain.model.SendState
import com.example.overgram.domain.model.StoredMessage
import com.example.overgram.domain.usecase.CreateGroupUseCase
import com.example.overgram.domain.usecase.GetChatUseCase
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.GetUsersUseCase
import com.example.overgram.domain.usecase.LeaveChatUseCase
import com.example.overgram.domain.usecase.LoadOlderMessagesUseCase
import com.example.overgram.domain.usecase.MarkChatReadUseCase
import com.example.overgram.domain.usecase.ObserveConnectionStateUseCase
import com.example.overgram.domain.usecase.ObserveMessagesUseCase
import com.example.overgram.domain.usecase.ObserveReadCursorsUseCase
import com.example.overgram.domain.usecase.ObserveRealtimeEventsUseCase
import com.example.overgram.domain.usecase.RefreshMessagesUseCase
import com.example.overgram.domain.usecase.RenameGroupUseCase
import com.example.overgram.domain.usecase.RetryMessageUseCase
import com.example.overgram.domain.usecase.SendTextMessageUseCase
import com.example.overgram.domain.usecase.SendTypingUseCase
import com.example.overgram.domain.usecase.SetChatMutedUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val args: ChatArgs,
    private val refreshMessages: RefreshMessagesUseCase,
    private val loadOlderMessages: LoadOlderMessagesUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val retryMessage: RetryMessageUseCase,
    private val markChatRead: MarkChatReadUseCase,
    private val getChat: GetChatUseCase,
    private val getUsers: GetUsersUseCase,
    private val setChatMuted: SetChatMutedUseCase,
    private val renameGroup: RenameGroupUseCase,
    private val leaveChat: LeaveChatUseCase,
    private val sendTyping: SendTypingUseCase,
    private val observeConnectionState: ObserveConnectionStateUseCase,
    observeMessages: ObserveMessagesUseCase,
    observeEvents: ObserveRealtimeEventsUseCase,
    observeReadCursors: ObserveReadCursorsUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: ChatArgs): ChatViewModel
    }

    private val myUserId = getCurrentUserId()

    /** The chat's messages as stored on the device (the screen's single source of truth). */
    private var stored: List<StoredMessage> = emptyList()

    /** Users whose profile was already requested, so each is fetched once. */
    private val requestedProfiles = HashSet<String>()

    private var lastMarkedReadSeq = 0L
    private var pollingJob: Job? = null
    private var olderJob: Job? = null
    private var newestJob: Job? = null

    /** The server said there's nothing older than what's stored. */
    private var olderExhausted = false

    /** The screen is started: only then do incoming messages count as read. */
    private var isVisible = false

    /** Highest seq any other member has read; own messages up to it get the double tick. */
    private var othersReadUpTo = 0L

    /** userId → job that clears their "typing…". */
    private val typingTimeouts = HashMap<String, Job>()

    private val _uiState = MutableStateFlow(
        ChatUiState(type = args.type, title = args.title, currentUserId = myUserId)
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Stored history shows at once (also offline); the network only adds to it.
        viewModelScope.launch {
            observeMessages(args.chatId).collect { messages ->
                stored = messages
                if (messages.isNotEmpty()) _uiState.update { it.copy(isLoading = false, loadError = null) }
                publish()
                markReadUpToTop()
            }
        }
        refreshNewest(initial = true)
        refreshDetails()
        viewModelScope.launch { observeEvents().collect(::onEvent) }
        viewModelScope.launch {
            observeReadCursors(args.chatId).collect { cursors ->
                val othersMax = cursors.filterKeys { it != myUserId }.values.maxOrNull() ?: 0L
                if (othersMax > othersReadUpTo) {
                    othersReadUpTo = othersMax
                    publish()
                }
            }
        }
        viewModelScope.launch {
            var wasConnected = observeConnectionState().value == ConnectionState.Connected
            observeConnectionState().collect { connection ->
                val connected = connection == ConnectionState.Connected
                // Reconnected: the global catch-up replays events, but this chat's newest page
                // is the cheapest way to be sure nothing on screen is stale.
                if (connected && !wasConnected) refreshNewest(initial = false)
                wasConnected = connected
            }
        }
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
        // The server rate-limits relays itself (one per 3 s), so every keystroke can just say so.
        if (text.isNotBlank()) sendTyping(args.chatId)
    }

    /** Queues the message: it shows right away and goes out now or once back online. */
    fun send() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty()) return
        _uiState.update { it.copy(input = "") }
        viewModelScope.launch { sendTextMessage(args.chatId, text) }
    }

    /** Tap on a failed bubble: back in the queue with the same clientMessageId, so it can't duplicate. */
    fun retry(clientMessageId: String) {
        viewModelScope.launch { retryMessage(clientMessageId) }
    }

    /** Called when the oldest loaded message scrolls into view. */
    fun loadOlder() {
        if (!_uiState.value.hasOlder || olderJob?.isActive == true) return
        _uiState.update { it.copy(isLoadingOlder = true) }
        olderJob = viewModelScope.launch {
            when (val outcome = loadOlderMessages(args.chatId)) {
                is AuthOutcome.Success -> {
                    olderExhausted = !outcome.value
                    _uiState.update { it.copy(isLoadingOlder = false) }
                    publish()
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isLoadingOlder = false) }
                    onError(outcome.error)
                }
            }
        }
    }

    fun retryLoad() {
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        refreshNewest(initial = true)
        refreshDetails()
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun toggleMute() {
        val muted = !_uiState.value.isMuted
        _uiState.update { it.copy(isMuted = muted) } // optimistic
        viewModelScope.launch {
            when (val outcome = setChatMuted(args.chatId, muted)) {
                is AuthOutcome.Success -> applyDetails(outcome.value)
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isMuted = !muted) }
                    onError(outcome.error)
                }
            }
        }
    }

    fun openRenameDialog() {
        _uiState.update { it.copy(renameDialog = RenameDialogState(input = it.title)) }
    }

    fun onRenameInput(text: String) {
        _uiState.update { state ->
            state.copy(
                renameDialog = state.renameDialog?.copy(
                    input = text.take(CreateGroupUseCase.MAX_TITLE_LENGTH),
                    error = null
                )
            )
        }
    }

    fun dismissRenameDialog() {
        _uiState.update { it.copy(renameDialog = null) }
    }

    fun saveRename() {
        val dialog = _uiState.value.renameDialog ?: return
        val title = dialog.input.trim()
        if (title.isEmpty() || dialog.isSaving) return
        _uiState.update { it.copy(renameDialog = dialog.copy(isSaving = true, error = null)) }
        viewModelScope.launch {
            when (val outcome = renameGroup(args.chatId, title)) {
                is AuthOutcome.Success -> {
                    applyDetails(outcome.value)
                    _uiState.update { it.copy(renameDialog = null) }
                }
                is AuthOutcome.Failure -> _uiState.update { state ->
                    state.copy(renameDialog = state.renameDialog?.copy(isSaving = false, error = outcome.error))
                }
            }
        }
    }

    fun leave() {
        if (_uiState.value.isLeaving) return
        _uiState.update { it.copy(isLeaving = true) }
        viewModelScope.launch {
            when (val outcome = leaveChat(args.chatId)) {
                is AuthOutcome.Success -> {
                    stopPolling()
                    _uiState.update { it.copy(isLeaving = false, hasLeft = true) }
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isLeaving = false) }
                    onError(outcome.error)
                }
            }
        }
    }

    /**
     * Screen started. Live changes arrive over the WebSocket; polling is the fallback, frequent
     * only while the socket is down.
     */
    fun startPolling() {
        isVisible = true
        markReadUpToTop() // messages that arrived while in the background
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                val connected = observeConnectionState().value == ConnectionState.Connected
                delay(if (connected) POLL_INTERVAL_CONNECTED_MS else POLL_INTERVAL_OFFLINE_MS)
                refreshNewest(initial = false)
                if (connected || ++tick % DETAILS_EVERY_N_POLLS == 0) refreshDetails()
            }
        }
    }

    fun stopPolling() {
        isVisible = false
        pollingJob?.cancel()
        pollingJob = null
    }

    /** Messages themselves arrive through the database (RealtimeSync); this handles the rest. */
    private fun onEvent(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.MessageNew -> if (event.chatId == args.chatId) clearTyping(event.message.senderId)
            is RealtimeEvent.ChatChanged -> if (event.chatId == args.chatId) refreshDetails()
            is RealtimeEvent.MemberChanged -> if (event.chatId == args.chatId) refreshDetails()
            is RealtimeEvent.Presence -> _uiState.update { state ->
                val peer = state.peer?.takeIf { it.id == event.userId }
                    ?.copy(isOnline = event.isOnline, lastSeenAt = event.lastSeenAt)
                val profile = state.profiles[event.userId]
                    ?.copy(isOnline = event.isOnline, lastSeenAt = event.lastSeenAt)
                state.copy(
                    peer = peer ?: state.peer,
                    profiles = if (profile != null) state.profiles + (event.userId to profile) else state.profiles
                )
            }
            is RealtimeEvent.Typing -> if (event.chatId == args.chatId && event.userId != myUserId) {
                showTyping(event.userId)
            }
            RealtimeEvent.Resynced -> refreshNewest(initial = true)
            RealtimeEvent.SessionEnded -> onError(AuthError.SessionExpired)
            // Edits/deletes are stored by RealtimeSync; read cursors come via observeReadCursors.
            is RealtimeEvent.MessageEdited,
            is RealtimeEvent.MessageDeleted,
            is RealtimeEvent.ReadReceipt,
            is RealtimeEvent.Delivered -> Unit
        }
    }

    private fun showTyping(userId: String) {
        _uiState.update { it.copy(typingUserIds = it.typingUserIds + userId) }
        typingTimeouts.remove(userId)?.cancel()
        typingTimeouts[userId] = viewModelScope.launch {
            delay(TYPING_TIMEOUT_MS)
            clearTyping(userId)
        }
    }

    private fun clearTyping(userId: String) {
        typingTimeouts.remove(userId)?.cancel()
        _uiState.update { it.copy(typingUserIds = it.typingUserIds - userId) }
    }

    private fun refreshNewest(initial: Boolean) {
        if (!initial && newestJob?.isActive == true) return
        newestJob = viewModelScope.launch {
            when (val outcome = refreshMessages(args.chatId)) {
                // The stored-messages flow delivers the result.
                is AuthOutcome.Success -> _uiState.update { it.copy(isLoading = false, loadError = null) }
                is AuthOutcome.Failure -> {
                    // Nothing stored and nothing loaded: show the error instead of an empty chat.
                    if (initial && stored.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, loadError = outcome.error) }
                    }
                    // Background refreshes stay quiet unless the session is gone.
                    if (outcome.error.endsSession()) onError(outcome.error)
                }
            }
        }
    }

    private fun refreshDetails() {
        viewModelScope.launch {
            val outcome = getChat(args.chatId)
            if (outcome is AuthOutcome.Success) applyDetails(outcome.value)
        }
    }

    private fun applyDetails(chat: ChatSummary) {
        _uiState.update { state ->
            state.copy(
                title = when (chat.type) {
                    ChatType.GROUP -> chat.title ?: state.title
                    ChatType.DIRECT -> chat.peer?.displayName ?: state.title
                },
                peer = chat.peer ?: state.peer,
                isMuted = chat.isMuted
            )
        }
    }

    private fun markReadUpToTop() {
        if (!isVisible) return
        val top = stored.mapNotNull { it.serverSeq }.maxOrNull() ?: return
        if (top <= lastMarkedReadSeq) return
        lastMarkedReadSeq = top
        viewModelScope.launch {
            // Best effort: max-wins on the server, the next refresh tries again on failure.
            if (markChatRead(args.chatId, top) is AuthOutcome.Failure) {
                lastMarkedReadSeq = minOf(lastMarkedReadSeq, top - 1)
            }
        }
    }

    /** Maps stored messages (already in display order) to bubbles. */
    private fun publish() {
        val oldestSeq = stored.mapNotNull { it.serverSeq }.minOrNull()
        _uiState.update {
            it.copy(
                messages = stored.map { message -> message.toItem() },
                // seq starts at 1 per chat, so anything above it means older history exists.
                hasOlder = !olderExhausted && oldestSeq != null && oldestSeq > 1
            )
        }
        if (args.type == ChatType.GROUP) loadMissingProfiles()
    }

    /** Group bubbles and system notes need the names of everyone they mention. */
    private fun loadMissingProfiles() {
        val missing = stored
            .flatMap { message ->
                listOf(message.senderId) + message.systemEvent?.let { listOf(it.actorId) + it.targetUserIds }.orEmpty()
            }
            .filter { it != myUserId && requestedProfiles.add(it) }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val loaded = getUsers(missing)
            // Allow another attempt later for the ones that failed.
            requestedProfiles -= (missing - loaded.keys).toSet()
            _uiState.update { it.copy(profiles = it.profiles + loaded) }
        }
    }

    private fun StoredMessage.toItem(): ChatMessageItem {
        val isOwn = senderId == myUserId
        val seq = serverSeq
        return ChatMessageItem(
            clientMessageId = clientMessageId,
            serverSeq = seq,
            senderId = senderId,
            type = type,
            body = body,
            createdAt = createdAt,
            isOutgoing = isOwn && type != MessageType.SYSTEM,
            isEdited = isEdited,
            isDeleted = isDeleted,
            outgoingState = when {
                !isOwn -> null
                sendState == SendState.SENDING -> OutgoingState.Sending
                sendState == SendState.FAILED -> OutgoingState.Failed
                seq != null && seq <= othersReadUpTo -> OutgoingState.Read
                else -> OutgoingState.Sent
            },
            systemEvent = systemEvent,
            failureReason = failureReason
        )
    }

    private fun onError(error: AuthError) {
        _uiState.update {
            it.copy(error = error, isSessionEnded = it.isSessionEnded || error.endsSession())
        }
    }

    private fun AuthError.endsSession(): Boolean =
        this == AuthError.SessionExpired || this == AuthError.SessionRevoked

    private companion object {
        const val POLL_INTERVAL_OFFLINE_MS = 4_000L

        /** Safety net only: the WebSocket delivers changes as they happen. */
        const val POLL_INTERVAL_CONNECTED_MS = 60_000L

        /** Clients repeat `typing` every few seconds while typing; silence means they stopped. */
        const val TYPING_TIMEOUT_MS = 5_000L

        /** While offline, presence, title and mute are refreshed every ~16 s. */
        const val DETAILS_EVERY_N_POLLS = 4
    }
}
