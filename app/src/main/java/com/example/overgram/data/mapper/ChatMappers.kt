package com.example.overgram.data.mapper

import com.example.overgram.data.remote.dto.MessageDto
import com.example.overgram.data.remote.dto.MediaMetaDto
import com.example.overgram.data.remote.dto.MessagePreviewDto
import com.example.overgram.data.remote.dto.SystemBodyDto
import com.example.overgram.data.remote.dto.UserPublicDto
import com.example.overgram.domain.model.MediaAttachment
import com.example.overgram.domain.model.MediaKind
import com.example.overgram.domain.model.Message
import com.example.overgram.domain.model.MessagePreview
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SystemEvent
import com.example.overgram.domain.model.SystemEventKind
import com.example.overgram.domain.model.UserProfile
import com.google.gson.Gson
import com.google.gson.JsonParseException
import timber.log.Timber

/*
 * DTO → domain mappers shared by the REST repository and the realtime client
 * (live `message_new` updates carry the same Message payload as history pages).
 * DTO fields are nullable because Gson ignores Kotlin nullability; incomplete rows map to null.
 */

fun MessagePreviewDto.toDomain(gson: Gson): MessagePreview? {
    val messageType = MessageType.entries.firstOrNull { it.name == type } ?: MessageType.UNKNOWN
    return MessagePreview(
        senderId = senderId ?: return null,
        type = messageType,
        body = body,
        createdAt = createdAt ?: return null,
        isDeleted = deletedAt != null,
        systemEvent = if (messageType == MessageType.SYSTEM) parseSystemEvent(gson, body) else null
    )
}

fun MediaMetaDto.toDomain(): MediaAttachment? {
    val id = mediaId ?: return null
    return MediaAttachment(
        mediaId = id,
        kind = MediaKind.entries.firstOrNull { it.name == kind } ?: MediaKind.FILE,
        mimeType = mimeType ?: "application/octet-stream",
        sizeBytes = sizeBytes ?: 0L,
        width = width,
        height = height,
        durationMs = durationMs
    )
}

fun MessageDto.toDomain(gson: Gson): Message? {
    val messageType = MessageType.entries.firstOrNull { it.name == type } ?: MessageType.UNKNOWN
    return Message(
        clientMessageId = clientMessageId ?: return null,
        serverId = serverId ?: return null,
        serverSeq = serverSeq ?: return null,
        senderId = senderId ?: return null,
        type = messageType,
        body = body,
        createdAt = createdAt ?: return null,
        isEdited = editedAt != null,
        isDeleted = deletedAt != null,
        systemEvent = if (messageType == MessageType.SYSTEM) parseSystemEvent(gson, body) else null,
        media = media.orEmpty().mapNotNull { it.toDomain() },
        replyToId = replyToClientMessageId
    )
}

/** SYSTEM bodies are a JSON *string*; anything unparseable is simply not rendered as an event. */
fun parseSystemEvent(gson: Gson, body: String?): SystemEvent? {
    val dto = try {
        body?.let { gson.fromJson(it, SystemBodyDto::class.java) }
    } catch (e: JsonParseException) {
        Timber.w(e, "Unparseable SYSTEM body")
        null
    } ?: return null
    return SystemEvent(
        kind = SystemEventKind.entries.firstOrNull { it.name.equals(dto.event, ignoreCase = true) }
            ?: SystemEventKind.UNKNOWN,
        actorId = dto.actorId ?: return null,
        targetUserIds = dto.targetUserIds.orEmpty(),
        title = dto.title
    )
}

fun UserPublicDto.toDomain(): UserProfile? {
    return UserProfile(
        id = id ?: return null,
        displayName = displayName ?: username ?: return null,
        username = username,
        avatarMediaId = avatarMediaId,
        isOnline = online ?: false,
        lastSeenAt = lastSeenAt
    )
}
