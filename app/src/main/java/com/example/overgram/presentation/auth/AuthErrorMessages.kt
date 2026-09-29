package com.example.overgram.presentation.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.overgram.R
import com.example.overgram.domain.model.AuthError

/**
 * User-facing text for an [AuthError]. [cooldownSeconds] keeps the rate-limit message live.
 */
@Composable
fun authErrorMessage(error: AuthError, cooldownSeconds: Int = 0): String = when (error) {
    is AuthError.InvalidCode -> error.remainingAttempts?.let {
        pluralStringResource(R.plurals.auth_error_invalid_code_remaining, it, it)
    } ?: stringResource(R.string.auth_error_invalid_code)
    AuthError.CodeExpired -> stringResource(R.string.auth_error_code_expired)
    AuthError.OtpLocked -> stringResource(R.string.auth_error_otp_locked)
    is AuthError.RateLimited -> if (cooldownSeconds > 0) {
        stringResource(R.string.auth_error_rate_limited, cooldownSeconds)
    } else {
        stringResource(R.string.auth_error_rate_limited_later)
    }
    is AuthError.TelegramNotLinked -> stringResource(R.string.auth_error_telegram_not_linked)
    is AuthError.Validation -> error.message ?: stringResource(R.string.auth_error_validation)
    AuthError.SessionRevoked, AuthError.SessionExpired -> stringResource(R.string.auth_error_session_ended)
    AuthError.ServiceUnavailable -> stringResource(R.string.auth_error_service_unavailable)
    AuthError.Network -> stringResource(R.string.auth_error_network)
    is AuthError.Unknown -> stringResource(R.string.auth_error_unknown)
}
