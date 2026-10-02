package com.example.overgram.domain.model

/**
 * [WaitingForNetwork]: the device has no network at all; [Disconnected]/[Connecting]: network is
 * there but the server isn't reachable / answering yet.
 */
enum class ConnectionState { WaitingForNetwork, Disconnected, Connecting, Connected }

/**
 * Something that happened on the server, delivered live over the WebSocket (or replayed from
 * `GET /v1/updates` after a gap). Applying an event twice must be harmless.
 */
sealed interface RealtimeEvent {

    data class MessageNew(val chatId: String, val message: Message) : RealtimeEvent

    data class MessageEdited(
        val chatId: String,
        val serverId: Long,
        val body: String,
        val editedAt: Long
    ) : RealtimeEvent

    data class MessageDeleted(val chatId: String, val serverId: Long) : RealtimeEvent

    /** [userId] has read [chatId] up to [upToSeq] (also sent for the caller's own other devices). */
    data class ReadReceipt(val chatId: String, val userId: String, val upToSeq: Long) : RealtimeEvent

    /** [userId]'s device has received [chatId] up to [upToSeq]. */
    data class Delivered(val chatId: String, val userId: String, val upToSeq: Long) : RealtimeEvent

    /** Someone was added to / removed from [chatId], or their role changed. */
    data class MemberChanged(val chatId: String, val userId: String, val removed: Boolean) : RealtimeEvent

    /** [chatId] was created, renamed or got a new avatar. */
    data class ChatChanged(val chatId: String) : RealtimeEvent

    /** A peer went online/offline. [lastSeenAt] is only set when offline. */
    data class Presence(val userId: String, val isOnline: Boolean, val lastSeenAt: Long?) : RealtimeEvent

    /** [userId] is typing in [chatId]. Repeated every few seconds while they keep typing. */
    data class Typing(val chatId: String, val userId: String) : RealtimeEvent

    /** Too much was missed to replay: reload whatever is on screen from REST. */
    data object Resynced : RealtimeEvent

    /** The server rejected this device (logged out elsewhere / revoked): back to login. */
    data object SessionEnded : RealtimeEvent
}
