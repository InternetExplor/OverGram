package com.example.overgram.core.network

import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Singleton

/**
 * On a 401 for a bearer-authenticated request, gets a fresh access token from [TokenRefresher]
 * (which rotates the refresh token at most once for concurrent failures) and retries once.
 * If the refresh is rejected, the stored session is cleared and the 401 goes through.
 */
@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenRefresher: TokenRefresher
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Public endpoints (incl. /auth/refresh itself) carry no bearer token: nothing to refresh.
        val failedToken = response.request.header(AUTHORIZATION)?.removePrefix(BEARER) ?: return null
        // Already retried once with a fresh token: don't loop.
        if (response.priorResponse != null) return null

        val token = tokenRefresher.refresh(staleToken = failedToken) ?: return null
        return response.request.newBuilder().header(AUTHORIZATION, BEARER + token).build()
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer "
    }
}
