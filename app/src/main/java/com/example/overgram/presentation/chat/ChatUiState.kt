package com.example.overgram.presentation.chat

import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.MediaAttachment
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

/**
 * [Sent]: the server has it; [Delivered]: some other member's device has it; [Read]: some other
 * member has read it.
 */
enum class OutgoingState { Sending, Sent, Delivered, Read, Failed }

/**
 * The message a bubble replies to. [message] is null when it isn't loaded on this device (older
 * history): the quote then just says it's a reply.
 */
data class ReplyQuote(val clientMessageId: String, val message: ChatMessageItem?)

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
    val systemEvent: SystemEvent? = null,
    /** IMAGE / VIDEO / FILE messages. */
    val media: MediaAttachment? = null,
    val replyTo: ReplyQuote? = null
) {
    /** Only text and captions can be edited, only our own, only once sent, and not after deletion. */
    val canEdit: Boolean
        get() = isOutgoing && !isDeleted && serverSeq != null && type != MessageType.SYSTEM &&
            (type == MessageType.TEXT || type == MessageType.IMAGE || type == MessageType.VIDEO)

    val canDelete: Boolean get() = isOutgoing && !isDeleted

    val canReply: Boolean get() = serverSeq != null && !isDeleted && type != MessageType.SYSTEM
}

/** One-off problems with attachments, for a snackbar. */
enum class MediaNotice { TooLarge, Unreadable, NoAppToOpen, DownloadFailed }

data class ChatUiState(
    val type: ChatType,
    val title: String,
    val currentUserId: String?,
    /** DIRECT only. */
    val peer: UserProfile? = null,
    val isMuted: Boolean = false,
    /** Other members typing right now. */
    val typingUserIds: Set<String> = emptySet(),
    /** Shown instead of presence while live updates aren't flowing. */
    val connectionState: ConnectionState = ConnectionState.Connected,
    /** Newest first, matching the reversed message list. */
    val messages: List<ChatMessageItem> = emptyList(),
    /** Senders and users mentioned by system events, by id. */
    val profiles: Map<String, UserProfile> = emptyMap(),
    val input: String = "",
    /** The message being answered: shown above the input, sent as `replyTo`. */
    val replyingTo: ChatMessageItem? = null,
    /** Our message being edited: its text is in [input], Send saves the edit. */
    val editing: ChatMessageItem? = null,
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val hasOlder: Boolean = false,
    /** First load failed with nothing to show. */
    val loadError: AuthError? = null,
    /** Transient error for a snackbar. */
    val error: AuthError? = null,
    val mediaNotice: MediaNotice? = null,
    /** Our attachments being uploaded: progress 0..1 by clientMessageId. */
    val uploadProgress: Map<String, Float> = emptyMap(),
    val isSessionEnded: Boolean = false,
    val renameDialog: RenameDialogState? = null,
    val isLeaving: Boolean = false,
    /** The user left the group: close the screen. */
    val hasLeft: Boolean = false
) {
    val isGroup: Boolean get() = type == ChatType.GROUP

    /** Presence and typing are only current while connected; otherwise they're stale. */
    val isLive: Boolean get() = connectionState == ConnectionState.Connected

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
