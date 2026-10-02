package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.StoredMessage
import kotlinx.coroutines.flow.Flow

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

    /** Queues a TEXT message and starts sending it. Returns its clientMessageId. */
    suspend fun sendText(chatId: String, text: String): String

    /** Puts a FAILED message back in the queue. */
    suspend fun retry(clientMessageId: String)

    /** Sends everything still queued, oldest first (on reconnect, at app start). */
    suspend fun flushOutbox()

    /** Moves the caller's read cursor forward (never backwards). */
    suspend fun markRead(chatId: String, upToSeq: Long): AuthOutcome<Unit>
}
