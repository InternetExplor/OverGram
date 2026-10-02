package com.example.overgram.presentation.chatlist

import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.UserProfile

data class ChatListUiState(
    val chats: List<ChatSummary> = emptyList(),
    val currentUserId: String? = null,
    /** First load, nothing to show yet. */
    val isLoading: Boolean = true,
    /** Pull-to-refresh in progress. */
    val isRefreshing: Boolean = false,
    val error: AuthError? = null,
    /** The session was revoked/expired or the user logged out: go back to the auth flow. */
    val isSessionEnded: Boolean = false,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    /** chatId → ids of users typing there right now. */
    val typing: Map<String, Set<String>> = emptyMap()
) {
    /** Peers of DIRECT chats that currently have a live connection, most recent chat first. */
    val onlineUsers: List<UserProfile>
        get() = chats.mapNotNull { it.peer }
            .filter { it.isOnline && it.id != currentUserId }
            .distinctBy { it.id }
}
