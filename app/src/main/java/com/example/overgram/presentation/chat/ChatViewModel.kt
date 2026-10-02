package com.example.overgram.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.Message
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.usecase.CreateGroupUseCase
import com.example.overgram.domain.usecase.GetChatUseCase
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.GetMessagesUseCase
import com.example.overgram.domain.usecase.GetUsersUseCase
import com.example.overgram.domain.usecase.LeaveChatUseCase
import com.example.overgram.domain.usecase.MarkChatReadUseCase
import com.example.overgram.domain.usecase.RenameGroupUseCase
import com.example.overgram.domain.usecase.SendTextMessageUseCase
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
import java.util.UUID

@HiltViewModel(assistedFactory = ChatViewModel.Factory::class)
class ChatViewModel @AssistedInject constructor(
    @Assisted private val args: ChatArgs,
    private val getMessages: GetMessagesUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val markChatRead: MarkChatReadUseCase,
    private val getChat: GetChatUseCase,
    private val getUsers: GetUsersUseCase,
    private val setChatMuted: SetChatMutedUseCase,
    private val renameGroup: RenameGroupUseCase,
    private val leaveChat: LeaveChatUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: ChatArgs): ChatViewModel
    }

    private val myUserId = getCurrentUserId()

    /** Server-confirmed messages by clientMessageId. */
    private val confirmed = HashMap<String, Message>()

    /** Own messages not yet confirmed: being sent, or failed. */
    private val pending = LinkedHashMap<String, ChatMessageItem>()

    /** Users whose profile was already requested, so each is fetched once. */
    private val requestedProfiles = HashSet<String>()

    private var lastMarkedReadSeq = 0L
    private var pollingJob: Job? = null
    private var olderJob: Job? = null
    private var newestJob: Job? = null

    private val _uiState = MutableStateFlow(
        ChatUiState(type = args.type, title = args.title, currentUserId = myUserId)
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        loadNewest(initial = true)
        refreshDetails()
    }

    fun onInputChange(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun send() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty()) return
        val item = ChatMessageItem(
            clientMessageId = UUID.randomUUID().toString(),
            serverSeq = null,
            senderId = myUserId.orEmpty(),
            type = MessageType.TEXT,
            body = text,
            createdAt = System.currentTimeMillis(),
            isOutgoing = true,
            outgoingState = OutgoingState.Sending
        )
        pending[item.clientMessageId] = item
        _uiState.update { it.copy(input = "") }
        publish()
        deliver(item)
    }

    /** Tap on a failed bubble: resend with the same clientMessageId, so it can't duplicate. */
    fun retry(clientMessageId: String) {
        val item = pending[clientMessageId]?.takeIf { it.outgoingState == OutgoingState.Failed } ?: return
        val retrying = item.copy(outgoingState = OutgoingState.Sending)
        pending[clientMessageId] = retrying
        publish()
        deliver(retrying)
    }

    /** Called when the oldest loaded message scrolls into view. */
    fun loadOlder() {
        val state = _uiState.value
        if (!state.hasOlder || olderJob?.isActive == true) return
        val oldestSeq = confirmed.values.minOfOrNull { it.serverSeq } ?: return

        _uiState.update { it.copy(isLoadingOlder = true) }
        olderJob = viewModelScope.launch {
            when (val outcome = getMessages(args.chatId, beforeSeq = oldestSeq)) {
                is AuthOutcome.Success -> {
                    outcome.value.messages.forEach { confirmed[it.clientMessageId] = it }
                    _uiState.update { it.copy(isLoadingOlder = false, hasOlder = outcome.value.hasMore) }
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
        loadNewest(initial = true)
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
     * Until the WebSocket lands, new messages and chat details (presence, title, mute) are
     * pulled periodically while the screen is visible.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                loadNewest(initial = false)
                if (++tick % DETAILS_EVERY_N_POLLS == 0) refreshDetails()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun deliver(item: ChatMessageItem) {
        viewModelScope.launch {
            when (val outcome = sendTextMessage(args.chatId, item.clientMessageId, item.body.orEmpty())) {
                is AuthOutcome.Success -> {
                    pending.remove(item.clientMessageId)
                    val sent = outcome.value
                    // Keep a poll that already delivered the echo; it is the same message.
                    confirmed.getOrPut(item.clientMessageId) {
                        Message(
                            clientMessageId = item.clientMessageId,
                            serverId = sent.serverId,
                            serverSeq = sent.serverSeq,
                            senderId = myUserId.orEmpty(),
                            type = MessageType.TEXT,
                            body = item.body,
                            createdAt = sent.createdAt,
                            isEdited = false,
                            isDeleted = false
                        )
                    }
                }
                is AuthOutcome.Failure -> {
                    // A poll may have confirmed it meanwhile (lost response, message stored).
                    if (pending.containsKey(item.clientMessageId)) {
                        pending[item.clientMessageId] = item.copy(outgoingState = OutgoingState.Failed)
                    }
                    // Not a member any more, rate limited…: say why, the bubble alone can't.
                    if (outcome.error !is AuthError.Network) onError(outcome.error)
                }
            }
            publish()
        }
    }

    private fun loadNewest(initial: Boolean) {
        if (!initial && newestJob?.isActive == true) return
        newestJob = viewModelScope.launch {
            when (val outcome = getMessages(args.chatId)) {
                is AuthOutcome.Success -> {
                    val page = outcome.value
                    val knownTop = confirmed.values.maxOfOrNull { it.serverSeq }
                    val pageBottom = page.messages.minOfOrNull { it.serverSeq }
                    // More than a page arrived since the last poll: drop the stale history
                    // rather than show a hole in the middle of it.
                    val hasGap = knownTop != null && pageBottom != null && pageBottom > knownTop + 1
                    if (hasGap) confirmed.clear()

                    page.messages.forEach {
                        confirmed[it.clientMessageId] = it
                        pending.remove(it.clientMessageId)
                    }
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            loadError = null,
                            hasOlder = if (initial || hasGap || knownTop == null) page.hasMore else state.hasOlder
                        )
                    }
                    publish()
                    markReadUpToTop()
                }
                is AuthOutcome.Failure -> {
                    if (initial && confirmed.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, loadError = outcome.error) }
                    }
                    // Background polls stay quiet unless the session is gone.
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
        val top = confirmed.values.maxOfOrNull { it.serverSeq } ?: return
        if (top <= lastMarkedReadSeq) return
        lastMarkedReadSeq = top
        viewModelScope.launch {
            // Best effort: max-wins on the server, the next poll tries again on failure.
            if (markChatRead(args.chatId, top) is AuthOutcome.Failure) {
                lastMarkedReadSeq = minOf(lastMarkedReadSeq, top - 1)
            }
        }
    }

    /** Rebuilds the visible list: pending messages on top (newest), then history by seq. */
    private fun publish() {
        val history = confirmed.values
            .sortedByDescending { it.serverSeq }
            .map { it.toItem() }
        val outgoing = pending.values.sortedByDescending { it.createdAt }
        _uiState.update { it.copy(messages = outgoing + history) }
        if (args.type == ChatType.GROUP) loadMissingProfiles()
    }

    /** Group bubbles and system notes need the names of everyone they mention. */
    private fun loadMissingProfiles() {
        val missing = confirmed.values
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

    private fun Message.toItem(): ChatMessageItem {
        val isOwn = senderId == myUserId
        return ChatMessageItem(
            clientMessageId = clientMessageId,
            serverSeq = serverSeq,
            senderId = senderId,
            type = type,
            body = body,
            createdAt = createdAt,
            isOutgoing = isOwn && type != MessageType.SYSTEM,
            isEdited = isEdited,
            isDeleted = isDeleted,
            outgoingState = if (isOwn) OutgoingState.Sent else null,
            systemEvent = systemEvent
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
        const val POLL_INTERVAL_MS = 4_000L

        /** Presence, title and mute are refreshed every ~16 s. */
        const val DETAILS_EVERY_N_POLLS = 4
    }
}
