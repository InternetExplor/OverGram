package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.AuthResult

interface AuthRepository {

    /** Asks the server to generate and deliver an OTP for [phone] (E.164, e.g. "+998901234567"). */
    suspend fun requestOtp(phone: String): AuthOutcome<Unit>

    /** Exchanges the OTP for a token pair. On success the session is persisted locally. */
    suspend fun verifyOtp(phone: String, code: String, deviceName: String): AuthOutcome<AuthResult>

    /** Rotates the refresh token. On TOKEN_REUSED / expiry the local session is cleared. */
    suspend fun refresh(refreshToken: String): AuthOutcome<AuthResult>

    /** Revokes this device's session on the server (best effort) and always clears it locally. */
    suspend fun logout(): AuthOutcome<Unit>

    /** Whether a session (refresh token) is stored locally. */
    fun hasSession(): Boolean
}
