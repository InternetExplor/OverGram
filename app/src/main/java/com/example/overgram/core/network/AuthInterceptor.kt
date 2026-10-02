package com.example.overgram.core.network

import com.example.overgram.data.local.prefs.TokenPreferences
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds `Authorization: Bearer <accessToken>` to every request unless the endpoint opts out
 * with [NO_AUTH_HEADER] (public auth endpoints). The marker header is stripped before sending.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenPreferences: TokenPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.header(NO_AUTH_HEADER_NAME) != null) {
            return chain.proceed(request.newBuilder().removeHeader(NO_AUTH_HEADER_NAME).build())
        }

        val token = tokenPreferences.getAccessToken()
        if (token.isNullOrEmpty() || request.header(AUTHORIZATION) != null) {
            return chain.proceed(request)
        }

        return chain.proceed(
            request.newBuilder()
                .header(AUTHORIZATION, "Bearer $token")
                .build()
        )
    }

    companion object {
        private const val AUTHORIZATION = "Authorization"
        private const val NO_AUTH_HEADER_NAME = "X-No-Auth"

        /** Use as `@Headers(AuthInterceptor.NO_AUTH_HEADER)` on unauthenticated endpoints. */
        const val NO_AUTH_HEADER = "$NO_AUTH_HEADER_NAME: true"
    }
}
