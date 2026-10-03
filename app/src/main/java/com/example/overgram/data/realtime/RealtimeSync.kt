package com.example.overgram.data.realtime

import com.example.overgram.data.local.db.MessageDao
import com.example.overgram.data.local.db.UserDao
import com.example.overgram.data.mapper.toEntity
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.RealtimeEvent
import com.example.overgram.domain.repository.MessageRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes live server events into the local database, so every screen that observes it
 * updates by itself, and sends queued messages whenever the connection comes back.
 * Runs for the whole app process (started from the Application).
 */
@Singleton
class RealtimeSync @Inject constructor(
    private val realtime: RealtimeClient,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val messageRepository: MessageRepository,
    private val gson: Gson
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            realtime.events.collect { event ->
                try {
                    apply(event)
                } catch (e: Exception) {
                    Timber.e(e, "Couldn't store $event")
                }
            }
        }
        scope.launch {
            realtime.connectionState
                .filter { it == ConnectionState.Connected }
                .collect { messageRepository.flushOutbox() }
        }
        // Messages queued before the app was killed.
        scope.launch { messageRepository.flushOutbox() }
    }

    private suspend fun apply(event: RealtimeEvent) {
        when (event) {
            // Also the echo of our own message: same clientMessageId, so it replaces the queued row.
            is RealtimeEvent.MessageNew -> messageDao.upsert(event.message.toEntity(event.chatId, gson))
            is RealtimeEvent.MessageEdited -> messageDao.applyEdit(event.serverId, event.body)
            is RealtimeEvent.MessageDeleted -> messageDao.applyDelete(event.serverId)
            is RealtimeEvent.Presence -> userDao.updatePresence(event.userId, event.isOnline, event.lastSeenAt)
            // Chat list rows (unread, preview, membership) are refreshed from the server by the list.
            else -> Unit
        }
    }
}
