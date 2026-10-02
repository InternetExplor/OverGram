package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatMember
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.GroupMembers
import com.example.overgram.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ChatRepository {

    /**
     * The current user's chats as stored on the device, most recently active first, with peer
     * profiles (name + last known presence) resolved. Emits immediately, then on every change.
     */
    fun observeChats(): Flow<List<ChatSummary>>

    /** Replaces the stored chat list with the server's. */
    suspend fun refreshChats(): AuthOutcome<Unit>

    /** A user's public profile, including online status. */
    suspend fun getUser(userId: String): AuthOutcome<UserProfile>

    /**
     * Best-effort profiles for several users (e.g. group message senders). Users whose profile
     * can't be loaded are missing from the result; known ones come from the local cache.
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
