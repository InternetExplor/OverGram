package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.MessagePage
import com.example.overgram.domain.model.SentMessage
import com.example.overgram.domain.model.UserProfile

interface ChatRepository {

    /**
     * The current user's chats, most recently active first, with the peer profile
     * (name + online status) resolved for every DIRECT chat.
     */
    suspend fun getChats(): AuthOutcome<List<ChatSummary>>

    /** A user's public profile, including online status. */
    suspend fun getUser(userId: String): AuthOutcome<UserProfile>

    /** History page, newest first. [beforeSeq] = null for the newest page. */
    suspend fun getMessages(chatId: String, beforeSeq: Long?): AuthOutcome<MessagePage>

    /**
     * Sends a TEXT message. Retrying with the same [clientMessageId] never duplicates it:
     * the server returns the original result.
     */
    suspend fun sendText(chatId: String, clientMessageId: String, text: String): AuthOutcome<SentMessage>

    /** Moves the caller's read cursor forward (never backwards). */
    suspend fun markRead(chatId: String, upToSeq: Long): AuthOutcome<Unit>

    /** Id of the logged-in user, used to tell own messages apart in previews. */
    fun currentUserId(): String?
}
