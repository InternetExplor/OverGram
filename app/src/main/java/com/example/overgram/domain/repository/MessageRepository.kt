package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatReceipts
import com.example.overgram.domain.model.MediaSendResult
import com.example.overgram.domain.model.StoredMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Message history, stored on the device. Screens observe the local copy; the network
 * (REST pages, live WebSocket updates) only ever writes into it.
 */
interface MessageRepository {

    /** Queued outgoing messages first, then history newest first. Emits on every change. */
    fun observeMessages(chatId: String): Flow<List<StoredMessage>>

    /** Fetches the newest page into the local history. */
    suspend fun refreshNewest(chatId: String): AuthOutcome<Unit>

    /**
     * Fetches the page before the oldest stored message. Returns whether there is still
     * older history (the first message of a chat has seq 1).
     */
    suspend fun loadOlder(chatId: String): AuthOutcome<Boolean>

    /**
     * Queues a TEXT message and starts sending it. [replyTo] is the clientMessageId of the
     * message being answered. Returns the new message's clientMessageId.
     */
    suspend fun sendText(chatId: String, text: String, replyTo: String? = null): String

    /**
     * Copies the picked item ([uri], a content Uri) into the outbox and queues it as a photo/video
     * message ([asFile] false) or a document ([asFile] true). [caption] is ignored for documents,
     * whose body is the file name.
     */
    suspend fun sendMedia(
        chatId: String,
        uri: String,
        caption: String?,
        asFile: Boolean,
        replyTo: String? = null
    ): MediaSendResult

    /** Upload progress (0..1) of queued attachments, by clientMessageId. */
    val uploadProgress: StateFlow<Map<String, Float>>

    /** Drops a message that isn't sent yet (e.g. stops an upload). No-op for sent messages. */
    suspend fun cancelSending(clientMessageId: String)

    /** Replaces the text (or caption) of our own sent message. Sender only, within 48 h. */
    suspend fun editMessage(clientMessageId: String, text: String): AuthOutcome<Unit>

    /**
     * Deletes a message for everyone (a tombstone: "Message deleted" stays in its place). Our own
     * unsent message is simply dropped from the outbox.
     */
    suspend fun deleteMessage(clientMessageId: String): AuthOutcome<Unit>

    /** Delivery receipt: this device has [chatId] up to [upToSeq]. Max-wins on the server. */
    suspend fun markDelivered(chatId: String, upToSeq: Long)

    /** How far the other members got with our messages; kept across restarts. */
    fun observeReceipts(chatId: String): Flow<ChatReceipts>

    /** Puts a FAILED message back in the queue. */
    suspend fun retry(clientMessageId: String)

    /** Sends everything still queued, oldest first (on reconnect, at app start). */
    suspend fun flushOutbox()

    /** Moves the caller's read cursor forward (never backwards). */
    suspend fun markRead(chatId: String, upToSeq: Long): AuthOutcome<Unit>
}
