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

data class MessagePageDto(
    @SerializedName("messages") val messages: List<MessageDto>?,
    @SerializedName("hasMore") val hasMore: Boolean?
)

data class MessageDto(
    @SerializedName("clientMessageId") val clientMessageId: String?,
    @SerializedName("serverId") val serverId: Long?,
    @SerializedName("chatId") val chatId: String?,
    @SerializedName("senderId") val senderId: String?,
    @SerializedName("serverSeq") val serverSeq: Long?,
    @SerializedName("type") val type: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("createdAt") val createdAt: Long?,
    @SerializedName("editedAt") val editedAt: Long?,
    @SerializedName("deletedAt") val deletedAt: Long?
)

data class SendMessageRequestDto(
    @SerializedName("clientMessageId") val clientMessageId: String,
    @SerializedName("type") val type: String,
    @SerializedName("body") val body: String?
)

data class SendMessageResultDto(
    @SerializedName("clientMessageId") val clientMessageId: String?,
    @SerializedName("serverId") val serverId: Long?,
    @SerializedName("serverSeq") val serverSeq: Long?,
    @SerializedName("serverCreatedAt") val serverCreatedAt: Long?
)

/** Body of `POST /v1/chats/{id}/read`. */
data class SeqCursorDto(
    @SerializedName("upToSeq") val upToSeq: Long
)

/** The `body` of a SYSTEM message is a JSON string of this shape. */
data class SystemBodyDto(
    @SerializedName("event") val event: String?,
    @SerializedName("actorId") val actorId: String?,
    @SerializedName("targetUserIds") val targetUserIds: List<String>?,
    @SerializedName("title") val title: String?
)

data class CreateGroupRequestDto(
    @SerializedName("title") val title: String,
    @SerializedName("memberIds") val memberIds: List<String>
)

data class UpdateChatRequestDto(
    @SerializedName("title") val title: String
)

/** `mutedUntil` null (with muted = true) means muted indefinitely. */
data class ChatSettingsRequestDto(
    @SerializedName("muted") val muted: Boolean,
    @SerializedName("mutedUntil") val mutedUntil: Long? = null
)

data class AddMembersRequestDto(
    @SerializedName("userIds") val userIds: List<String>
)

data class ChatMembersDto(
    @SerializedName("members") val members: List<ChatMemberDto>?
)

data class ChatMemberDto(
    @SerializedName("userId") val userId: String?,
    @SerializedName("role") val role: String?,
    @SerializedName("online") val online: Boolean?
)

data class UserSearchResultDto(
    @SerializedName("users") val users: List<UserPublicDto>?
)

data class DirectChatRequestDto(
    @SerializedName("peerUserId") val peerUserId: String
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
