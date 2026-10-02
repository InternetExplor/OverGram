package com.example.overgram.data.remote

import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.remote.dto.ErrorDto
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runs authenticated Relay calls and turns transport/HTTP failures into [AuthError]s.
 * (The auth endpoints have their own mapping in AuthRepositoryImpl: there a 401 means a wrong code.)
 */
@Singleton
class ApiCaller @Inject constructor(
    private val tokenPreferences: TokenPreferences,
    private val gson: Gson
) {

    /** The body for a 2xx (null for bodiless 204s), or a typed failure. */
    suspend fun <T> call(block: suspend () -> Response<T>): AuthOutcome<T?> =
        try {
            val response = block()
            if (response.isSuccessful) {
                AuthOutcome.Success(response.body())
            } else {
                AuthOutcome.Failure(response.toError())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Request failed: network")
            AuthOutcome.Failure(AuthError.Network)
        } catch (e: Exception) {
            Timber.e(e, "Request failed")
            AuthOutcome.Failure(AuthError.Unknown(e.message))
        }

    private fun Response<*>.toError(): AuthError {
        val error = parseError()
        return when {
            // TokenAuthenticator already tried to refresh; no session left means it was revoked.
            code() == 401 && !tokenPreferences.hasSession() -> AuthError.SessionExpired
            error?.code == "RATE_LIMITED" || code() == 429 ->
                AuthError.RateLimited(headers()["Retry-After"]?.trim()?.toIntOrNull())
            code() in 500..599 -> AuthError.ServiceUnavailable
            // VALIDATION_ERROR, USERNAME_TAKEN, FORBIDDEN ("You must be an admin…"), NOT_FOUND:
            // the server's message is user-readable.
            code() in setOf(400, 403, 404, 409) -> AuthError.Validation(error?.message)
            else -> AuthError.Unknown(error?.message)
        }
    }

    private fun Response<*>.parseError(): ErrorDto? = try {
        errorBody()?.charStream()?.use { gson.fromJson(it, ErrorDto::class.java) }
    } catch (e: JsonParseException) {
        null
    } catch (e: IOException) {
        null
    }
}

/** Maps a success value; a null result means the body was malformed. */
inline fun <T, R : Any> AuthOutcome<T>.mapNotNull(transform: (T) -> R?): AuthOutcome<R> =
    when (this) {
        is AuthOutcome.Success -> transform(value)?.let { AuthOutcome.Success(it) }
            ?: AuthOutcome.Failure(AuthError.Unknown("Malformed response")).also {
                Timber.e("Malformed response")
            }
        is AuthOutcome.Failure -> this
    }
