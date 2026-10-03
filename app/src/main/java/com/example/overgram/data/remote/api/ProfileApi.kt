package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.ServerInfoDto
import com.example.overgram.data.remote.dto.UserMeDto
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH

/** The caller's own profile and server info. Bearer token via [AuthInterceptor]. */
interface ProfileApi {

    @GET("v1/users/me")
    suspend fun getMe(): Response<UserMeDto>

    /**
     * Partial update: only the fields present are changed. A JsonObject body so that
     * `avatarMediaId: null` (remove the avatar) can be sent explicitly.
     */
    @PATCH("v1/users/me")
    suspend fun updateMe(@Body body: JsonObject): Response<UserMeDto>

    @Headers(AuthInterceptor.NO_AUTH_HEADER)
    @GET("v1/server/info")
    suspend fun getServerInfo(): Response<ServerInfoDto>
}
