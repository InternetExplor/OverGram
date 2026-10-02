package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.ChatListPageDto
import com.example.overgram.data.remote.dto.UserPublicDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/** Bearer token is attached by [AuthInterceptor]. */
interface ChatApi {

    /** Ordered by last activity, most recent first. */
    @GET("v1/chats")
    suspend fun listChats(
        @Query("limit") limit: Int,
        @Query("cursor") cursor: String? = null
    ): Response<ChatListPageDto>

    /** Public profile, including `online` / `lastSeenAt`. */
    @GET("v1/users/{id}")
    suspend fun getUser(@Path("id") userId: String): Response<UserPublicDto>
}
