package com.example.overgram.domain.model

/**
 * Domain-level auth failures. Mapped from Relay's `{ code, message, retryable }` error bodies
 * in the data layer so presentation never sees HTTP or Retrofit types.
 */
sealed interface AuthError {
    /** 401 INVALID_OTP. [remainingAttempts] is only set if the server reports it. */
    data class InvalidCode(val remainingAttempts: Int? = null) : AuthError

    /** 401 OTP_EXPIRED — a new code must be requested. */
    data object CodeExpired : AuthError

    /** 429 OTP_LOCKED — 5 wrong guesses; the current code is dead, a new one must be requested. */
    data object OtpLocked : AuthError

    /** 429 RATE_LIMITED. [retryAfterSeconds] comes from the Retry-After header when present. */
    data class RateLimited(val retryAfterSeconds: Int? = null) : AuthError

    /** 409 TELEGRAM_NOT_LINKED — non-test numbers must link Telegram via [botUrl] first. */
    data class TelegramNotLinked(val botUrl: String?) : AuthError

    /** 400 VALIDATION_ERROR. */
    data class Validation(val message: String?) : AuthError

    /** 401 TOKEN_REUSED — the whole device session was revoked; force a full re-login. */
    data object SessionRevoked : AuthError

    /** 401 TOKEN_EXPIRED / UNAUTHORIZED on refresh — the user has to log in again. */
    data object SessionExpired : AuthError

    /** 503 OTP_DELIVERY_UNAVAILABLE / SERVER_ERROR — not the user's fault, retry later. */
    data object ServiceUnavailable : AuthError

    /** No connectivity, timeout, DNS failure… */
    data object Network : AuthError

    data class Unknown(val message: String? = null) : AuthError
}
