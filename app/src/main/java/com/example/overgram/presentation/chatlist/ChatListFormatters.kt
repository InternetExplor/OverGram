package com.example.overgram.presentation.chatlist

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.example.overgram.R
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.UserProfile
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
        MessageType.SYSTEM -> return stringResource(R.string.chats_preview_system)
        MessageType.UNKNOWN -> stringResource(R.string.chats_preview_unsupported)
    }.replace('\n', ' ')

    return if (currentUserId != null && message.senderId == currentUserId) {
        stringResource(R.string.chats_preview_you, text)
    } else {
        text
    }
}

private fun String?.withPrefix(label: String): String =
    if (isNullOrBlank()) label else "$label, $this"

@Composable
fun presenceText(user: UserProfile, now: Long = System.currentTimeMillis()): String {
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
fun chatTime(timestamp: Long, now: Long = System.currentTimeMillis()): String {
    if (timestamp <= 0L) return ""
    return when {
        isSameDay(timestamp, now) -> formatClock(timestamp)
        now - timestamp < WEEK_MS -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> formatDate(timestamp)
    }
}

@Composable
private fun formatClock(timestamp: Long): String =
    DateFormat.getTimeFormat(LocalContext.current).format(Date(timestamp))

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("dd.MM.yy", Locale.getDefault()).format(Date(timestamp))

private fun isSameDay(a: Long, b: Long): Boolean {
    val first = Calendar.getInstance().apply { timeInMillis = a }
    val second = Calendar.getInstance().apply { timeInMillis = b }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

private const val DAY_MS = 24 * 60 * 60 * 1000L
private const val WEEK_MS = 7 * DAY_MS
