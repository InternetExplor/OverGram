package com.example.overgram.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /** Outgoing (no seq yet) first, then history newest first — the order the list shows. */
    @Query(
        """
        SELECT * FROM messages WHERE chatId = :chatId
        ORDER BY serverSeq IS NOT NULL, serverSeq DESC, createdAt DESC
        """
    )
    fun observeChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT MIN(serverSeq) FROM messages WHERE chatId = :chatId AND serverSeq IS NOT NULL")
    suspend fun oldestSeq(chatId: String): Long?

    @Query("SELECT MAX(serverSeq) FROM messages WHERE chatId = :chatId AND serverSeq IS NOT NULL")
    suspend fun newestSeq(chatId: String): Long?

    @Query("SELECT * FROM messages WHERE clientMessageId = :clientMessageId")
    suspend fun get(clientMessageId: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE sendState = 'SENDING' ORDER BY createdAt")
    suspend fun outbox(): List<MessageEntity>

    @Upsert
    suspend fun upsert(messages: List<MessageEntity>)

    @Upsert
    suspend fun upsert(message: MessageEntity)

    @Query(
        """
        UPDATE messages SET serverId = :serverId, serverSeq = :serverSeq, createdAt = :createdAt,
            sendState = 'SENT', failureReason = NULL
        WHERE clientMessageId = :clientMessageId
        """
    )
    suspend fun markSent(clientMessageId: String, serverId: Long, serverSeq: Long, createdAt: Long)

    @Query("UPDATE messages SET sendState = :state, failureReason = :reason WHERE clientMessageId = :clientMessageId")
    suspend fun setSendState(clientMessageId: String, state: String, reason: String?)

    @Query("UPDATE messages SET body = :body, isEdited = 1 WHERE serverId = :serverId")
    suspend fun applyEdit(serverId: Long, body: String)

    @Query("UPDATE messages SET isDeleted = 1 WHERE serverId = :serverId")
    suspend fun applyDelete(serverId: Long)

    /** Confirmed history only: queued outgoing messages survive a history reset. */
    @Query("DELETE FROM messages WHERE chatId = :chatId AND serverSeq IS NOT NULL")
    suspend fun clearHistory(chatId: String)

    /**
     * A newest page that doesn't touch the cached history means messages were missed in
     * between: drop the stale part instead of showing a hole in the middle.
     */
    @Transaction
    suspend fun insertNewestPage(chatId: String, page: List<MessageEntity>) {
        val cachedTop = newestSeq(chatId)
        val pageBottom = page.mapNotNull { it.serverSeq }.minOrNull()
        if (cachedTop != null && pageBottom != null && pageBottom > cachedTop + 1) clearHistory(chatId)
        // Same clientMessageId as a queued row = the server confirmed it: overwrite.
        upsert(page)
    }
}

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY lastActivityAt DESC")
    fun observeAll(): Flow<List<ChatEntity>>

    @Upsert
    suspend fun upsert(chats: List<ChatEntity>)

    @Upsert
    suspend fun upsert(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<String>)

    @Query("DELETE FROM chats")
    suspend fun deleteAll()

    @Query("DELETE FROM chats WHERE id = :chatId")
    suspend fun delete(chatId: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE id = :chatId")
    suspend fun markRead(chatId: String)

    /** The server's list is the whole truth: chats we left or were removed from disappear. */
    @Transaction
    suspend fun replaceAll(chats: List<ChatEntity>) {
        if (chats.isEmpty()) deleteAll() else deleteAllExcept(chats.map { it.id })
        upsert(chats)
    }
}

@Dao
interface UserDao {

    @Query("SELECT * FROM users")
    fun observeAll(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id IN (:ids)")
    suspend fun get(ids: List<String>): List<UserEntity>

    @Upsert
    suspend fun upsert(users: List<UserEntity>)

    @Query("UPDATE users SET isOnline = :isOnline, lastSeenAt = :lastSeenAt WHERE id = :userId")
    suspend fun updatePresence(userId: String, isOnline: Boolean, lastSeenAt: Long?)
}
