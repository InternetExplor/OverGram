package com.example.overgram.domain.repository

import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary

interface ChatRepository {

    /**
     * The current user's chats, most recently active first, with the peer profile
     * (name + online status) resolved for every DIRECT chat.
     */
    suspend fun getChats(): AuthOutcome<List<ChatSummary>>

    /** Id of the logged-in user, used to tell own messages apart in previews. */
    fun currentUserId(): String?
}
