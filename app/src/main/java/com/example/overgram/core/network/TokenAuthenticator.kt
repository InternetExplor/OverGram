package com.example.overgram.core.network

import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * On a 401 for a bearer-authenticated request, rotates the refresh token once and retries
 * with the new access token. Concurrent 401s share a single refresh: a request whose token
 * was already replaced just retries with the current one.
 *
 * [AuthRepository] is behind a [Provider] because it depends on the same OkHttp client.
 * If the refresh is rejected, the repository clears the stored session.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenPreferences: TokenPreferences,
    private val authRepository: Provider<AuthRepository>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Public endpoints (incl. /auth/refresh itself) carry no bearer token: nothing to refresh.
        val failedToken = response.request.header(AUTHORIZATION)?.removePrefix(BEARER) ?: return null
        // Already retried once with a fresh token: don't loop.
        if (response.priorResponse != null) return null

        synchronized(this) {
            val current = tokenPreferences.getAccessToken()
            if (!current.isNullOrEmpty() && current != failedToken) {
                return response.request.withToken(current)
            }

            val refreshToken = tokenPreferences.getRefreshToken() ?: return null
            val outcome = runBlocking { authRepository.get().refresh(refreshToken) }
            return (outcome as? AuthOutcome.Success)?.let {
                response.request.withToken(it.value.accessToken)
            }
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AUTHORIZATION, BEARER + token).build()

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer "
    }
}
