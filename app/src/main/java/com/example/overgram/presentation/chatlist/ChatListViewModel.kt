package com.example.overgram.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.RealtimeEvent
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.ObserveChatsUseCase
import com.example.overgram.domain.usecase.ObserveConnectionStateUseCase
import com.example.overgram.domain.usecase.ObserveRealtimeEventsUseCase
import com.example.overgram.domain.usecase.RefreshChatsUseCase
import com.example.overgram.domain.usecase.SetRealtimeActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val refreshChats: RefreshChatsUseCase,
    observeChats: ObserveChatsUseCase,
    private val setRealtimeActive: SetRealtimeActiveUseCase,
    observeEvents: ObserveRealtimeEventsUseCase,
    observeConnectionState: ObserveConnectionStateUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState(currentUserId = getCurrentUserId()))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var reloadJob: Job? = null
    private var pollingJob: Job? = null

    /** "chatId/userId" → job that clears that typing indicator. */
    private val typingTimeouts = HashMap<String, Job>()

    init {
        // Right after login the activity's onStart has already run, so connect from here too.
        setRealtimeActive(true)
        // The stored list shows at once (also offline); the network only refreshes it.
        viewModelScope.launch {
            observeChats().collect { chats ->
                _uiState.update {
                    it.copy(chats = chats, isLoading = it.isLoading && chats.isEmpty())
                }
            }
        }
        load()
        viewModelScope.launch { observeEvents().collect(::onEvent) }
        viewModelScope.launch {
            observeConnectionState().collect { connection ->
                val wasConnected = _uiState.value.connectionState == ConnectionState.Connected
                _uiState.update { it.copy(connectionState = connection) }
                // Back online after a gap: the catch-up fires events, but a resync is cheap insurance.
                if (connection == ConnectionState.Connected && !wasConnected && !_uiState.value.isLoading) {
                    scheduleReload()
                }
            }
        }
    }

    /** Pull-to-refresh. */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        load(force = true)
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        load(force = true)
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Live updates come over the WebSocket; this poll is only a safety net (frequent while
     * the socket is down). It also reloads right away when coming back from a chat.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            // On first start this is a no-op: the initial load is still running.
            while (isActive) {
                load(silent = true)
                val connected = _uiState.value.connectionState == ConnectionState.Connected
                delay(if (connected) POLL_INTERVAL_CONNECTED_MS else POLL_INTERVAL_OFFLINE_MS)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    private fun onEvent(event: RealtimeEvent) {
        when (event) {
            is RealtimeEvent.MessageNew -> {
                clearTyping(event.chatId, event.message.senderId)
                scheduleReload()
            }
            // Unread counts, previews, titles and membership all come from the server's list.
            is RealtimeEvent.MessageEdited,
            is RealtimeEvent.MessageDeleted,
            is RealtimeEvent.ReadReceipt,
            is RealtimeEvent.MemberChanged,
            is RealtimeEvent.ChatChanged,
            RealtimeEvent.Resynced -> scheduleReload()
            // Stored by RealtimeSync; the observed list picks it up.
            is RealtimeEvent.Presence -> Unit
            is RealtimeEvent.Typing -> if (event.userId != _uiState.value.currentUserId) {
                showTyping(event.chatId, event.userId)
            }
            RealtimeEvent.SessionEnded -> _uiState.update { it.copy(isSessionEnded = true) }
            is RealtimeEvent.Delivered -> Unit
        }
    }

    /** Bursts of events (a catch-up replay) collapse into one list reload. */
    private fun scheduleReload() {
        reloadJob?.cancel()
        reloadJob = viewModelScope.launch {
            delay(RELOAD_DEBOUNCE_MS)
            load(force = true, silent = true)
        }
    }

    private fun showTyping(chatId: String, userId: String) {
        _uiState.update { state ->
            state.copy(typing = state.typing + (chatId to (state.typing[chatId].orEmpty() + userId)))
        }
        val key = "$chatId/$userId"
        typingTimeouts.remove(key)?.cancel()
        typingTimeouts[key] = viewModelScope.launch {
            delay(TYPING_TIMEOUT_MS)
            clearTyping(chatId, userId)
        }
    }

    private fun clearTyping(chatId: String, userId: String) {
        typingTimeouts.remove("$chatId/$userId")?.cancel()
        _uiState.update { state ->
            val left = state.typing[chatId].orEmpty() - userId
            state.copy(typing = if (left.isEmpty()) state.typing - chatId else state.typing + (chatId to left))
        }
    }

    /** [silent] background loads keep the current list and error untouched on failure. */
    private fun load(force: Boolean = false, silent: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!force) return
            loadJob?.cancel()
        }
        loadJob = viewModelScope.launch {
            val outcome = refreshChats() // writes the stored list, which observeChats delivers
            _uiState.update { state ->
                when (outcome) {
                    is AuthOutcome.Success -> state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                    is AuthOutcome.Failure -> state.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = if (silent && !outcome.error.endsSession()) state.error else outcome.error,
                        isSessionEnded = outcome.error.endsSession()
                    )
                }
            }
        }
    }

    private fun AuthError.endsSession(): Boolean =
        this == AuthError.SessionExpired || this == AuthError.SessionRevoked

    private companion object {
        const val POLL_INTERVAL_CONNECTED_MS = 60_000L
        const val POLL_INTERVAL_OFFLINE_MS = 30_000L
        const val RELOAD_DEBOUNCE_MS = 400L

        /** Clients repeat `typing` every few seconds while typing; silence means they stopped. */
        const val TYPING_TIMEOUT_MS = 5_000L
    }
}
