package com.example.overgram.domain.model

enum class ChatType { DIRECT, GROUP }

enum class MessageType { TEXT, IMAGE, VIDEO, FILE, SYSTEM, UNKNOWN }

enum class MemberRole { OWNER, ADMIN, MEMBER }

/**
 * Parsed body of a SYSTEM message: structured data, rendered into text by the client.
 * [kind] is [SystemEventKind.UNKNOWN] for events this client doesn't know yet.
 */
data class SystemEvent(
    val kind: SystemEventKind,
    val actorId: String,
    val targetUserIds: List<String>,
    /** Only for [SystemEventKind.GROUP_CREATED]. */
    val title: String?
)

enum class SystemEventKind {
    GROUP_CREATED, MEMBERS_ADDED, MEMBER_REMOVED, MEMBER_LEFT, OWNER_CHANGED, ROLE_CHANGED, UNKNOWN
}

data class MessagePreview(
    val senderId: String,
    val type: MessageType,
    val body: String?,
    val createdAt: Long,
    val isDeleted: Boolean,
    /** Set for SYSTEM messages whose body could be parsed. */
    val systemEvent: SystemEvent? = null
)

/**
 * A message confirmed by the server. [clientMessageId] is the stable identity across retries
 * and devices; [serverSeq] is the per-chat order.
 */
data class Message(
    val clientMessageId: String,
    val serverId: Long,
    val serverSeq: Long,
    val senderId: String,
    val type: MessageType,
    val body: String?,
    val createdAt: Long,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    /** Set for SYSTEM messages whose body could be parsed. */
    val systemEvent: SystemEvent? = null
)

/** One page of history, newest first. */
data class MessagePage(
    val messages: List<Message>,
    val hasMore: Boolean
)

/** What the server assigned to a sent message. */
data class SentMessage(
    val serverId: Long,
    val serverSeq: Long,
    val createdAt: Long
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

/** A group member as returned when members are added. */
data class ChatMember(
    val userId: String,
    val role: MemberRole,
    val isOnline: Boolean
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
    /** GROUP chats: who sent [lastMessage], for "Ada: hi" previews. */
    val lastMessageSender: UserProfile? = null,
    val lastActivityAt: Long,
    val unreadCount: Int,
    val isMuted: Boolean
)
