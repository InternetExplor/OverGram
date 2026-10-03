package com.example.overgram.data.remote.dto

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// ---- REST sync (GET /v1/updates, /v1/updates/state) ----

data class UpdatesPageDto(
    @SerializedName("updates") val updates: List<UpdateEnvelopeDto>?,
    @SerializedName("state") val state: UpdatesStateDto?,
    /** Cursor too old (retention / 10k events): a full resync is needed instead of paging. */
    @SerializedName("tooLong") val tooLong: Boolean?
)

data class UpdatesStateDto(
    @SerializedName("updateSeq") val updateSeq: Long?
)

/** One row of the update stream; also the shape of a live `update` frame. */
data class UpdateEnvelopeDto(
    @SerializedName("updateSeq") val updateSeq: Long?,
    @SerializedName("kind") val kind: String?,
    /** Shape depends on [kind]; parsed lazily. */
    @SerializedName("payload") val payload: JsonObject?
)

data class MessageEditPayloadDto(
    @SerializedName("serverId") val serverId: Long?,
    @SerializedName("chatId") val chatId: String?,
    @SerializedName("body") val body: String?,
    @SerializedName("editedAt") val editedAt: Long?
)

data class MessageDeletePayloadDto(
    @SerializedName("serverId") val serverId: Long?,
    @SerializedName("chatId") val chatId: String?
)

/** `read` and `delivered` payloads. */
data class CursorPayloadDto(
    @SerializedName("chatId") val chatId: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("upToSeq") val upToSeq: Long?
)

data class MemberPayloadDto(
    @SerializedName("chatId") val chatId: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("removed") val removed: Boolean?
)

data class ChatPayloadDto(
    @SerializedName("chatId") val chatId: String?
)

// ---- WebSocket frames, client → server ----

data class AuthFrame(
    @SerializedName("token") val token: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("cursor") val cursor: Long,
    @SerializedName("type") val type: String = "auth"
)

/** The chat-message type goes in `messageType`: `type` is the frame envelope. */
data class SendFrame(
    @SerializedName("clientMessageId") val clientMessageId: String,
    @SerializedName("chatId") val chatId: String,
    @SerializedName("messageType") val messageType: String,
    @SerializedName("body") val body: String?,
    @SerializedName("mediaIds") val mediaIds: List<String>,
    /** clientMessageId of the message this one replies to (same chat). */
    @SerializedName("replyTo") val replyTo: String?,
    @SerializedName("type") val type: String = "send"
)

/** `read` / `received`. */
data class CursorFrame(
    @SerializedName("type") val type: String,
    @SerializedName("chatId") val chatId: String,
    @SerializedName("upToSeq") val upToSeq: Long
)

data class TypingFrame(
    @SerializedName("chatId") val chatId: String,
    @SerializedName("type") val type: String = "typing"
)

// ---- WebSocket frames, server → client ----

/**
 * Every server frame flattened into one shape; [type] says which fields are meaningful:
 * `auth_ok` (userId, updateSeq), `update` (updateSeq, kind, payload), `ack` (clientMessageId,
 * serverId, serverSeq, serverCreatedAt), `nack` (clientMessageId, code, message, retryable),
 * `typing` (chatId, userId), `presence` (userId, online, lastSeenAt), `error` (code, message).
 */
data class ServerFrameDto(
    @SerializedName("type") val type: String?,
    @SerializedName("userId") val userId: String?,
    @SerializedName("updateSeq") val updateSeq: Long?,
    @SerializedName("kind") val kind: String?,
    @SerializedName("payload") val payload: JsonObject?,
    @SerializedName("clientMessageId") val clientMessageId: String?,
    @SerializedName("serverId") val serverId: Long?,
    @SerializedName("serverSeq") val serverSeq: Long?,
    @SerializedName("serverCreatedAt") val serverCreatedAt: Long?,
    @SerializedName("code") val code: String?,
    @SerializedName("message") val message: String?,
    @SerializedName("retryable") val retryable: Boolean?,
    @SerializedName("chatId") val chatId: String?,
    @SerializedName("online") val online: Boolean?,
    @SerializedName("lastSeenAt") val lastSeenAt: Long?
)
