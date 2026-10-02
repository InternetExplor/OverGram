package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatMember
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.GroupMembers
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

    /**
     * Best-effort profiles for several users (e.g. group message senders). Users whose profile
     * can't be loaded are missing from the result; recently loaded ones come from a cache.
     */
    suspend fun getUsers(userIds: Collection<String>): Map<String, UserProfile>

    /** A single chat, with the peer resolved for DIRECT chats. 403 once the caller left it. */
    suspend fun getChat(chatId: String): AuthOutcome<ChatSummary>

    /** Creates a GROUP chat with the caller as OWNER. Returns its id. */
    suspend fun createGroup(title: String, memberIds: List<String>): AuthOutcome<String>

    /** ADMIN/OWNER only. */
    suspend fun renameGroup(chatId: String, title: String): AuthOutcome<ChatSummary>

    /** ADMIN/OWNER only. Returns the group's updated member list. */
    suspend fun addMembers(chatId: String, userIds: List<String>): AuthOutcome<List<ChatMember>>

    suspend fun leaveChat(chatId: String): AuthOutcome<Unit>

    /**
     * Current members and owner of a group, rebuilt from its SYSTEM messages
     * (created / added / removed / left / owner changed).
     */
    suspend fun getGroupMembers(chatId: String): AuthOutcome<GroupMembers>

    /** OWNER/ADMIN only. */
    suspend fun removeMember(chatId: String, userId: String): AuthOutcome<Unit>

    /** The caller's own mute setting for a chat. */
    suspend fun setMuted(chatId: String, muted: Boolean): AuthOutcome<ChatSummary>

    /** History page, newest first. [beforeSeq] = null for the newest page. */
    suspend fun getMessages(chatId: String, beforeSeq: Long?): AuthOutcome<MessagePage>

    /**
     * Sends a TEXT message. Retrying with the same [clientMessageId] never duplicates it:
     * the server returns the original result.
     */
    suspend fun sendText(chatId: String, clientMessageId: String, text: String): AuthOutcome<SentMessage>

    /** Moves the caller's read cursor forward (never backwards). */
    suspend fun markRead(chatId: String, upToSeq: Long): AuthOutcome<Unit>

    /** Users whose username starts with [query] (case-insensitive). */
    suspend fun searchUsers(query: String): AuthOutcome<List<UserProfile>>

    /** The DIRECT chat with [peerUserId], created if it doesn't exist yet. Returns its id. */
    suspend fun openDirectChat(peerUserId: String): AuthOutcome<String>

    /** The logged-in user's own profile. */
    suspend fun getMe(): AuthOutcome<UserProfile>

    /** Sets the caller's username; fails with [com.example.overgram.domain.model.AuthError.Validation] if taken. */
    suspend fun setUsername(username: String): AuthOutcome<UserProfile>

    /** Id of the logged-in user, used to tell own messages apart in previews. */
    fun currentUserId(): String?
}
