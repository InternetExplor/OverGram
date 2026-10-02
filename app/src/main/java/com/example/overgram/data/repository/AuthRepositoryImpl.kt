package com.example.overgram.data.repository

import com.example.overgram.data.local.db.OverGramDatabase
import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.remote.api.AuthApi
import com.example.overgram.data.remote.dto.ErrorDto
import com.example.overgram.data.remote.dto.OtpRequestDto
import com.example.overgram.data.remote.dto.OtpVerifyRequestDto
import com.example.overgram.data.remote.dto.RefreshRequestDto
import com.example.overgram.data.remote.dto.TokenPairDto
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.AuthResult
import com.example.overgram.domain.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val tokenPreferences: TokenPreferences,
    private val gson: Gson,
    private val database: OverGramDatabase
) : AuthRepository {

    override suspend fun requestOtp(phone: String): AuthOutcome<Unit> =
        call { api.requestOtp(OtpRequestDto(phone)) }.map { }

    override suspend fun verifyOtp(
        phone: String,
        code: String,
        deviceName: String
    ): AuthOutcome<AuthResult> {
        val outcome = call { api.verifyOtp(OtpVerifyRequestDto(phone, code, deviceName)) }
            .map { it.toDomain() }
            .alsoOnSuccess(::persist)
        // A fresh session may be another account: never show it the previous one's chats.
        if (outcome is AuthOutcome.Success) clearLocalData()
        return outcome
    }

    override suspend fun refresh(refreshToken: String): AuthOutcome<AuthResult> {
        val outcome = call { api.refresh(RefreshRequestDto(refreshToken)) }
            .map { it.toDomain() }
            .alsoOnSuccess(::persist)

        if (outcome is AuthOutcome.Failure &&
            (outcome.error == AuthError.SessionRevoked || outcome.error == AuthError.SessionExpired)
        ) {
            tokenPreferences.clear()
        }
        return outcome
    }

    override suspend fun logout(): AuthOutcome<Unit> {
        // Local logout must always happen, even if the server call fails.
        val outcome = call { api.logout() }.map { }
        tokenPreferences.clear()
        clearLocalData()
        return outcome
    }

    /** Stored chats, messages and the outbox belong to the session that's ending. */
    private suspend fun clearLocalData() {
        try {
            withContext(Dispatchers.IO) { database.clearAllTables() }
        } catch (e: Exception) {
            Timber.e(e, "Couldn't clear local data")
        }
    }

    override fun hasSession(): Boolean = tokenPreferences.hasSession()

    private fun persist(result: AuthResult) {
        tokenPreferences.save(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            userId = result.userId,
            deviceId = result.deviceId
        )
    }

    /**
     * Runs a Retrofit call and converts transport/HTTP failures to [AuthError].
     * Returns the body for 2xx (null for bodiless 204s).
     */
    private suspend fun <T> call(block: suspend () -> Response<T>): AuthOutcome<T?> =
        try {
            val response = block()
            if (response.isSuccessful) {
                AuthOutcome.Success(response.body())
            } else {
                AuthOutcome.Failure(response.toAuthError())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Auth request failed: network")
            AuthOutcome.Failure(AuthError.Network)
        } catch (e: Exception) {
            Timber.e(e, "Auth request failed")
            AuthOutcome.Failure(AuthError.Unknown(e.message))
        }

    private fun Response<*>.toAuthError(): AuthError {
        val error = parseError()
        return when (error?.code) {
            "INVALID_OTP" -> AuthError.InvalidCode(error?.remainingAttempts)
            "OTP_EXPIRED" -> AuthError.CodeExpired
            "OTP_LOCKED" -> AuthError.OtpLocked
            "RATE_LIMITED" -> AuthError.RateLimited(retryAfterSeconds())
            "TELEGRAM_NOT_LINKED" -> AuthError.TelegramNotLinked(error?.botUrl)
            "VALIDATION_ERROR" -> AuthError.Validation(error?.message)
            "TOKEN_REUSED" -> AuthError.SessionRevoked
            "TOKEN_EXPIRED" -> AuthError.SessionExpired
            "OTP_DELIVERY_UNAVAILABLE", "SERVER_ERROR" -> AuthError.ServiceUnavailable
            else -> when (code()) {
                400 -> AuthError.Validation(error?.message)
                401 -> if (raw().request.url.encodedPath.endsWith("/auth/refresh")) {
                    AuthError.SessionExpired
                } else {
                    AuthError.InvalidCode()
                }
                429 -> AuthError.RateLimited(retryAfterSeconds())
                in 500..599 -> AuthError.ServiceUnavailable
                else -> AuthError.Unknown(error?.message)
            }
        }
    }

    private fun Response<*>.parseError(): ErrorDto? = try {
        errorBody()?.charStream()?.use { gson.fromJson(it, ErrorDto::class.java) }
    } catch (e: JsonParseException) {
        null
    } catch (e: IOException) {
        null
    }

    private fun Response<*>.retryAfterSeconds(): Int? =
        headers()["Retry-After"]?.trim()?.toIntOrNull()?.takeIf { it > 0 }

    private fun TokenPairDto?.toDomain(): AuthResult {
        requireNotNull(this) { "Empty token response" }
        return AuthResult(
            accessToken = requireNotNull(accessToken) { "accessToken missing" },
            refreshToken = requireNotNull(refreshToken) { "refreshToken missing" },
            userId = requireNotNull(userId) { "userId missing" },
            deviceId = requireNotNull(deviceId) { "deviceId missing" },
            isNewUser = isNewUser ?: false
        )
    }

    private inline fun <T, R> AuthOutcome<T>.map(transform: (T) -> R): AuthOutcome<R> =
        when (this) {
            is AuthOutcome.Success -> try {
                AuthOutcome.Success(transform(value))
            } catch (e: IllegalArgumentException) {
                Timber.e(e, "Malformed auth response")
                AuthOutcome.Failure(AuthError.Unknown(e.message))
            }
            is AuthOutcome.Failure -> this
        }

    private inline fun <T> AuthOutcome<T>.alsoOnSuccess(action: (T) -> Unit): AuthOutcome<T> {
        if (this is AuthOutcome.Success) action(value)
        return this
    }
}
