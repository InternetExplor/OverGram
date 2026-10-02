package com.example.overgram.core.network

import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * The single place that rotates the refresh token. HTTP (TokenAuthenticator) and the WebSocket
 * both come through here: refresh tokens are single-use, so two parallel refreshes would make
 * the server see a reused token and revoke the whole device session.
 *
 * [AuthRepository] is behind a [Provider] because it depends on the same OkHttp client.
 */
@Singleton
class TokenRefresher @Inject constructor(
    private val tokenPreferences: TokenPreferences,
    private val authRepository: Provider<AuthRepository>
) {

    private val lock = Any()

    /**
     * Returns a usable access token after [staleToken] was rejected, or null if the session is over
     * (the repository then clears it). If another caller already replaced [staleToken], that newer
     * token is returned without refreshing again. Blocking: call it off the main thread.
     */
    fun refresh(staleToken: String?): String? = synchronized(lock) {
        val current = tokenPreferences.getAccessToken()
        if (!current.isNullOrEmpty() && current != staleToken) return current

        val refreshToken = tokenPreferences.getRefreshToken() ?: return null
        val outcome = runBlocking { authRepository.get().refresh(refreshToken) }
        (outcome as? AuthOutcome.Success)?.value?.accessToken
    }
}
