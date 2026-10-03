package com.example.overgram.domain.model

enum class ChatType { DIRECT, GROUP }

enum class MessageType { TEXT, IMAGE, VIDEO, FILE, SYSTEM, UNKNOWN }

enum class MemberRole { OWNER, ADMIN, MEMBER }

enum class MediaKind { IMAGE, VIDEO, FILE }

/**
 * A photo, video or file attached to a message. Relay keeps no file names, so for FILE messages
 * the message body carries the name (see [StoredMessage.body]).
 *
 * @param mediaId Null until our own upload has started.
 * @param localPath Our own copy of a file we're sending, until the upload is done.
 */
data class MediaAttachment(
    val mediaId: String?,
    val kind: MediaKind,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val localPath: String? = null
)

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
    val systemEvent: SystemEvent? = null,
    val media: List<MediaAttachment> = emptyList(),
    /** clientMessageId of the message this one replies to. */
    val replyToId: String? = null
)

/** Delivery state of a message stored on this device. */
enum class SendState {
    /** Confirmed by the server. */
    SENT,
    /** Ours, queued: goes out as soon as there's a connection (survives app restarts). */
    SENDING,
    /** Ours, refused by the server (e.g. no longer a member). Can be retried by hand. */
    FAILED
}

/**
 * A message as stored on this device: server history plus our own outgoing messages that
 * aren't confirmed yet ([serverSeq] null).
 */
data class StoredMessage(
    val clientMessageId: String,
    val chatId: String,
    val serverId: Long?,
    val serverSeq: Long?,
    val senderId: String,
    val type: MessageType,
    /** Text, a media caption, or — for FILE messages — the file name. */
    val body: String?,
    val createdAt: Long,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val systemEvent: SystemEvent?,
    val sendState: SendState,
    val media: List<MediaAttachment> = emptyList(),
    /** Server's reason for a [SendState.FAILED] message. */
    val failureReason: String? = null,
    /** clientMessageId of the message this one replies to. */
    val replyToId: String? = null
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

/**
 * A group's current members, rebuilt from its history (Relay has no member-list endpoint).
 * [isComplete] is false when the history was too long to read back to the group's creation;
 * the list then only has people seen in the part that was read.
 */
data class GroupMembers(
    val members: List<UserProfile>,
    /** Member ids whose profile couldn't be loaded. */
    val unknownMemberIds: List<String>,
    val ownerId: String?,
    val isComplete: Boolean
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

/** Outcome of picking an attachment to send. */
sealed interface MediaSendResult {
    data class Queued(val clientMessageId: String) : MediaSendResult
    /** Over the server's 100 MB limit. */
    data object TooLarge : MediaSendResult
    data object Unreadable : MediaSendResult
}

/**
 * How far the other members of a chat got with our messages: [deliveredUpTo] (some device has
 * them) and [readUpTo] (someone read them). serverSeq values; reading implies delivery.
 */
data class ChatReceipts(val deliveredUpTo: Long = 0L, val readUpTo: Long = 0L)
