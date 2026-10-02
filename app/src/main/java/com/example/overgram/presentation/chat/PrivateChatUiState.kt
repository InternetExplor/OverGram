package com.example.overgram.presentation.chat

import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.UserProfile

/** What the chat screen is opened with. */
data class PrivateChatArgs(
    val chatId: String,
    val peerUserId: String?,
    /** Shown until the peer profile loads. */
    val title: String
)

enum class OutgoingState { Sending, Sent, Failed }

/**
 * One bubble. Confirmed messages have a [serverSeq]; a message still being sent (or that
 * failed) only has its [clientMessageId].
 */
data class ChatMessageItem(
    val clientMessageId: String,
    val serverSeq: Long?,
    val type: MessageType,
    val body: String?,
    val createdAt: Long,
    val isOutgoing: Boolean,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    /** Null for incoming messages. */
    val outgoingState: OutgoingState? = null
)

data class PrivateChatUiState(
    val title: String,
    val peer: UserProfile? = null,
    /** Newest first, matching the reversed message list. */
    val messages: List<ChatMessageItem> = emptyList(),
    val input: String = "",
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val hasOlder: Boolean = false,
    /** First load failed with nothing to show. */
    val loadError: AuthError? = null,
    /** Transient error for a snackbar. */
    val error: AuthError? = null,
    val isSessionEnded: Boolean = false
) {
    val canSend: Boolean get() = input.isNotBlank()
}
