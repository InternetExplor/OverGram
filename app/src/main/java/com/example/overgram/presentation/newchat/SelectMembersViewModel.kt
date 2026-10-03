package com.example.overgram.presentation.newchat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.usecase.AddMembersUseCase
import com.example.overgram.domain.usecase.CreateGroupUseCase
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.SearchUsersUseCase
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
import kotlinx.coroutines.launch

data class SelectMembersUiState(
    /** Null: creating a new group. Otherwise: adding members to this chat. */
    val addToChatId: String?,
    val title: String = "",
    val query: String = "",
    val results: List<UserProfile> = emptyList(),
    val searchedQuery: String? = null,
    val isSearching: Boolean = false,
    /** In the order they were picked. */
    val selected: List<UserProfile> = emptyList(),
    val isSubmitting: Boolean = false,
    val error: AuthError? = null,
    /** One-shot: the group was created (its id) or members were added (the chat id). */
    val doneChatId: String? = null,
    val isSessionEnded: Boolean = false
) {
    val isCreating: Boolean get() = addToChatId == null

    val canSubmit: Boolean
        get() = !isSubmitting && selected.isNotEmpty() && (!isCreating || title.isNotBlank())

    fun isSelected(user: UserProfile): Boolean = selected.any { it.id == user.id }
}

@HiltViewModel(assistedFactory = SelectMembersViewModel.Factory::class)
class SelectMembersViewModel @AssistedInject constructor(
    @Assisted addToChatId: String?,
    private val searchUsers: SearchUsersUseCase,
    private val createGroup: CreateGroupUseCase,
    private val addMembers: AddMembersUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(addToChatId: String?): SelectMembersViewModel
    }

    private val myUserId = getCurrentUserId()

    private val _uiState = MutableStateFlow(SelectMembersUiState(addToChatId = addToChatId))
    val uiState: StateFlow<SelectMembersUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onTitleChange(text: String) {
        _uiState.update { it.copy(title = text.take(CreateGroupUseCase.MAX_TITLE_LENGTH)) }
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

    fun toggle(user: UserProfile) {
        _uiState.update { state ->
            state.copy(
                selected = if (state.isSelected(user)) {
                    state.selected.filterNot { it.id == user.id }
                } else {
                    state.selected + user
                }
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, error = null) }
        val ids = state.selected.map { it.id }
        viewModelScope.launch {
            val outcome = when (val chatId = state.addToChatId) {
                null -> createGroup(state.title, ids)
                else -> when (val added = addMembers(chatId, ids)) {
                    is AuthOutcome.Success -> AuthOutcome.Success(chatId)
                    is AuthOutcome.Failure -> added
                }
            }
            when (outcome) {
                is AuthOutcome.Success -> _uiState.update {
                    it.copy(isSubmitting = false, doneChatId = outcome.value)
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    onError(outcome.error)
                }
            }
        }
    }

    fun onDoneHandled() {
        _uiState.update { it.copy(doneChatId = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun onError(error: AuthError) {
        val endsSession = error == AuthError.SessionExpired || error == AuthError.SessionRevoked
        _uiState.update { it.copy(error = error, isSessionEnded = it.isSessionEnded || endsSession) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
