package com.example.overgram.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.usecase.RequestOtpUseCase
import com.example.overgram.domain.usecase.VerifyOtpUseCase
import com.example.overgram.presentation.auth.AuthUiState.Companion.PHONE_LOCAL_LENGTH
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val requestOtp: RequestOtpUseCase,
    private val verifyOtp: VerifyOtpUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var cooldownJob: Job? = null

    fun onPhoneChange(input: String) {
        var digits = input.filter(Char::isDigit)
        // Accept pasted numbers that include the country code.
        if (digits.length > PHONE_LOCAL_LENGTH && digits.startsWith(COUNTRY_CODE_DIGITS)) {
            digits = digits.removePrefix(COUNTRY_CODE_DIGITS)
        }
        digits = digits.take(PHONE_LOCAL_LENGTH)
        _uiState.update { state ->
            if (state.phoneDigits == digits) state
            else state.copy(phoneDigits = digits, error = state.error.takeIfRateLimitFor(state, digits))
        }
    }

    fun onCodeChange(input: String) {
        _uiState.update { state ->
            val digits = input.filter(Char::isDigit).take(state.otpLength)
            // Keep "locked"/"expired" visible: typing can't fix those, only a new code can.
            state.copy(code = digits, error = state.error.takeIf { state.needsNewCode })
        }
    }

    /** "Get code" on the phone step. */
    fun requestCode() {
        if (!_uiState.value.canRequestCode) return
        sendOtp()
    }

    /** "Resend code" / "Request new code" on the OTP step. */
    fun resendCode() {
        val state = _uiState.value
        if (state.isLoading || state.isCooldownActive) return
        sendOtp()
    }

    fun verify() {
        val state = _uiState.value
        if (!state.canVerify) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = verifyOtp(state.fullPhone, state.code)) {
                is AuthOutcome.Success -> _uiState.update {
                    it.copy(isLoading = false, step = AuthStep.Success, isNewUser = outcome.value.isNewUser)
                }
                is AuthOutcome.Failure -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = outcome.error,
                        // Wrong code: clear the boxes so the user can retype right away.
                        code = if (outcome.error is AuthError.InvalidCode) "" else it.code
                    )
                }
            }
        }
    }

    fun backToPhoneEntry() {
        _uiState.update {
            it.copy(step = AuthStep.PhoneEntry, code = "", error = null, isLoading = false)
        }
    }

    /** Called once the UI has navigated away after a successful login. */
    fun onLoginHandled() {
        cooldownJob?.cancel()
        _uiState.value = AuthUiState()
    }

    /** Debug builds only: fill the fixed test number / code. */
    fun fillTestPhone() = onPhoneChange(AuthUiState.TEST_PHONE_DIGITS)

    fun fillTestCode() = onCodeChange(AuthUiState.TEST_OTP_CODE)

    private fun sendOtp() {
        val phoneDigits = _uiState.value.phoneDigits
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = requestOtp(AuthUiState.COUNTRY_PREFIX + phoneDigits)) {
                is AuthOutcome.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, step = AuthStep.OtpEntry, code = "", error = null)
                    }
                    startCooldown(phoneDigits, RESEND_COOLDOWN_SECONDS)
                }
                is AuthOutcome.Failure -> {
                    _uiState.update { it.copy(isLoading = false, error = outcome.error) }
                    val error = outcome.error
                    if (error is AuthError.RateLimited) {
                        startCooldown(phoneDigits, error.retryAfterSeconds ?: RATE_LIMIT_COOLDOWN_SECONDS)
                    }
                }
            }
        }
    }

    private fun startCooldown(phoneDigits: String, seconds: Int) {
        cooldownJob?.cancel()
        _uiState.update { it.copy(cooldownSeconds = seconds, cooldownPhoneDigits = phoneDigits) }
        cooldownJob = viewModelScope.launch {
            while (_uiState.value.cooldownSeconds > 0) {
                delay(1_000)
                _uiState.update { state ->
                    val remaining = state.cooldownSeconds - 1
                    state.copy(
                        cooldownSeconds = remaining,
                        // The rate-limit message is stale once the button is usable again.
                        error = if (remaining <= 0 && state.error is AuthError.RateLimited) null else state.error
                    )
                }
            }
        }
    }

    /** A rate-limit error stays visible only while the number it applies to is still entered. */
    private fun AuthError?.takeIfRateLimitFor(state: AuthUiState, newDigits: String): AuthError? =
        takeIf { it is AuthError.RateLimited && state.cooldownPhoneDigits == newDigits }

    private companion object {
        const val COUNTRY_CODE_DIGITS = "998"

        /**
         * The server allows 3 requests/minute per phone; spacing resends 30 s apart keeps a
         * user comfortably inside that without ever showing them a 429.
         */
        const val RESEND_COOLDOWN_SECONDS = 30

        /** Used after a 429 when the server sends no Retry-After header. */
        const val RATE_LIMIT_COOLDOWN_SECONDS = 60
    }
}
