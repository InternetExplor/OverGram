package com.example.overgram.data.realtime

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Base64
import com.example.overgram.BuildConfig
import com.example.overgram.core.di.RealtimeHttpClient
import com.example.overgram.core.network.TokenRefresher
import com.example.overgram.data.local.prefs.SyncPreferences
import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.mapper.toDomain
import com.example.overgram.data.remote.api.SyncApi
import com.example.overgram.data.remote.dto.AuthFrame
import com.example.overgram.data.remote.dto.ChatPayloadDto
import com.example.overgram.data.remote.dto.CursorFrame
import com.example.overgram.data.remote.dto.CursorPayloadDto
import com.example.overgram.data.remote.dto.MemberPayloadDto
import com.example.overgram.data.remote.dto.MessageDeletePayloadDto
import com.example.overgram.data.remote.dto.MessageDto
import com.example.overgram.data.remote.dto.MessageEditPayloadDto
import com.example.overgram.data.remote.dto.SendFrame
import com.example.overgram.data.remote.dto.ServerFrameDto
import com.example.overgram.data.remote.dto.TypingFrame
import com.example.overgram.data.remote.dto.UpdateEnvelopeDto
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.RealtimeEvent
import com.example.overgram.domain.model.SentMessage
import com.example.overgram.domain.repository.RealtimeRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

/** Outcome of a message sent over the socket. */
sealed interface SendResult {
    data class Acked(val sent: SentMessage) : SendResult
    data class Nacked(val code: String?, val message: String?, val retryable: Boolean) : SendResult
}

/**
 * The WebSocket half of Relay (`GET /v1/ws`, see /docs/ws).
 *
 * Every state change (start/stop, socket callbacks, timers) goes through one command queue that a
 * single coroutine drains, so live frames, gap catch-up, reconnects and token rotation are
 * strictly ordered without locks. Live `update` frames are applied only in `updateSeq` order:
 * anything that isn't `cursor + 1` triggers a REST catch-up (`GET /v1/updates`), duplicates are
 * dropped, and the cursor is persisted so a restart resumes where it left off.
 */
