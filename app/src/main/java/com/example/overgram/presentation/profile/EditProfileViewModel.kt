package com.example.overgram.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.usecase.GetMyProfileUseCase
import com.example.overgram.domain.usecase.ObserveMyProfileUseCase
import com.example.overgram.domain.usecase.RemoveAvatarUseCase
import com.example.overgram.domain.usecase.SetAvatarUseCase
import com.example.overgram.domain.usecase.SetUsernameUseCase
import com.example.overgram.domain.usecase.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UsernameProblem { Invalid, CannotRemove }

data class EditProfileUiState(
    val me: MyProfile? = null,
    val name: String = "",
    val username: String = "",
    val isNameBlank: Boolean = false,
    val usernameProblem: UsernameProblem? = null,
    /** Server refused the username (taken…). */
    val usernameError: AuthError? = null,
    val isSaving: Boolean = false,
    val isUpdatingAvatar: Boolean = false,
    /** Transient error for a snackbar (avatar upload, network…). */
    val error: AuthError? = null,
    /** One-shot: saved, close the screen. */
    val isSaved: Boolean = false
) {
    private val nameChanged: Boolean get() = me != null && name.trim() != me.displayName
    private val usernameChanged: Boolean get() = me != null && username.trim() != me.username.orEmpty()

    val hasChanges: Boolean get() = nameChanged || usernameChanged

    val canSave: Boolean
        get() = hasChanges && !isSaving && !isNameBlank && usernameProblem == null && usernameError == null

    fun changedName(): String? = name.trim().takeIf { nameChanged }
    fun changedUsername(): String? = username.trim().takeIf { usernameChanged }
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val getMyProfile: GetMyProfileUseCase,
    private val updateProfile: UpdateProfileUseCase,
    private val setAvatar: SetAvatarUseCase,
    private val removeAvatar: RemoveAvatarUseCase,
    observeMyProfile: ObserveMyProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState().withProfile(observeMyProfile().value))
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        if (_uiState.value.me == null) {
            viewModelScope.launch {
                when (val outcome = getMyProfile()) {
                    is AuthOutcome.Success -> _uiState.update { it.withProfile(outcome.value) }
                    is AuthOutcome.Failure -> _uiState.update { it.copy(error = outcome.error) }
                }
            }
        }
    }

    fun onNameChange(text: String) {
        val name = text.take(UpdateProfileUseCase.MAX_NAME_LENGTH)
        _uiState.update { it.copy(name = name, isNameBlank = name.isBlank()) }
    }

    fun onUsernameChange(text: String) {
        val username = text.trim().removePrefix("@")
        _uiState.update { state ->
            state.copy(
                username = username,
                usernameError = null,
                usernameProblem = when {
                    // The server has no "remove username": min length 3 once set.
                    username.isEmpty() && state.me?.username != null -> UsernameProblem.CannotRemove
                    username.isEmpty() -> null
                    !SetUsernameUseCase.USERNAME_PATTERN.matches(username) -> UsernameProblem.Invalid
                    else -> null
                }
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val outcome = updateProfile(state.changedName(), state.changedUsername())) {
                is AuthOutcome.Success -> _uiState.update { it.copy(isSaving = false, isSaved = true) }
                is AuthOutcome.Failure -> _uiState.update {
                    if (outcome.error is AuthError.Validation) {
                        // Taken / invalid username: show it under the field.
                        it.copy(isSaving = false, usernameError = outcome.error)
                    } else {
                        it.copy(isSaving = false, error = outcome.error)
                    }
                }
            }
        }
    }

    /** [uri] from the system photo picker. */
    fun onAvatarPicked(uri: String) = changeAvatar { setAvatar(uri) }

    fun onRemoveAvatar() = changeAvatar { removeAvatar() }

    fun onErrorShown() {
        _uiState.update { it.copy(error = null) }
    }

    private fun changeAvatar(action: suspend () -> AuthOutcome<MyProfile>) {
        if (_uiState.value.isUpdatingAvatar) return
        _uiState.update { it.copy(isUpdatingAvatar = true) }
        viewModelScope.launch {
            when (val outcome = action()) {
                // Only the avatar changed: keep whatever the user is typing.
                is AuthOutcome.Success -> _uiState.update { it.copy(me = outcome.value, isUpdatingAvatar = false) }
                is AuthOutcome.Failure -> _uiState.update { it.copy(isUpdatingAvatar = false, error = outcome.error) }
            }
        }
    }

    private fun EditProfileUiState.withProfile(profile: MyProfile?): EditProfileUiState =
        if (profile == null) this
        else copy(me = profile, name = profile.displayName, username = profile.username.orEmpty())
}
