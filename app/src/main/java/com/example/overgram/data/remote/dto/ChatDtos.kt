package com.example.overgram.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Fields are nullable because Gson ignores Kotlin nullability; the repository validates them.
 */
data class ChatListPageDto(
    @SerializedName("chats") val chats: List<ChatDto>?,
    @SerializedName("nextCursor") val nextCursor: String?
)

data class ChatDto(
    @SerializedName("id") val id: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("avatarMediaId") val avatarMediaId: String?,
    @SerializedName("peerUserId") val peerUserId: String?,
    @SerializedName("lastMessage") val lastMessage: MessagePreviewDto?,
    @SerializedName("lastActivityAt") val lastActivityAt: Long?,
    @SerializedName("unreadCount") val unreadCount: Int?,
    @SerializedName("muted") val muted: Boolean?
)

data class MessagePreviewDto(
    @SerializedName("serverId") val serverId: Long?,
    @SerializedName("senderId") val senderId: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("createdAt") val createdAt: Long?,
    @SerializedName("deletedAt") val deletedAt: Long?
)

/** Relay's `UserPublic`: a profile without the phone number. */
data class UserPublicDto(
    @SerializedName("id") val id: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("displayName") val displayName: String?,
    @SerializedName("avatarMediaId") val avatarMediaId: String?,
    @SerializedName("online") val online: Boolean?,
    @SerializedName("lastSeenAt") val lastSeenAt: Long?
)
