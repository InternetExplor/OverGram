package com.example.overgram.presentation.auth

import com.example.overgram.domain.model.AuthError

enum class AuthStep { PhoneEntry, OtpEntry, Success }

/**
 * Single state for the whole auth flow.
 *
 * @param phoneDigits 9-digit local part of an Uzbek number, without the +998 prefix.
 * @param cooldownSeconds Seconds until an OTP may be requested again for [cooldownPhoneDigits].
 */
data class AuthUiState(
    val step: AuthStep = AuthStep.PhoneEntry,
    val phoneDigits: String = "",
    val code: String = "",
    val isLoading: Boolean = false,
    val error: AuthError? = null,
    val cooldownSeconds: Int = 0,
    val cooldownPhoneDigits: String? = null,
    val isNewUser: Boolean = false
) {
    val fullPhone: String get() = COUNTRY_PREFIX + phoneDigits

    val isPhoneValid: Boolean get() = phoneDigits.length == PHONE_LOCAL_LENGTH

    /** Cooldown only applies to the number it was started for. */
    val isCooldownActive: Boolean
        get() = cooldownSeconds > 0 && cooldownPhoneDigits == phoneDigits

    val canRequestCode: Boolean get() = isPhoneValid && !isLoading && !isCooldownActive

    /** The current code can no longer be verified; the user has to request a fresh one. */
    val needsNewCode: Boolean
        get() = error is AuthError.OtpLocked || error is AuthError.CodeExpired

    val canVerify: Boolean
        get() = code.length == otpLength && !isLoading && !needsNewCode

    /** +998900000000..+998900000999 always accept [TEST_OTP_CODE]. */
    val isTestNumber: Boolean
        get() = isPhoneValid && phoneDigits.startsWith(TEST_NUMBER_PREFIX)

    /** Test numbers use the fixed 5-digit code; codes delivered via Telegram have 6 digits. */
    val otpLength: Int
        get() = if (isTestNumber) TEST_OTP_LENGTH else OTP_LENGTH

    companion object {
        const val COUNTRY_PREFIX = "+998"
        const val PHONE_LOCAL_LENGTH = 9
        const val OTP_LENGTH = 6
        const val TEST_OTP_LENGTH = 5

        const val TEST_NUMBER_PREFIX = "900000"
        const val TEST_PHONE_DIGITS = "900000001"
        const val TEST_OTP_CODE = "11111"
    }
}
