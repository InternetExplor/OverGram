package com.example.overgram.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.ServerInfo
import com.example.overgram.domain.usecase.ClearCacheUseCase
import com.example.overgram.domain.usecase.GetMyProfileUseCase
import com.example.overgram.domain.usecase.GetServerInfoUseCase
import com.example.overgram.domain.usecase.LogoutUseCase
import com.example.overgram.domain.usecase.ObserveMyProfileUseCase
import com.example.overgram.domain.usecase.SetRealtimeActiveUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val me: MyProfile? = null,
    val serverInfo: ServerInfo? = null,
    /** Profile couldn't be loaded and there's nothing to show. */
    val loadError: AuthError? = null,
    val isClearingCache: Boolean = false,
    /** One-shot: show "cache cleared". */
    val cacheCleared: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isSessionEnded: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getMyProfile: GetMyProfileUseCase,
    private val getServerInfo: GetServerInfoUseCase,
    private val clearCacheUseCase: ClearCacheUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val setRealtimeActive: SetRealtimeActiveUseCase,
    observeMyProfile: ObserveMyProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(me = observeMyProfile().value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Edits made on the profile screen show up here right away.
        viewModelScope.launch {
            observeMyProfile().collect { me -> if (me != null) _uiState.update { it.copy(me = me) } }
        }
        viewModelScope.launch {
            val info = getServerInfo()
            if (info is AuthOutcome.Success) _uiState.update { it.copy(serverInfo = info.value) }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val outcome = getMyProfile()) {
                is AuthOutcome.Success -> _uiState.update { it.copy(loadError = null) }
                is AuthOutcome.Failure -> _uiState.update { state ->
                    val ended = outcome.error == AuthError.SessionExpired || outcome.error == AuthError.SessionRevoked
                    state.copy(
                        loadError = outcome.error.takeIf { state.me == null },
                        isSessionEnded = state.isSessionEnded || ended
                    )
                }
            }
        }
    }

    fun clearCache() {
        if (_uiState.value.isClearingCache) return
        _uiState.update { it.copy(isClearingCache = true) }
        viewModelScope.launch {
            clearCacheUseCase()
            _uiState.update { it.copy(isClearingCache = false, cacheCleared = true) }
        }
    }

    fun onCacheClearedShown() {
        _uiState.update { it.copy(cacheCleared = false) }
    }

    fun logout() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        setRealtimeActive(false)
        viewModelScope.launch {
            // Local logout always happens (and clears local data), whatever the server says.
            logoutUseCase()
            _uiState.update { it.copy(isLoggingOut = false, isSessionEnded = true) }
        }
    }
}
