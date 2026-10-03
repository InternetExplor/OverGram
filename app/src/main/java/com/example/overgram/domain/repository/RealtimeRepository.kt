package com.example.overgram.domain.repository

import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.RealtimeEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** The live connection to the server (WebSocket) and the event stream it produces. */
interface RealtimeRepository {

    val connectionState: StateFlow<ConnectionState>

    val events: SharedFlow<RealtimeEvent>

    /** Connect (and keep reconnecting) while the app is in the foreground. Idempotent. */
    fun start()

    /** Disconnect and stop reconnecting (app in background, logout). */
    fun stop()

    /** "I'm typing" in [chatId]; dropped silently when not connected. */
    fun sendTyping(chatId: String)
}
