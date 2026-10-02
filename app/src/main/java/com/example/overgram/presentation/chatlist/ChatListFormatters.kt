package com.example.overgram.presentation.chatlist

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.overgram.R
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SystemEvent
import com.example.overgram.domain.model.SystemEventKind
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.ui.time.LocalNow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun chatTitle(chat: ChatSummary): String = when (chat.type) {
    ChatType.DIRECT -> chat.peer?.displayName ?: stringResource(R.string.chats_unknown_user)
    ChatType.GROUP -> chat.title?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chats_untitled_group)
}

/**
 * Second line of a chat row: the last message, or — for a DIRECT chat with no messages —
 * the peer's presence.
 */
@Composable
fun chatSubtitle(chat: ChatSummary, currentUserId: String?): String {
    val message = chat.lastMessage
        ?: return chat.peer?.let { presenceText(it) }
            ?: stringResource(R.string.chats_preview_no_messages)

    if (message.isDeleted) return stringResource(R.string.chats_preview_deleted)

    val text = when (message.type) {
        MessageType.TEXT -> message.body.orEmpty()
        MessageType.IMAGE -> message.body.withPrefix(stringResource(R.string.chats_preview_photo))
        MessageType.VIDEO -> message.body.withPrefix(stringResource(R.string.chats_preview_video))
        MessageType.FILE -> message.body.withPrefix(stringResource(R.string.chats_preview_file))
        // SYSTEM bodies are structured JSON, never display text.
        MessageType.SYSTEM -> return message.systemEvent?.let { event ->
            val sender = chat.lastMessageSender
            systemEventText(event, currentUserId) { id -> sender?.takeIf { it.id == id } }
        } ?: stringResource(R.string.chats_preview_system)
        MessageType.UNKNOWN -> stringResource(R.string.chats_preview_unsupported)
    }.replace('\n', ' ')

    return when {
        // Like Telegram: "You:" only in groups; a direct chat shows ticks instead.
        chat.type == ChatType.GROUP && currentUserId != null && message.senderId == currentUserId ->
            stringResource(R.string.chats_preview_you, text)
        chat.type == ChatType.GROUP && chat.lastMessageSender != null ->
            stringResource(R.string.chats_preview_sender, chat.lastMessageSender.displayName, text)
        else -> text
    }
}

/**
 * Renders a group SYSTEM event ("Ada added Bob and Carl"). [profileOf] returns what's known
 * about a user; when some targets are unknown the text falls back to a count.
 */
@Composable
fun systemEventText(
    event: SystemEvent,
    currentUserId: String?,
    profileOf: (String) -> UserProfile?
): String {
    val actor = personName(event.actorId, currentUserId, profileOf)
    val targets = event.targetUserIds
    val targetNames = targets.map { id ->
        if (id == currentUserId) stringResource(R.string.system_you_object) else profileOf(id)?.displayName
    }
    val targetsText = if (targetNames.all { it != null }) {
        joinNames(targetNames.filterNotNull())
    } else {
        pluralStringResource(R.plurals.system_members_count, targets.size, targets.size)
    }
    val firstTarget = targets.firstOrNull()?.let { personName(it, currentUserId, profileOf) } ?: actor

    // Separate "You …" sentences: other languages conjugate the verb by subject
    // (ru: «Вы удалили» vs «Ада удалила»), so the actor can't just be slotted in.
    val byMe = currentUserId != null && event.actorId == currentUserId
    return when (event.kind) {
        SystemEventKind.GROUP_CREATED -> when {
            byMe && event.title != null -> stringResource(R.string.system_you_group_created_titled, event.title)
            byMe -> stringResource(R.string.system_you_group_created)
            event.title != null -> stringResource(R.string.system_group_created_titled, actor, event.title)
            else -> stringResource(R.string.system_group_created, actor)
        }
        SystemEventKind.MEMBERS_ADDED -> if (byMe) {
            stringResource(R.string.system_you_members_added, targetsText)
        } else {
            stringResource(R.string.system_members_added, actor, targetsText)
        }
        SystemEventKind.MEMBER_REMOVED -> if (byMe) {
            stringResource(R.string.system_you_member_removed, targetsText)
        } else {
            stringResource(R.string.system_member_removed, actor, targetsText)
        }
        SystemEventKind.MEMBER_LEFT -> if (event.targetUserIds.firstOrNull() == currentUserId || (byMe && event.targetUserIds.isEmpty())) {
            stringResource(R.string.system_you_member_left)
        } else {
            stringResource(R.string.system_member_left, firstTarget)
        }
        SystemEventKind.OWNER_CHANGED -> if (event.targetUserIds.firstOrNull() == currentUserId) {
            stringResource(R.string.system_you_owner)
        } else {
            stringResource(R.string.system_owner_changed, firstTarget)
        }
        SystemEventKind.ROLE_CHANGED -> if (byMe) {
            stringResource(R.string.system_you_role_changed, targetsText)
        } else {
            stringResource(R.string.system_role_changed, actor, targetsText)
        }
        SystemEventKind.UNKNOWN -> stringResource(R.string.chats_preview_system)
    }
}

