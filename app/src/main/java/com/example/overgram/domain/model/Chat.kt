package com.example.overgram.domain.model

enum class ChatType { DIRECT, GROUP }

enum class MessageType { TEXT, IMAGE, VIDEO, FILE, SYSTEM, UNKNOWN }

data class MessagePreview(
    val senderId: String,
    val type: MessageType,
    val body: String?,
    val createdAt: Long,
    val isDeleted: Boolean
)

/** A user's public profile. [lastSeenAt] is null while online, or if never seen. */
data class UserProfile(
    val id: String,
    val displayName: String,
    val username: String?,
    val avatarMediaId: String?,
    val isOnline: Boolean,
    val lastSeenAt: Long?
)

/**
 * One row of the chat list. For DIRECT chats [peer] is the other user's profile
 * (null if it couldn't be loaded); for GROUP chats it is always null.
 */
data class ChatSummary(
    val id: String,
    val type: ChatType,
    val title: String?,
    val peer: UserProfile?,
    val lastMessage: MessagePreview?,
    val lastActivityAt: Long,
    val unreadCount: Int,
    val isMuted: Boolean
)
