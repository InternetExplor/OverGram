package com.example.overgram.data.local.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A message on this device: confirmed by the server (has [serverSeq]) or one of ours still in
 * the outbox ([sendState] SENDING / FAILED). Keyed by clientMessageId, so the server's echo or
 * ack of an outgoing message simply overwrites the pending row.
 */
@Entity(
    tableName = "messages",
    indices = [Index(value = ["chatId", "serverSeq"]), Index(value = ["serverId"]), Index(value = ["sendState"])]
)
data class MessageEntity(
    @PrimaryKey val clientMessageId: String,
    val chatId: String,
    val serverId: Long?,
    val serverSeq: Long?,
    val senderId: String,
    /** MessageType name. */
    val type: String,
    /** For SYSTEM messages this is the JSON event body. */
    val body: String?,
    val createdAt: Long,
    val isEdited: Boolean,
    val isDeleted: Boolean,
    /** SendState name. */
    val sendState: String,
    /** Why the server refused it, for FAILED messages. */
    val failureReason: String? = null,
    /** JSON list of [MediaJson]; null for messages without attachments. */
    val media: String? = null,
    /** Our own attachment's upload session, so an interrupted upload resumes instead of restarting. */
    val uploadId: String? = null
)

/** One attachment as stored in [MessageEntity.media]. */
data class MediaJson(
    val mediaId: String?,
    val kind: String,
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    /** Our copy of a file being sent; dropped once the upload is done. */
    val localPath: String? = null
)

/** One row of the chat list, as last returned by the server. */
@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    /** ChatType name. */
    val type: String,
    val title: String?,
    val peerUserId: String?,
    val lastMessageSenderId: String?,
    val lastMessageType: String?,
    val lastMessageBody: String?,
    val lastMessageCreatedAt: Long?,
    val lastMessageDeleted: Boolean,
    val lastActivityAt: Long,
    val unreadCount: Int,
    val isMuted: Boolean
)

/** Cached public profile (names for chats and senders, last known presence). */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val username: String?,
    val avatarMediaId: String?,
    val isOnline: Boolean,
    val lastSeenAt: Long?
)