/** Subject form: "You" for the current user, the display name, or "Someone". */
@Composable
private fun personName(userId: String, currentUserId: String?, profileOf: (String) -> UserProfile?): String =
    when (userId) {
        currentUserId -> stringResource(R.string.system_you_subject)
        else -> profileOf(userId)?.displayName ?: stringResource(R.string.system_someone)
    }

/** "Ada", "Ada and Bob", "Ada, Bob and Carl". */
@Composable
fun joinNames(names: List<String>): String = when (names.size) {
    0 -> ""
    1 -> names.single()
    else -> stringResource(R.string.names_and, names.dropLast(1).joinToString(", "), names.last())
}

private fun String?.withPrefix(label: String): String =
    if (isNullOrBlank()) label else "$label, $this"

/** "Waiting for network…" / "Connecting…" while live updates aren't flowing; null when connected. */
@Composable
fun connectionStatusText(state: ConnectionState): String? = when (state) {
    ConnectionState.WaitingForNetwork -> stringResource(R.string.connection_waiting_for_network)
    ConnectionState.Disconnected, ConnectionState.Connecting -> stringResource(R.string.connection_connecting)
    ConnectionState.Connected -> null
}

@Composable
fun presenceText(user: UserProfile, now: Long = LocalNow.current): String {
    if (user.isOnline) return stringResource(R.string.presence_online)
    val lastSeen = user.lastSeenAt ?: return stringResource(R.string.presence_last_seen_recently)

    val minutes = ((now - lastSeen) / 60_000L).toInt()
    return when {
        minutes < 1 -> stringResource(R.string.presence_last_seen_just_now)
        minutes < 60 -> pluralStringResource(R.plurals.presence_last_seen_minutes, minutes, minutes)
        isSameDay(lastSeen, now) ->
            stringResource(R.string.presence_last_seen_today, formatClock(lastSeen))
        isSameDay(lastSeen, now - DAY_MS) ->
            stringResource(R.string.presence_last_seen_yesterday, formatClock(lastSeen))
        else -> stringResource(R.string.presence_last_seen_date, formatDate(lastSeen))
    }
}

/** "21:32" today, "Mon" within the last week, "28.09.26" otherwise. */
@Composable
fun chatTime(timestamp: Long, now: Long = LocalNow.current): String {
    if (timestamp <= 0L) return ""
    return when {
        isSameDay(timestamp, now) -> formatClock(timestamp)
        now - timestamp < WEEK_MS -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> formatDate(timestamp)
    }
}

/** Local wall-clock time, honouring the device's 12/24-hour setting. */
@Composable
fun formatClock(timestamp: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(timestamp))

/** "Today", "Yesterday", or "2 October" (with the year if it isn't the current one). */
@Composable
fun dayLabel(timestamp: Long, now: Long = LocalNow.current): String = when {
    isSameDay(timestamp, now) -> stringResource(R.string.day_today)
    isSameDay(timestamp, now - DAY_MS) -> stringResource(R.string.day_yesterday)
    else -> {
        val sameYear = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR) ==
            Calendar.getInstance().apply { timeInMillis = now }.get(Calendar.YEAR)
        val pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), if (sameYear) "dMMMM" else "dMMMMyyyy")
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }
}

fun isSameDay(a: Long, b: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = a }
    val second = Calendar.getInstance().apply { timeInMillis = b }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val WEEK_MS = 7 * DAY_MS
