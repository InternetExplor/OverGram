package com.example.overgram.presentation.chat

import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SystemEvent
import com.example.overgram.domain.model.SystemEventKind
import com.example.overgram.domain.model.UserProfile

/** What the chat screen is opened with. */
data class ChatArgs(
    val chatId: String,
    val type: ChatType,
    val peerUserId: String?,
    /** Shown until the chat details load. */
    val title: String
)

/** [Read]: some other member has read up to this message. */
enum class OutgoingState { Sending, Sent, Read, Failed }

/**
 * One bubble. Confirmed messages have a [serverSeq]; a message still being sent (or that
 * failed) only has its [clientMessageId].
 */
data class ChatMessageItem(
    val clientMessageId: String,
    val serverSeq: Long?,
    val senderId: String,
    val type: MessageType,
    val body: String?,
    val createdAt: Long,
    val isOutgoing: Boolean,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    /** Null for incoming messages. */
    val outgoingState: OutgoingState? = null,
    /** Why the server refused an outgoing message ([OutgoingState.Failed]). */
    val failureReason: String? = null,
    /** Parsed SYSTEM message, rendered as a centered note instead of a bubble. */
    val systemEvent: SystemEvent? = null
)

data class ChatUiState(
    val type: ChatType,
    val title: String,
    val currentUserId: String?,
    /** DIRECT only. */
    val peer: UserProfile? = null,
    val isMuted: Boolean = false,
    /** Other members typing right now. */
    val typingUserIds: Set<String> = emptySet(),
    /** Newest first, matching the reversed message list. */
    val messages: List<ChatMessageItem> = emptyList(),
    /** Senders and users mentioned by system events, by id. */
    val profiles: Map<String, UserProfile> = emptyMap(),
    val input: String = "",
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val hasOlder: Boolean = false,
    /** First load failed with nothing to show. */
    val loadError: AuthError? = null,
    /** Transient error for a snackbar. */
    val error: AuthError? = null,
    val isSessionEnded: Boolean = false,
    val renameDialog: RenameDialogState? = null,
    val isLeaving: Boolean = false,
    /** The user left the group: close the screen. */
    val hasLeft: Boolean = false
) {
    val isGroup: Boolean get() = type == ChatType.GROUP

    /**
     * Group members as far as the loaded history tells (Relay has no member-list endpoint):
     * senders plus users added by system events, minus those removed or who left since.
     */
    val knownMemberIds: List<String>
        get() {
            val members = LinkedHashSet<String>()
            messages.asReversed().forEach { message ->
                val event = message.systemEvent
                when (event?.kind) {
                    SystemEventKind.GROUP_CREATED, SystemEventKind.MEMBERS_ADDED -> {
                        members += event.actorId
                        members += event.targetUserIds
                    }
                    SystemEventKind.MEMBER_REMOVED, SystemEventKind.MEMBER_LEFT ->
                        members -= event.targetUserIds.ifEmpty { listOf(event.actorId) }.toSet()
                    else -> if (message.type != MessageType.SYSTEM) members += message.senderId
                }
            }
            return members.toList()
        }
}

data class RenameDialogState(
    val input: String,
    val isSaving: Boolean = false,
    val error: AuthError? = null
)