@Singleton
class RealtimeClient @Inject constructor(
    @ApplicationContext private val context: Context,
    @RealtimeHttpClient private val httpClient: OkHttpClient,
    private val syncApi: SyncApi,
    private val tokenPreferences: TokenPreferences,
    private val syncPreferences: SyncPreferences,
    private val tokenRefresher: TokenRefresher,
    private val gson: Gson
) : RealtimeRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commands = Channel<Command>(Channel.UNLIMITED)

    /** Socket lifecycle as the command loop sees it; [connectionState] adds "no network at all". */
    private val socketState = MutableStateFlow(ConnectionState.Disconnected)
    private val networkAvailable = MutableStateFlow(true)

    override val connectionState: StateFlow<ConnectionState> =
        combine(socketState, networkAvailable) { socket, network ->
            if (!network && socket != ConnectionState.Connected) ConnectionState.WaitingForNetwork else socket
        }.stateIn(scope, SharingStarted.Eagerly, ConnectionState.Disconnected)

    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = EVENT_BUFFER)
    override val events: SharedFlow<RealtimeEvent> = _events.asSharedFlow()

    /** chatId → userId → highest read seq seen. */
    private val readCursorsByChat = MutableStateFlow<Map<String, Map<String, Long>>>(emptyMap())

    /** Read from any thread by senders; written by the command loop. */
    @Volatile
    private var socket: WebSocket? = null
    private val pendingAcks = ConcurrentHashMap<String, CompletableDeferred<SendResult?>>()

    // Owned by the command loop.
    private var wanted = false
    private var generation = 0
    private var attempt = 0
    private var unauthorizedStreak = 0
    private var tokenInUse: String? = null
    private var reconnectJob: Job? = null
    private var rotateJob: Job? = null

    private val wsUrl = BuildConfig.API_BASE_URL.replaceFirst("http", "ws") + "v1/ws"

    init {
        // Coming back online (airplane mode off, Wi-Fi back): connect now, not after the backoff
        // that grew while every attempt failed.
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        networkAvailable.value = connectivity?.activeNetwork != null
        try {
            connectivity?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    networkAvailable.value = true
                    commands.trySend(Command.NetworkAvailable)
                }

                override fun onLost(network: Network) {
                    // When switching Wi-Fi ↔ mobile the old network is lost after the new one
                    // became the default; only report "no network" if nothing replaced it.
                    networkAvailable.value = connectivity.activeNetwork != null
                }
            })
        } catch (e: SecurityException) {
            Timber.w(e, "Realtime: can't watch connectivity")
        }

        scope.launch {
            for (command in commands) {
                try {
                    handle(command)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Realtime: failed to handle $command")
                }
            }
        }
    }

    override fun start() {
        commands.trySend(Command.Start)
    }

    override fun stop() {
        commands.trySend(Command.Stop)
    }

    override fun readCursors(chatId: String): Flow<Map<String, Long>> =
        readCursorsByChat.map { it[chatId].orEmpty() }.distinctUntilChanged()

    override fun sendTyping(chatId: String) {
        if (socketState.value == ConnectionState.Connected) {
            socket?.send(gson.toJson(TypingFrame(chatId)))
        }
    }

    /**
     * Sends a TEXT message and waits for its `ack`/`nack`. Returns null when the socket is down or
     * no answer came in time: the caller then falls back to REST with the same clientMessageId,
     * which the server deduplicates.
     */
    suspend fun sendMessage(chatId: String, clientMessageId: String, text: String): SendResult? {
        if (socketState.value != ConnectionState.Connected) return null
        val ws = socket ?: return null
        val deferred = CompletableDeferred<SendResult?>()
        pendingAcks[clientMessageId] = deferred
        return try {
            val frame = SendFrame(clientMessageId, chatId, MessageType.TEXT.name, text)
            if (!ws.send(gson.toJson(frame))) return null
            withTimeoutOrNull(ACK_TIMEOUT_MS) { deferred.await() }
        } finally {
            pendingAcks.remove(clientMessageId, deferred)
        }
    }

    /** Advances the read cursor over the socket; false if not connected (use REST then). */
    fun sendRead(chatId: String, upToSeq: Long): Boolean =
        socketState.value == ConnectionState.Connected &&
            socket?.send(gson.toJson(CursorFrame("read", chatId, upToSeq))) == true

    // ---- command loop ----

    private sealed interface Command {
        data object Start : Command
        data object Stop : Command
        data object Reconnect : Command
        /** Reconnect with the freshly rotated access token. */
        data object Rotate : Command
        data object CatchUp : Command
        data object NetworkAvailable : Command
        data class Opened(val generation: Int) : Command
        data class Text(val generation: Int, val text: String) : Command
        data class Closed(val generation: Int, val code: Int?, val reason: String?) : Command
    }

    private suspend fun handle(command: Command) {
        when (command) {
            Command.Start -> {
                wanted = true
                if (socket == null) {
                    // Coming back to the foreground: connect now rather than after a backoff.
                    reconnectJob?.cancel()
                    attempt = 0
                    connect()
                }
            }
            Command.Stop -> {
                wanted = false
                disconnect()
            }
            Command.Reconnect -> if (wanted && socket == null) connect()
            Command.NetworkAvailable -> if (wanted && socket == null) {
                reconnectJob?.cancel()
                attempt = 0
                connect()
            }
            Command.Rotate -> if (wanted) {
                disconnect()
                connect()
            }
            Command.CatchUp -> if (socketState.value == ConnectionState.Connected) catchUp()
            is Command.Opened -> if (command.generation == generation) sendAuth()
            is Command.Text -> if (command.generation == generation) onFrame(command.text)
            is Command.Closed -> if (command.generation == generation) onClosed(command.code, command.reason)
        }
    }

    private suspend fun connect() {
        if (!wanted || socket != null) return
        val token = tokenPreferences.getAccessToken()
        if (token.isNullOrEmpty() || tokenPreferences.getDeviceId().isNullOrEmpty()) {
            wanted = false // logged out
            return
        }
        socketState.value = ConnectionState.Connecting
        // A fresh install starts from the server's current position, not from history.
        if (syncPreferences.getCursor() == null && !bootstrapCursor()) {
            socketState.value = ConnectionState.Disconnected
            scheduleReconnect()
            return
        }
        val gen = ++generation
        tokenInUse = tokenPreferences.getAccessToken() // bootstrap may have refreshed it
        socket = httpClient.newWebSocket(Request.Builder().url(wsUrl).build(), Listener(gen))
    }

    /** Closes the current socket (if any) without triggering a reconnect. */
    private fun disconnect() {
        reconnectJob?.cancel()
        rotateJob?.cancel()
        generation++ // ignore whatever the old socket still reports
        socket?.close(NORMAL_CLOSURE, null)
        socket = null
        socketState.value = ConnectionState.Disconnected
        failPendingAcks()
    }

    private fun sendAuth() {
        val frame = AuthFrame(
            token = tokenInUse.orEmpty(),
            deviceId = tokenPreferences.getDeviceId().orEmpty(),
            cursor = syncPreferences.getCursor() ?: 0L
        )
        socket?.send(gson.toJson(frame))
    }

    private suspend fun onFrame(text: String) {
        val frame = try {
            gson.fromJson(text, ServerFrameDto::class.java)
        } catch (e: JsonParseException) {
            Timber.w(e, "Realtime: malformed frame")
            return
        } ?: return

        when (frame.type) {
            "auth_ok" -> {
                socketState.value = ConnectionState.Connected
                attempt = 0
                unauthorizedStreak = 0
                scheduleTokenRotation()
                // Everything up to the server's updateSeq is only available via REST.
                val serverSeq = frame.updateSeq ?: 0L
                if (serverSeq > (syncPreferences.getCursor() ?: 0L)) catchUp()
            }
            "update" -> onLiveUpdate(UpdateEnvelopeDto(frame.updateSeq, frame.kind, frame.payload))
            "ack" -> {
                val id = frame.clientMessageId ?: return
                val serverId = frame.serverId ?: return
                val serverSeq = frame.serverSeq ?: return
                val createdAt = frame.serverCreatedAt ?: return
                pendingAcks[id]?.complete(SendResult.Acked(SentMessage(serverId, serverSeq, createdAt)))
            }
            "nack" -> {
                val id = frame.clientMessageId ?: return
                pendingAcks[id]?.complete(SendResult.Nacked(frame.code, frame.message, frame.retryable ?: false))
            }
            "typing" -> {
                val chatId = frame.chatId ?: return
                val userId = frame.userId ?: return
                _events.emit(RealtimeEvent.Typing(chatId, userId))
            }
            "presence" -> {
                val userId = frame.userId ?: return
                _events.emit(RealtimeEvent.Presence(userId, frame.online ?: false, frame.lastSeenAt))
            }
            "error" -> Timber.w("Realtime: server error ${frame.code}: ${frame.message}")
            else -> Timber.d("Realtime: ignoring frame type ${frame.type}")
        }
    }

    private suspend fun onClosed(code: Int?, reason: String?) {
        Timber.i("Realtime: closed code=$code reason=$reason")
        socket = null
        rotateJob?.cancel()
        socketState.value = ConnectionState.Disconnected
        failPendingAcks()
        if (!wanted) return

        when (code) {
            TOKEN_EXPIRED -> refreshThenReconnect()
            UNAUTHORIZED -> {
                // Revoked device or an invalid token: one refresh attempt, then give up.
                if (++unauthorizedStreak > 1) endSession() else refreshThenReconnect()
            }
            SESSION_REPLACED -> wanted = false // another socket of this device took over
            AUTH_TIMEOUT -> connect()
            else -> scheduleReconnect()
        }
    }

    private fun refreshThenReconnect() {
        val stale = tokenInUse
        reconnectJob = scope.launch {
            val fresh = tokenRefresher.refresh(staleToken = stale)
            when {
                fresh != null -> commands.send(Command.Reconnect)
                // The repository clears the session only when the server rejected the refresh.
                !tokenPreferences.hasSession() -> endSession()
                else -> {
                    delay(backoffDelay())
                    commands.send(Command.Reconnect)
                }
            }
        }
    }

    private suspend fun endSession() {
        wanted = false
        _events.emit(RealtimeEvent.SessionEnded)
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        if (!wanted) return
        val wait = backoffDelay()
        Timber.d("Realtime: reconnecting in ${wait}ms")
        reconnectJob = scope.launch {
            delay(wait)
            commands.send(Command.Reconnect)
        }
    }

    /** 1 s, 2 s, 4 s … up to 30 s, with jitter so many clients don't reconnect in lockstep. */
    private fun backoffDelay(): Long {
        val base = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS shl min(attempt, 5))
        attempt++
        return (base * Random.nextDouble(0.5, 1.0)).toLong()
    }

    /**
     * The server closes the socket when its access token expires. Rotating shortly before that
     * and reconnecting avoids the gap (the docs recommend it).
     */
    private fun scheduleTokenRotation() {
        rotateJob?.cancel()
        val token = tokenInUse ?: return
        val expiresAt = jwtExpiryMillis(token) ?: return
        val wait = expiresAt - System.currentTimeMillis() - TOKEN_ROTATION_MARGIN_MS
        if (wait <= 0) return // too close: the 4001 path handles it
        rotateJob = scope.launch {
            delay(wait)
            if (tokenRefresher.refresh(staleToken = token) != null) commands.send(Command.Rotate)
        }
    }

    private fun jwtExpiryMillis(token: String): Long? = try {
        val payload = token.split('.').getOrNull(1) ?: return null
        val json = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
        gson.fromJson(json, JsonObject::class.java)?.get("exp")?.asLong?.times(1000)
    } catch (e: Exception) {
        null
    }

    private fun failPendingAcks() {
        // Null means "no answer": senders fall back to REST with the same clientMessageId.
        pendingAcks.values.forEach { it.complete(null) }
        pendingAcks.clear()
    }

    // ---- update stream ----

    private suspend fun bootstrapCursor(): Boolean = try {
        val response = syncApi.getState()
        val seq = response.body()?.updateSeq
        if (response.isSuccessful && seq != null) {
            syncPreferences.setCursor(seq)
            true
        } else {
            false
        }
    } catch (e: IOException) {
        Timber.w(e, "Realtime: cursor bootstrap failed")
        false
    }

    private suspend fun onLiveUpdate(update: UpdateEnvelopeDto) {
        val seq = update.updateSeq ?: return
        val cursor = syncPreferences.getCursor() ?: 0L
        when {
            seq <= cursor -> Unit // duplicate or already caught up
            seq == cursor + 1 -> {
                apply(update)
                syncPreferences.setCursor(seq)
            }
            // Out of order or something was lost: replay the exact missing range from REST.
            else -> catchUp()
        }
    }

    private suspend fun catchUp() {
        var cursor = syncPreferences.getCursor() ?: return
        repeat(MAX_CATCH_UP_PAGES) {
            val page = try {
                syncApi.getUpdates(since = cursor, limit = CATCH_UP_PAGE_SIZE).takeIf { it.isSuccessful }?.body()
            } catch (e: IOException) {
                null
            }
            if (page == null) {
                // E.g. a transient 500: try again shortly instead of waiting for the next frame.
                Timber.w("Realtime: catch-up failed, retrying")
                scope.launch {
                    delay(CATCH_UP_RETRY_MS)
                    commands.send(Command.CatchUp)
                }
                return
            }
            if (page.tooLong == true) {
                page.state?.updateSeq?.let(syncPreferences::setCursor)
                _events.emit(RealtimeEvent.Resynced)
                return
            }
            val updates = page.updates.orEmpty()
                .filter { (it.updateSeq ?: 0L) > cursor }
                .sortedBy { it.updateSeq }
            for (update in updates) {
                apply(update)
                cursor = update.updateSeq ?: cursor
                syncPreferences.setCursor(cursor)
            }
            val serverTop = page.state?.updateSeq ?: cursor
            if (updates.isEmpty() || cursor >= serverTop) return
        }
    }

    private suspend fun apply(update: UpdateEnvelopeDto) {
        val payload = update.payload ?: return
        val event: RealtimeEvent? = try {
            when (update.kind) {
                "message_new" -> {
                    val dto = gson.fromJson(payload, MessageDto::class.java)
                    val chatId = dto.chatId
                    val message = dto.toDomain(gson)
                    if (chatId != null && message != null) RealtimeEvent.MessageNew(chatId, message) else null
                }
                "message_edit" -> gson.fromJson(payload, MessageEditPayloadDto::class.java).let {
                    RealtimeEvent.MessageEdited(
                        chatId = it.chatId ?: return,
                        serverId = it.serverId ?: return,
                        body = it.body.orEmpty(),
                        editedAt = it.editedAt ?: 0L
                    )
                }
                "message_delete" -> gson.fromJson(payload, MessageDeletePayloadDto::class.java).let {
                    RealtimeEvent.MessageDeleted(chatId = it.chatId ?: return, serverId = it.serverId ?: return)
                }
                "read" -> gson.fromJson(payload, CursorPayloadDto::class.java).let {
                    val chatId = it.chatId ?: return
                    val userId = it.userId ?: return
                    val upToSeq = it.upToSeq ?: return
                    readCursorsByChat.update { all ->
                        val chat = all[chatId].orEmpty()
                        all + (chatId to (chat + (userId to maxOf(chat[userId] ?: 0L, upToSeq))))
                    }
                    RealtimeEvent.ReadReceipt(chatId, userId, upToSeq)
                }
                "delivered" -> gson.fromJson(payload, CursorPayloadDto::class.java).let {
                    RealtimeEvent.Delivered(
                        chatId = it.chatId ?: return,
                        userId = it.userId ?: return,
                        upToSeq = it.upToSeq ?: return
                    )
                }
                "member" -> gson.fromJson(payload, MemberPayloadDto::class.java).let {
                    RealtimeEvent.MemberChanged(
                        chatId = it.chatId ?: return,
                        userId = it.userId ?: return,
                        removed = it.removed ?: false
                    )
                }
                "chat" -> gson.fromJson(payload, ChatPayloadDto::class.java).let {
                    RealtimeEvent.ChatChanged(it.chatId ?: return)
                }
                else -> null
            }
        } catch (e: JsonParseException) {
            Timber.w(e, "Realtime: bad ${update.kind} payload")
            null
        }
        event?.let { _events.emit(it) }
    }

    private inner class Listener(private val generation: Int) : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            commands.trySend(Command.Opened(generation))
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            commands.trySend(Command.Text(generation, text))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            commands.trySend(Command.Closed(generation, code, reason))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Timber.w(t, "Realtime: socket failure")
            commands.trySend(Command.Closed(generation, code = null, reason = t.message))
        }
    }

    private companion object {
        const val NORMAL_CLOSURE = 1000
        const val TOKEN_EXPIRED = 4001
        const val UNAUTHORIZED = 4003
        const val AUTH_TIMEOUT = 4008
        const val SESSION_REPLACED = 4009

        const val ACK_TIMEOUT_MS = 8_000L
        const val BASE_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
        const val TOKEN_ROTATION_MARGIN_MS = 60_000L
        const val CATCH_UP_PAGE_SIZE = 500
        const val MAX_CATCH_UP_PAGES = 40
        const val CATCH_UP_RETRY_MS = 3_000L
        const val EVENT_BUFFER = 256
    }
}
