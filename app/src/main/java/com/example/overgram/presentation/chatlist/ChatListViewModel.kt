package com.example.overgram.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.usecase.GetChatsUseCase
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.LogoutUseCase
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
    private val getChats: GetChatsUseCase,
    private val logoutUseCase: LogoutUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState(currentUserId = getCurrentUserId()))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var pollingJob: Job? = null

    init {
        load()
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
     * Online status only changes server-side while there is no realtime connection yet,
     * so the list is re-pulled periodically while the screen is visible.
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                load(silent = true)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun logout() {
        stopPolling()
        loadJob?.cancel()
        viewModelScope.launch {
            // Local logout always happens, so the outcome doesn't matter here.
            logoutUseCase()
            _uiState.update { it.copy(isSessionEnded = true) }
        }
    }

    /** [silent] background polls keep the current list and error untouched on failure. */
    private fun load(force: Boolean = false, silent: Boolean = false) {
        if (loadJob?.isActive == true) {
            if (!force) return
            loadJob?.cancel()
        }
        loadJob = viewModelScope.launch {
            val outcome = getChats()
            _uiState.update { state ->
                when (outcome) {
                    is AuthOutcome.Success -> state.copy(
                        chats = outcome.value,
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
        const val POLL_INTERVAL_MS = 30_000L
    }
}
