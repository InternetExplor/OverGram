package com.example.overgram.data.repository

import com.example.overgram.data.local.db.ChatDao
import com.example.overgram.data.local.db.MessageDao
import com.example.overgram.data.local.db.MessageEntity
import com.example.overgram.data.local.db.ReceiptDao
import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.mapper.parseMedia
import com.example.overgram.data.mapper.toDomain
import com.example.overgram.data.mapper.toEntity
import com.example.overgram.data.mapper.toMediaJson
import com.example.overgram.data.media.ChunksResult
import com.example.overgram.data.media.FileUploadSource
import com.example.overgram.data.media.LocalMediaStore
import com.example.overgram.data.media.MediaPreparer
import com.example.overgram.data.media.MediaUploader
import com.example.overgram.data.media.PrepareResult
import com.example.overgram.data.realtime.RealtimeClient
import com.example.overgram.data.realtime.SendResult
import com.example.overgram.data.remote.ApiCaller
import com.example.overgram.data.remote.api.ChatApi
import com.example.overgram.data.remote.dto.EditMessageRequestDto
import com.example.overgram.data.remote.dto.MessageDto
import com.example.overgram.data.remote.dto.SendMessageRequestDto
import com.example.overgram.data.remote.dto.SeqCursorDto
import com.example.overgram.data.remote.mapNotNull
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatReceipts
import com.example.overgram.domain.model.ConnectionState
import com.example.overgram.domain.model.MediaAttachment
import com.example.overgram.domain.model.MediaKind
import com.example.overgram.domain.model.MediaSendResult
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SendState
import com.example.overgram.domain.model.SentMessage
import com.example.overgram.domain.model.StoredMessage
import com.example.overgram.domain.repository.MessageRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val apiCaller: ApiCaller,
    private val messageDao: MessageDao,
    private val receiptDao: ReceiptDao,
    private val chatDao: ChatDao,
    private val realtime: RealtimeClient,
    private val tokenPreferences: TokenPreferences,
    private val preparer: MediaPreparer,
    private val uploader: MediaUploader,
    private val mediaStore: LocalMediaStore,
    private val gson: Gson
) : MessageRepository {

    /** Outgoing sends outlive the screen that started them. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One flush at a time keeps the outbox in order and never sends a message twice in parallel. */
    private val outboxLock = Mutex()

    /** A flush retried later after a hiccup while the socket stayed up (nothing else would trigger it). */
    private var retryJob: Job? = null

    private val _uploadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    override val uploadProgress: StateFlow<Map<String, Float>> = _uploadProgress.asStateFlow()

    override fun observeMessages(chatId: String): Flow<List<StoredMessage>> =
        messageDao.observeChat(chatId)
            .map { rows -> rows.map { it.toDomain(gson) } }
            .flowOn(Dispatchers.Default)

    override suspend fun refreshNewest(chatId: String): AuthOutcome<Unit> =
        fetchPage(chatId, beforeSeq = null).mapNotNull { (messages, _) ->
            messageDao.insertNewestPage(chatId, messages)
        }

    override suspend fun loadOlder(chatId: String): AuthOutcome<Boolean> {
        val oldest = messageDao.oldestSeq(chatId) ?: return refreshNewest(chatId).mapNotNull { true }
        if (oldest <= FIRST_SEQ) return AuthOutcome.Success(false)
        return fetchPage(chatId, beforeSeq = oldest).mapNotNull { (messages, hasMore) ->
            messageDao.upsert(messages)
            hasMore
        }
    }

    private suspend fun fetchPage(chatId: String, beforeSeq: Long?): AuthOutcome<Pair<List<MessageEntity>, Boolean>> =
        apiCaller.call { api.listMessages(chatId, beforeSeq, PAGE_SIZE) }.mapNotNull { page ->
            page ?: return@mapNotNull null
            val messages = page.messages.orEmpty().mapNotNull { dto -> dto.toEntityOrNull(chatId) }
            messages to (page.hasMore ?: false)
        }

    private fun MessageDto.toEntityOrNull(chatId: String): MessageEntity? = toDomain(gson)?.toEntity(chatId, gson)

    override suspend fun sendText(chatId: String, text: String, replyTo: String?): String =
        enqueue(chatId, MessageType.TEXT, text, media = null, replyTo = replyTo)

    override suspend fun sendMedia(
        chatId: String,
        uri: String,
        caption: String?,
        asFile: Boolean,
        replyTo: String?
    ): MediaSendResult {
        val prepared = when (val result = preparer.prepare(uri, asFile)) {
            is PrepareResult.Ready -> result.media
            PrepareResult.TooLarge -> return MediaSendResult.TooLarge
            PrepareResult.Unreadable -> return MediaSendResult.Unreadable
        }
        val attachment = MediaAttachment(
            mediaId = null,
            kind = prepared.kind,
            mimeType = prepared.mimeType,
            sizeBytes = prepared.sizeBytes,
            width = prepared.width,
            height = prepared.height,
            durationMs = prepared.durationMs,
            localPath = prepared.file.path
        )
        val type = when (prepared.kind) {
            MediaKind.IMAGE -> MessageType.IMAGE
            MediaKind.VIDEO -> MessageType.VIDEO
            MediaKind.FILE -> MessageType.FILE
        }
        // Relay keeps no file names: a document's body is its name (captions are for photos/videos).
        val body = if (prepared.kind == MediaKind.FILE) prepared.fileName else caption
        return MediaSendResult.Queued(enqueue(chatId, type, body, attachment, replyTo))
    }

    private suspend fun enqueue(
        chatId: String,
        type: MessageType,
        body: String?,
        media: MediaAttachment?,
        replyTo: String?
    ): String {
        val entity = MessageEntity(
            clientMessageId = UUID.randomUUID().toString(),
            chatId = chatId,
            serverId = null,
            serverSeq = null,
            senderId = tokenPreferences.getUserId().orEmpty(),
            type = type.name,
            body = body,
            createdAt = System.currentTimeMillis(),
            isEdited = false,
            isDeleted = false,
            sendState = SendState.SENDING.name,
            media = listOfNotNull(media).toMediaJson(gson),
            replyTo = replyTo
        )
        messageDao.upsert(entity)
        scope.launch { flushOutbox() }
        return entity.clientMessageId
    }

    override suspend fun cancelSending(clientMessageId: String) {
        val message = messageDao.get(clientMessageId) ?: return
        // A running upload notices the row is gone before its next chunk and stops.
        if (messageDao.deleteUnsent(clientMessageId) == 0) return
        parseMedia(gson, message.media).forEach { mediaStore.discard(it.localPath) }
        _uploadProgress.update { it - clientMessageId }
    }

    override suspend fun editMessage(clientMessageId: String, text: String): AuthOutcome<Unit> {
        val serverId = messageDao.get(clientMessageId)?.serverId
            ?: return AuthOutcome.Failure(AuthError.Validation(null))
        return apiCaller.call { api.editMessage(serverId, EditMessageRequestDto(text)) }.mapNotNull { dto ->
            // The server's copy, so the text is exactly what everyone else sees.
            messageDao.applyEdit(serverId, dto?.body ?: text)
        }
    }

    override suspend fun deleteMessage(clientMessageId: String): AuthOutcome<Unit> {
        val message = messageDao.get(clientMessageId) ?: return AuthOutcome.Success(Unit)
        val serverId = message.serverId
        if (serverId == null) {
            cancelSending(clientMessageId)
            return AuthOutcome.Success(Unit)
        }
        return apiCaller.call { api.deleteMessage(serverId) }.mapNotNull {
            messageDao.applyDelete(serverId)
        }
    }

    override suspend fun markDelivered(chatId: String, upToSeq: Long) {
        if (!realtime.sendReceived(chatId, upToSeq)) {
            apiCaller.call { api.markReceived(chatId, SeqCursorDto(upToSeq)) }
        }
    }

    override fun observeReceipts(chatId: String): Flow<ChatReceipts> =
        receiptDao.observe(chatId).map { row ->
            ChatReceipts(deliveredUpTo = row?.deliveredUpTo ?: 0L, readUpTo = row?.readUpTo ?: 0L)
        }.distinctUntilChanged()

    override suspend fun retry(clientMessageId: String) {
        messageDao.setSendState(clientMessageId, SendState.SENDING.name, reason = null)
        scope.launch { flushOutbox() }
    }

    override suspend fun flushOutbox() {
        outboxLock.withLock {
            for (message in messageDao.outbox()) {
                when (val result = deliver(message)) {
                    is Delivery.Sent -> messageDao.markSent(
                        message.clientMessageId,
                        result.sent.serverId,
                        result.sent.serverSeq,
                        result.sent.createdAt
                    )
                    is Delivery.Refused -> messageDao.setSendState(
                        message.clientMessageId,
                        SendState.FAILED.name,
                        result.reason
                    )
                    Delivery.Cancelled -> Unit
                    // Keep it (and everything after it, to preserve order) queued for the next flush.
                    Delivery.NoConnection -> {
                        scheduleRetryIfOnline()
                        return
                    }
                }
            }
        }
    }

    /**
     * A reconnect flushes the outbox by itself; but an upload can fail (a timeout, a 5xx) while the
     * socket stays up, and then nothing would. Try again a little later.
     */
    private fun scheduleRetryIfOnline() {
        if (realtime.connectionState.value != ConnectionState.Connected) return
        if (retryJob?.isActive == true) return
        retryJob = scope.launch {
            delay(RETRY_DELAY_MS)
            flushOutbox()
        }
    }

    private sealed interface Delivery {
        data class Sent(val sent: SentMessage) : Delivery
        data class Refused(val reason: String?) : Delivery
        data object Cancelled : Delivery
        data object NoConnection : Delivery
    }

    /**
     * Uploads the attachment (if any) first, then sends. The socket is the fast path; with no
     * answer, REST with the same clientMessageId is safe because the server deduplicates it (and
     * returns the original result).
     */
    private suspend fun deliver(message: MessageEntity): Delivery {
        val type = MessageType.entries.firstOrNull { it.name == message.type } ?: MessageType.TEXT
        val mediaIds = parseMedia(gson, message.media).map { attachment ->
            when (val upload = ensureUploaded(message, attachment)) {
                is Upload.Ready -> upload.mediaId
                is Upload.Stopped -> return upload.delivery
            }
        }
        val socketResult = realtime.sendMessage(
            message.chatId, message.clientMessageId, type, message.body, mediaIds, message.replyTo
        )
        when (val result = socketResult) {
            is SendResult.Acked -> return Delivery.Sent(result.sent)
            is SendResult.Nacked -> if (!result.retryable) return Delivery.Refused(result.message)
            null -> Unit
        }
        val outcome = apiCaller.call {
            api.sendMessage(
                message.chatId,
                SendMessageRequestDto(message.clientMessageId, type.name, message.body, mediaIds, message.replyTo)
            )
        }.mapNotNull { body ->
            SentMessage(
                serverId = body?.serverId ?: return@mapNotNull null,
                serverSeq = body.serverSeq ?: return@mapNotNull null,
                createdAt = body.serverCreatedAt ?: return@mapNotNull null
            )
        }
        return when (outcome) {
            is AuthOutcome.Success -> Delivery.Sent(outcome.value)
            is AuthOutcome.Failure -> outcome.error.toDelivery()
        }
    }

    private sealed interface Upload {
        data class Ready(val mediaId: String) : Upload
        data class Stopped(val delivery: Delivery) : Upload
    }

    /**
     * Makes sure [attachment] is on the server. The session is saved before the first chunk, so
     * after a crash, a reboot or a lost connection the upload resumes where the server got to.
     */
    private suspend fun ensureUploaded(message: MessageEntity, attachment: MediaAttachment): Upload {
        val id = message.clientMessageId
        val localPath = attachment.localPath
        // Uploaded already (or someone else's media): nothing to do.
        if (localPath == null) {
            return attachment.mediaId?.let { Upload.Ready(it) } ?: Upload.Stopped(Delivery.Refused(null))
        }
        val file = File(localPath)
        if (!file.exists()) return Upload.Stopped(Delivery.Refused(MISSING_FILE))
        val source = FileUploadSource(file)

        var uploadId = message.uploadId
        var mediaId = attachment.mediaId
        repeat(MAX_SESSIONS) {
            val resume = uploadId != null && mediaId != null
            if (!resume) {
                val start = uploader.start(
                    source,
                    attachment.kind.name,
                    attachment.mimeType,
                    attachment.width,
                    attachment.height,
                    attachment.durationMs
                )
                val session = when (start) {
                    is AuthOutcome.Success -> start.value
                    is AuthOutcome.Failure -> return Upload.Stopped(start.error.toDelivery())
                }
                uploadId = session.uploadId
                mediaId = session.mediaId
                messageDao.setUpload(id, uploadId, listOf(attachment.copy(mediaId = mediaId)).toMediaJson(gson))
            }
            val result = uploader.sendChunks(
                source = source,
                uploadId = uploadId!!,
                resume = resume,
                onProgress = { sent ->
                    val fraction = if (source.size > 0) sent.toFloat() / source.size else 1f
                    _uploadProgress.update { it + (id to fraction) }
                },
                isCancelled = { messageDao.get(id) == null }
            )
            when (result) {
                ChunksResult.Done -> {
                    val finalId = mediaId!!
                    // Our copy becomes the media's local file: our own photo never needs downloading.
                    mediaStore.adopt(file, finalId)
                    messageDao.setUpload(id, null, listOf(attachment.copy(mediaId = finalId, localPath = null)).toMediaJson(gson))
                    _uploadProgress.update { it - id }
                    return Upload.Ready(finalId)
                }
                ChunksResult.Cancelled -> return Upload.Stopped(Delivery.Cancelled)
                is ChunksResult.Failed -> return Upload.Stopped(result.error.toDelivery())
                ChunksResult.SessionLost -> {
                    uploadId = null
                    mediaId = null
                    messageDao.setUpload(id, null, listOf(attachment.copy(mediaId = null)).toMediaJson(gson))
                    _uploadProgress.update { it - id }
                }
            }
        }
        return Upload.Stopped(Delivery.Refused(null))
    }

    private fun AuthError.toDelivery(): Delivery = when (this) {
        // The server said no (not a member, too large, invalid…): retrying won't help.
        is AuthError.Validation -> Delivery.Refused(message)
        // Network, 5xx, rate limit, session hiccup: try again later.
        else -> Delivery.NoConnection
    }

    override suspend fun markRead(chatId: String, upToSeq: Long): AuthOutcome<Unit> {
        // Clear the badge locally right away; the next list refresh confirms it.
        chatDao.markRead(chatId)
        return if (realtime.sendRead(chatId, upToSeq)) {
            AuthOutcome.Success(Unit)
        } else {
            apiCaller.call { api.markRead(chatId, SeqCursorDto(upToSeq)) }.mapNotNull { }
        }
    }

    private companion object {
        const val PAGE_SIZE = 50

        /** serverSeq is per chat, gap-free and starts at 1: nothing exists before it. */
        const val FIRST_SEQ = 1L

        const val RETRY_DELAY_MS = 5_000L

        /** A lost session is restarted once; twice in a row means something is wrong with the file. */
        const val MAX_SESSIONS = 2

        const val MISSING_FILE = "File is no longer on the device"
    }
}
