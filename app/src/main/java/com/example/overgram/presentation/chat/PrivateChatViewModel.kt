package com.example.overgram.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.Message
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.GetMessagesUseCase
import com.example.overgram.domain.usecase.GetUserUseCase
import com.example.overgram.domain.usecase.MarkChatReadUseCase
import com.example.overgram.domain.usecase.SendTextMessageUseCase
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

@HiltViewModel(assistedFactory = PrivateChatViewModel.Factory::class)
class PrivateChatViewModel @AssistedInject constructor(
    @Assisted private val args: PrivateChatArgs,
    private val getMessages: GetMessagesUseCase,
    private val sendTextMessage: SendTextMessageUseCase,
    private val markChatRead: MarkChatReadUseCase,
    private val getUser: GetUserUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(args: PrivateChatArgs): PrivateChatViewModel
    }

    private val myUserId = getCurrentUserId()

    /** Server-confirmed messages by clientMessageId. */
    private val confirmed = HashMap<String, Message>()

    /** Own messages not yet confirmed: being sent, or failed. */
    private val pending = LinkedHashMap<String, ChatMessageItem>()

    private var lastMarkedReadSeq = 0L
    private var pollingJob: Job? = null
    private var olderJob: Job? = null
    private var newestJob: Job? = null

    private val _uiState = MutableStateFlow(PrivateChatUiState(title = args.title))
    val uiState: StateFlow<PrivateChatUiState> = _uiState.asStateFlow()

    init {
        loadNewest(initial = true)
        refreshPeer()
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
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Until the WebSocket lands, new messages and the peer's presence are pulled
     * periodically while the screen is visible.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                loadNewest(initial = false)
                if (++tick % PRESENCE_EVERY_N_POLLS == 0) refreshPeer()
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
                    if (outcome.error.endsSession()) onError(outcome.error)
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

    private fun refreshPeer() {
        val peerId = args.peerUserId ?: return
        viewModelScope.launch {
            val outcome = getUser(peerId)
            if (outcome is AuthOutcome.Success) _uiState.update { it.copy(peer = outcome.value) }
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
    }

    private fun Message.toItem(): ChatMessageItem {
        val isOwn = senderId == myUserId
        return ChatMessageItem(
            clientMessageId = clientMessageId,
            serverSeq = serverSeq,
            type = type,
            body = body,
            createdAt = createdAt,
            isOutgoing = isOwn,
            isEdited = isEdited,
            isDeleted = isDeleted,
            outgoingState = if (isOwn) OutgoingState.Sent else null
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

        /** Presence is refreshed every ~16 s. */
        const val PRESENCE_EVERY_N_POLLS = 4
    }
}
