package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.ChatListPageDto
import com.example.overgram.data.remote.dto.MessagePageDto
import com.example.overgram.data.remote.dto.SendMessageRequestDto
import com.example.overgram.data.remote.dto.SendMessageResultDto
import com.example.overgram.data.remote.dto.SeqCursorDto
import com.example.overgram.data.remote.dto.UserPublicDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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

    /** Newest first; [beforeSeq] pages backwards (exclusive). */
    @GET("v1/chats/{id}/messages")
    suspend fun listMessages(
        @Path("id") chatId: String,
        @Query("beforeSeq") beforeSeq: Long?,
        @Query("limit") limit: Int
    ): Response<MessagePageDto>

    /** Idempotent on `clientMessageId`: 201 for a new message, 200 for a replay. */
    @POST("v1/chats/{id}/messages")
    suspend fun sendMessage(
        @Path("id") chatId: String,
        @Body body: SendMessageRequestDto
    ): Response<SendMessageResultDto>

    /** Max-wins and clamped server-side, so it is always safe to send. */
    @POST("v1/chats/{id}/read")
    suspend fun markRead(
        @Path("id") chatId: String,
        @Body body: SeqCursorDto
    ): Response<Unit>
}
