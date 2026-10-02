package com.example.overgram.data.remote.api

import com.example.overgram.core.network.AuthInterceptor
import com.example.overgram.data.remote.dto.AddMembersRequestDto
import com.example.overgram.data.remote.dto.ChatDto
import com.example.overgram.data.remote.dto.ChatListPageDto
import com.example.overgram.data.remote.dto.ChatMembersDto
import com.example.overgram.data.remote.dto.ChatSettingsRequestDto
import com.example.overgram.data.remote.dto.CreateGroupRequestDto
import com.example.overgram.data.remote.dto.DirectChatRequestDto
import com.example.overgram.data.remote.dto.MessagePageDto
import com.example.overgram.data.remote.dto.SendMessageRequestDto
import com.example.overgram.data.remote.dto.SendMessageResultDto
import com.example.overgram.data.remote.dto.SeqCursorDto
import com.example.overgram.data.remote.dto.UpdateChatRequestDto
import com.example.overgram.data.remote.dto.UserPublicDto
import com.example.overgram.data.remote.dto.UserSearchResultDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
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

    /** Case-insensitive prefix match on username only. */
    @GET("v1/users/search")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("limit") limit: Int
    ): Response<UserSearchResultDto>

    /** Returns the existing DIRECT chat with this peer (200) or creates one (201). */
    @POST("v1/chats/direct")
    suspend fun getOrCreateDirectChat(@Body body: DirectChatRequestDto): Response<ChatDto>

    /** Caller must be an active member (403 otherwise). */
    @GET("v1/chats/{id}")
    suspend fun getChat(@Path("id") chatId: String): Response<ChatDto>

    /** Always creates a new group; the caller becomes OWNER. */
    @POST("v1/chats/group")
    suspend fun createGroup(@Body body: CreateGroupRequestDto): Response<ChatDto>

    /** ADMIN/OWNER only. */
    @PATCH("v1/chats/{id}")
    suspend fun updateChat(
        @Path("id") chatId: String,
        @Body body: UpdateChatRequestDto
    ): Response<ChatDto>

    /** ADMIN/OWNER only. Returns the updated member list. */
    @POST("v1/chats/{id}/members")
    suspend fun addMembers(
        @Path("id") chatId: String,
        @Body body: AddMembersRequestDto
    ): Response<ChatMembersDto>

    /** OWNER/ADMIN can remove any MEMBER. 204 on success. */
    @DELETE("v1/chats/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") chatId: String,
        @Path("userId") userId: String
    ): Response<Unit>

    /** If the OWNER leaves, ownership passes on automatically. */
    @POST("v1/chats/{id}/leave")
    suspend fun leaveChat(@Path("id") chatId: String): Response<Unit>

    /** The caller's own mute setting; invisible to other members. */
    @PUT("v1/chats/{id}/settings")
    suspend fun updateSettings(
        @Path("id") chatId: String,
        @Body body: ChatSettingsRequestDto
    ): Response<ChatDto>

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
