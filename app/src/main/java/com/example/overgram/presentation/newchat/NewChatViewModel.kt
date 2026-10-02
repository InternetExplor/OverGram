package com.example.overgram.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.GetMyProfileUseCase
import com.example.overgram.domain.usecase.OpenDirectChatUseCase
import com.example.overgram.domain.usecase.SearchUsersUseCase
import com.example.overgram.domain.usecase.SetUsernameUseCase
import com.example.overgram.presentation.chat.ChatArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewChatUiState(
    val query: String = "",
    val results: List<UserProfile> = emptyList(),
    val isSearching: Boolean = false,
    /** The query [results] belong to; null before the first search. */
    val searchedQuery: String? = null,
    val me: MyProfile? = null,
    /** User whose chat is being opened (shows a spinner on that row). */
    val openingUserId: String? = null,
    val error: AuthError? = null,
    /** One-shot: navigate to this chat, then call [NewChatViewModel.onChatOpened]. */
    val openChat: ChatArgs? = null,
    val isUsernameDialogOpen: Boolean = false,
    val usernameInput: String = "",
    val isUsernameInvalid: Boolean = false,
    val usernameError: AuthError? = null,
    val isSavingUsername: Boolean = false,
    val isSessionEnded: Boolean = false
)

@HiltViewModel
class NewChatViewModel @Inject constructor(
    private val searchUsers: SearchUsersUseCase,
    private val openDirectChat: OpenDirectChatUseCase,
    private val getMyProfile: GetMyProfileUseCase,
    private val setUsername: SetUsernameUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    private val myUserId = getCurrentUserId()

    private val _uiState = MutableStateFlow(NewChatUiState())
    val uiState: StateFlow<NewChatUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            when (val outcome = getMyProfile()) {
                is AuthOutcome.Success -> _uiState.update { it.copy(me = outcome.value) }
                is AuthOutcome.Failure -> onError(outcome.error)
            }
        }
    }

    fun onQueryChange(input: String) {
        _uiState.update { it.copy(query = input) }
        val query = input.trim().removePrefix("@")
        searchJob?.cancel()
        if (query.isEmpty()) {
            _uiState.update { it.copy(results = emptyList(), searchedQuery = null, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSearching = true) }
            when (val outcome = searchUsers(query)) {
                is AuthOutcome.Success -> _uiState.update { state ->
                    state.copy(
                        results = outcome.value.filter { it.id != myUserId },
                        searchedQuery = query,
                        isSearching = false
                    )
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isSearching = false) }
                    onError(outcome.error)
                }
            }
        }
    }

    fun onUserClick(user: UserProfile) {
        if (_uiState.value.openingUserId != null) return
        _uiState.update { it.copy(openingUserId = user.id) }
        viewModelScope.launch {
            when (val outcome = openDirectChat(user.id)) {
                is AuthOutcome.Success -> _uiState.update {
                    it.copy(
                        openingUserId = null,
                        openChat = ChatArgs(outcome.value, ChatType.DIRECT, user.id, user.displayName)
                    )
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(openingUserId = null) }
                    onError(outcome.error)
                }
            }
        }
    }

    fun onChatOpened() {
        _uiState.update { it.copy(openChat = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    fun openUsernameDialog() {
        _uiState.update {
            it.copy(
                isUsernameDialogOpen = true,
                usernameInput = it.me?.username.orEmpty(),
                isUsernameInvalid = false,
                usernameError = null
            )
        }
    }

    fun dismissUsernameDialog() {
        _uiState.update { it.copy(isUsernameDialogOpen = false) }
    }

    fun onUsernameChange(input: String) {
        _uiState.update {
            it.copy(usernameInput = input.trim().removePrefix("@"), isUsernameInvalid = false, usernameError = null)
        }
    }

    fun saveUsername() {
        val username = _uiState.value.usernameInput
        if (!SetUsernameUseCase.USERNAME_PATTERN.matches(username)) {
            _uiState.update { it.copy(isUsernameInvalid = true) }
            return
        }
        _uiState.update { it.copy(isSavingUsername = true, usernameError = null) }
        viewModelScope.launch {
            when (val outcome = setUsername(username)) {
                is AuthOutcome.Success -> _uiState.update {
                    it.copy(me = outcome.value, isSavingUsername = false, isUsernameDialogOpen = false)
                }
                is AuthOutcome.Failure -> _uiState.update {
                    it.copy(
                        isSavingUsername = false,
                        usernameError = outcome.error,
                        isSessionEnded = outcome.error.endsSession()
                    )
                }
            }
        }
    }

    private fun onError(error: AuthError) {
        _uiState.update { it.copy(error = error, isSessionEnded = it.isSessionEnded || error.endsSession()) }
    }

    private fun AuthError.endsSession(): Boolean =
        this == AuthError.SessionExpired || this == AuthError.SessionRevoked

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
