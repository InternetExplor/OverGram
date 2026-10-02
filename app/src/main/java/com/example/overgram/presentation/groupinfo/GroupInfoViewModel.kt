package com.example.overgram.presentation.groupinfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.RealtimeEvent
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.usecase.GetChatUseCase
import com.example.overgram.domain.usecase.GetCurrentUserIdUseCase
import com.example.overgram.domain.usecase.GetGroupMembersUseCase
import com.example.overgram.domain.usecase.LeaveChatUseCase
import com.example.overgram.domain.usecase.ObserveConnectionStateUseCase
import com.example.overgram.domain.usecase.ObserveRealtimeEventsUseCase
import com.example.overgram.domain.usecase.RemoveMemberUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupInfoUiState(
    val title: String,
    val currentUserId: String?,
    /** Owner first, then you, then online people, then by name. */
    val members: List<UserProfile> = emptyList(),
    val unknownMemberCount: Int = 0,
    val ownerId: String? = null,
    val isComplete: Boolean = true,
    val isLoading: Boolean = true,
    val loadError: AuthError? = null,
    val error: AuthError? = null,
    /** Asking to confirm removing this member. */
    val confirmRemoval: UserProfile? = null,
    val removingUserId: String? = null,
    val isLeaving: Boolean = false,
    val hasLeft: Boolean = false,
    val isSessionEnded: Boolean = false,
    /** Presence is only current while connected. */
    val isLive: Boolean = true
) {
    /** Only the owner gets member management: admin rights aren't visible from history. */
    val isOwner: Boolean get() = ownerId != null && ownerId == currentUserId

    val memberCount: Int get() = members.size + unknownMemberCount
}

@HiltViewModel(assistedFactory = GroupInfoViewModel.Factory::class)
class GroupInfoViewModel @AssistedInject constructor(
    @Assisted("chatId") private val chatId: String,
    @Assisted("title") title: String,
    private val getGroupMembers: GetGroupMembersUseCase,
    private val getChat: GetChatUseCase,
    private val removeMember: RemoveMemberUseCase,
    private val leaveChat: LeaveChatUseCase,
    observeEvents: ObserveRealtimeEventsUseCase,
    observeConnectionState: ObserveConnectionStateUseCase,
    getCurrentUserId: GetCurrentUserIdUseCase
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(@Assisted("chatId") chatId: String, @Assisted("title") title: String): GroupInfoViewModel
    }

    private val _uiState = MutableStateFlow(GroupInfoUiState(title = title, currentUserId = getCurrentUserId()))
    val uiState: StateFlow<GroupInfoUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeConnectionState().collect { connection ->
                _uiState.update { it.copy(isLive = connection == ConnectionState.Connected) }
            }
        }
        viewModelScope.launch {
            observeEvents().collect { event ->
                when (event) {
                    is RealtimeEvent.Presence -> _uiState.update { state ->
                        state.copy(
                            members = state.members.map { member ->
                                if (member.id != event.userId) member
                                else member.copy(isOnline = event.isOnline, lastSeenAt = event.lastSeenAt)
                            }
                        )
                    }
                    // Someone joined/left/was removed, or the group was renamed.
                    is RealtimeEvent.MemberChanged -> if (event.chatId == chatId) refresh()
                    is RealtimeEvent.ChatChanged -> if (event.chatId == chatId) refresh()
                    RealtimeEvent.SessionEnded -> _uiState.update { it.copy(isSessionEnded = true) }
                    else -> Unit
                }
            }
        }
    }

    /** Called on every start, so the list is fresh after "Add members". */
    fun refresh() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            launch {
                val chat = getChat(chatId)
                if (chat is AuthOutcome.Success) {
                    chat.value.title?.let { title -> _uiState.update { it.copy(title = title) } }
                }
            }
            when (val outcome = getGroupMembers(chatId)) {
                is AuthOutcome.Success -> {
                    val result = outcome.value
                    _uiState.update { state ->
                        state.copy(
                            members = result.members.sortedWith(memberOrder(result.ownerId, state.currentUserId)),
                            unknownMemberCount = result.unknownMemberIds.size,
                            ownerId = result.ownerId,
                            isComplete = result.isComplete,
                            isLoading = false,
                            loadError = null
                        )
                    }
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            // Keep showing an already loaded list; just report the failure.
                            loadError = outcome.error.takeIf { state.members.isEmpty() },
                            error = outcome.error.takeIf { state.members.isNotEmpty() },
                            isSessionEnded = outcome.error.endsSession()
                        )
                    }
                }
            }
        }
    }

    fun askToRemove(user: UserProfile) {
        if (!_uiState.value.isOwner || user.id == _uiState.value.currentUserId) return
        _uiState.update { it.copy(confirmRemoval = user) }
    }

    fun dismissRemoval() {
        _uiState.update { it.copy(confirmRemoval = null) }
    }

    fun confirmRemoval() {
        val user = _uiState.value.confirmRemoval ?: return
        _uiState.update { it.copy(confirmRemoval = null, removingUserId = user.id) }
        viewModelScope.launch {
            when (val outcome = removeMember(chatId, user.id)) {
                is AuthOutcome.Success -> _uiState.update { state ->
                    state.copy(removingUserId = null, members = state.members.filterNot { it.id == user.id })
                }
                is AuthOutcome.Failure -> _uiState.update {
                    it.copy(
                        removingUserId = null,
                        error = outcome.error,
                        isSessionEnded = outcome.error.endsSession()
                    )
                }
            }
        }
    }

    fun leave() {
        if (_uiState.value.isLeaving) return
        _uiState.update { it.copy(isLeaving = true) }
        viewModelScope.launch {
            when (val outcome = leaveChat(chatId)) {
                is AuthOutcome.Success -> _uiState.update { it.copy(isLeaving = false, hasLeft = true) }
                is AuthOutcome.Failure -> _uiState.update {
                    it.copy(isLeaving = false, error = outcome.error, isSessionEnded = outcome.error.endsSession())
                }
            }
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun memberOrder(ownerId: String?, me: String?) =
        compareByDescending<UserProfile> { it.id == ownerId }
            .thenByDescending { it.id == me }
            .thenByDescending { it.isOnline }
            .thenBy { it.displayName.lowercase() }

    private fun AuthError.endsSession(): Boolean =
        this == AuthError.SessionExpired || this == AuthError.SessionRevoked
}
