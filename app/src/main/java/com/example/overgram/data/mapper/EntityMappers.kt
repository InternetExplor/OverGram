package com.example.overgram.data.mapper

import com.example.overgram.data.local.db.ChatEntity
import com.example.overgram.data.local.db.MessageEntity
import com.example.overgram.data.local.db.UserEntity
import com.example.overgram.data.remote.dto.ChatDto
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.Message
import com.example.overgram.domain.model.MessagePreview
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SendState
import com.example.overgram.domain.model.StoredMessage
import com.example.overgram.domain.model.UserProfile
import com.google.gson.Gson

/* Domain/DTO ↔ Room entity mappers. */

fun Message.toEntity(chatId: String): MessageEntity = MessageEntity(
    clientMessageId = clientMessageId,
    chatId = chatId,
    serverId = serverId,
    serverSeq = serverSeq,
    senderId = senderId,
    type = type.name,
    body = body,
    createdAt = createdAt,
    isEdited = isEdited,
    isDeleted = isDeleted,
    sendState = SendState.SENT.name
)

fun MessageEntity.toDomain(gson: Gson): StoredMessage {
    val messageType = MessageType.entries.firstOrNull { it.name == type } ?: MessageType.UNKNOWN
    return StoredMessage(
        clientMessageId = clientMessageId,
        chatId = chatId,
        serverId = serverId,
        serverSeq = serverSeq,
        senderId = senderId,
        type = messageType,
        body = body,
        createdAt = createdAt,
        isEdited = isEdited,
        isDeleted = isDeleted,
        systemEvent = if (messageType == MessageType.SYSTEM) parseSystemEvent(gson, body) else null,
        sendState = SendState.entries.firstOrNull { it.name == sendState } ?: SendState.SENT,
        failureReason = failureReason
    )
}

fun ChatDto.toEntity(): ChatEntity? {
    val id = id ?: return null
    return toEntity(id)
}

private fun ChatDto.toEntity(id: String): ChatEntity = ChatEntity(
    id = id,
    type = if (type == ChatType.GROUP.name) ChatType.GROUP.name else ChatType.DIRECT.name,
    title = title,
    peerUserId = peerUserId,
    lastMessageSenderId = lastMessage?.senderId,
    lastMessageType = lastMessage?.type,
    lastMessageBody = lastMessage?.body,
    lastMessageCreatedAt = lastMessage?.createdAt,
    lastMessageDeleted = lastMessage?.deletedAt != null,
    lastActivityAt = lastActivityAt ?: 0L,
    unreadCount = unreadCount ?: 0,
    isMuted = muted ?: false
)

fun ChatEntity.toDomain(users: Map<String, UserProfile>, gson: Gson): ChatSummary {
    val chatType = ChatType.entries.firstOrNull { it.name == type } ?: ChatType.DIRECT
    val previewType = MessageType.entries.firstOrNull { it.name == lastMessageType } ?: MessageType.UNKNOWN
    val preview = if (lastMessageSenderId != null && lastMessageCreatedAt != null) {
        MessagePreview(
            senderId = lastMessageSenderId,
            type = previewType,
            body = lastMessageBody,
            createdAt = lastMessageCreatedAt,
            isDeleted = lastMessageDeleted,
            systemEvent = if (previewType == MessageType.SYSTEM) parseSystemEvent(gson, lastMessageBody) else null
        )
    } else {
        null
    }
    return ChatSummary(
        id = id,
        type = chatType,
        title = title,
        peer = peerUserId?.let(users::get),
        lastMessage = preview,
        lastMessageSender = lastMessageSenderId?.takeIf { chatType == ChatType.GROUP }?.let(users::get),
        lastActivityAt = lastActivityAt,
        unreadCount = unreadCount,
        isMuted = isMuted
    )
}

fun UserProfile.toEntity(): UserEntity = UserEntity(
    id = id,
    displayName = displayName,
    username = username,
    avatarMediaId = avatarMediaId,
    isOnline = isOnline,
    lastSeenAt = lastSeenAt
)

fun UserEntity.toDomain(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    username = username,
    avatarMediaId = avatarMediaId,
    isOnline = isOnline,
    lastSeenAt = lastSeenAt
)
