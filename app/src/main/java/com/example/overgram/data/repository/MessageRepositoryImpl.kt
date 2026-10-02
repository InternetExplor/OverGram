package com.example.overgram.data.repository

import com.example.overgram.data.local.db.ChatDao
import com.example.overgram.data.local.db.MessageDao
import com.example.overgram.data.local.db.MessageEntity
import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.mapper.toDomain
import com.example.overgram.data.mapper.toEntity
import com.example.overgram.data.realtime.RealtimeClient
import com.example.overgram.data.realtime.SendResult
import com.example.overgram.data.remote.ApiCaller
import com.example.overgram.data.remote.api.ChatApi
import com.example.overgram.data.remote.dto.MessageDto
import com.example.overgram.data.remote.dto.SendMessageRequestDto
import com.example.overgram.data.remote.dto.SeqCursorDto
import com.example.overgram.data.remote.mapNotNull
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SendState
import com.example.overgram.domain.model.SentMessage
import com.example.overgram.domain.model.StoredMessage
import com.example.overgram.domain.repository.MessageRepository
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val apiCaller: ApiCaller,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val realtime: RealtimeClient,
    private val tokenPreferences: TokenPreferences,
    private val gson: Gson
) : MessageRepository {

    /** Outgoing sends outlive the screen that started them. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** One flush at a time keeps the outbox in order and never sends a message twice in parallel. */
    private val outboxLock = Mutex()

    override fun observeMessages(chatId: String): Flow<List<StoredMessage>> =
        messageDao.observeChat(chatId).map { rows -> rows.map { it.toDomain(gson) } }

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

    private fun MessageDto.toEntityOrNull(chatId: String): MessageEntity? = toDomain(gson)?.toEntity(chatId)

    override suspend fun sendText(chatId: String, text: String): String {
        val entity = MessageEntity(
            clientMessageId = UUID.randomUUID().toString(),
            chatId = chatId,
            serverId = null,
            serverSeq = null,
            senderId = tokenPreferences.getUserId().orEmpty(),
            type = MessageType.TEXT.name,
            body = text,
            createdAt = System.currentTimeMillis(),
            isEdited = false,
            isDeleted = false,
            sendState = SendState.SENDING.name
        )
        messageDao.upsert(entity)
        scope.launch { flushOutbox() }
        return entity.clientMessageId
    }

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
                    // Keep it (and everything after it, to preserve order) queued for the next flush.
                    Delivery.NoConnection -> return
                }
            }
        }
    }

    private sealed interface Delivery {
        data class Sent(val sent: SentMessage) : Delivery
        data class Refused(val reason: String?) : Delivery
        data object NoConnection : Delivery
    }

    /**
     * The socket is the fast path; with no answer, REST with the same clientMessageId is safe
     * because the server deduplicates it (and returns the original result).
     */
    private suspend fun deliver(message: MessageEntity): Delivery {
        val text = message.body.orEmpty()
        when (val result = realtime.sendMessage(message.chatId, message.clientMessageId, text)) {
            is SendResult.Acked -> return Delivery.Sent(result.sent)
            is SendResult.Nacked -> if (!result.retryable) return Delivery.Refused(result.message)
            null -> Unit
        }
        val outcome = apiCaller.call {
            api.sendMessage(message.chatId, SendMessageRequestDto(message.clientMessageId, MessageType.TEXT.name, text))
        }.mapNotNull { body ->
            SentMessage(
                serverId = body?.serverId ?: return@mapNotNull null,
                serverSeq = body.serverSeq ?: return@mapNotNull null,
                createdAt = body.serverCreatedAt ?: return@mapNotNull null
            )
        }
        return when (outcome) {
            is AuthOutcome.Success -> Delivery.Sent(outcome.value)
            is AuthOutcome.Failure -> when (val error = outcome.error) {
                // The server said no (not a member, invalid…): retrying won't help.
                is AuthError.Validation -> Delivery.Refused(error.message)
                // Network, 5xx, rate limit, session hiccup: try again later.
                else -> Delivery.NoConnection
            }
        }
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
    }
}
