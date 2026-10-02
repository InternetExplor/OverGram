package com.example.overgram.data.repository

import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.remote.api.ChatApi
import com.example.overgram.data.remote.dto.AddMembersRequestDto
import com.example.overgram.data.remote.dto.ChatDto
import com.example.overgram.data.remote.dto.ChatSettingsRequestDto
import com.example.overgram.data.remote.dto.CreateGroupRequestDto
import com.example.overgram.data.remote.dto.DirectChatRequestDto
import com.example.overgram.data.remote.dto.ErrorDto
import com.example.overgram.data.remote.dto.MessageDto
import com.example.overgram.data.remote.dto.MessagePreviewDto
import com.example.overgram.data.remote.dto.SendMessageRequestDto
import com.example.overgram.data.remote.dto.SeqCursorDto
import com.example.overgram.data.remote.dto.SystemBodyDto
import com.example.overgram.data.remote.dto.UpdateChatRequestDto
import com.example.overgram.data.remote.dto.UpdateMeRequestDto
import com.example.overgram.data.remote.dto.UserPublicDto
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatMember
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.GroupMembers
import com.example.overgram.domain.model.MemberRole
import com.example.overgram.domain.model.Message
import com.example.overgram.domain.model.MessagePage
import com.example.overgram.domain.model.MessagePreview
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SentMessage
import com.example.overgram.domain.model.SystemEvent
import com.example.overgram.domain.model.SystemEventKind
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.repository.ChatRepository
import com.google.gson.Gson
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import retrofit2.Response
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val tokenPreferences: TokenPreferences,
    private val gson: Gson
) : ChatRepository {

    /** Last known profile per user, used when a refresh of that profile fails. */
    private val profileCache = ConcurrentHashMap<String, UserProfile>()

    override suspend fun getChats(): AuthOutcome<List<ChatSummary>> {
        val chats = mutableListOf<ChatDto>()
        var cursor: String? = null
        var pages = 0
        do {
            when (val page = call { api.listChats(PAGE_SIZE, cursor) }) {
                is AuthOutcome.Failure -> return page
                is AuthOutcome.Success -> {
                    chats += page.value?.chats.orEmpty()
                    cursor = page.value?.nextCursor
                }
            }
        } while (cursor != null && ++pages < MAX_PAGES)

        // Peers are refreshed every time for their presence; group senders only need a name.
        val peerIds = chats.mapNotNull { it.peerUserId }.distinct()
        val senderIds = chats.filter { it.type == ChatType.GROUP.name }
            .mapNotNull { it.lastMessage?.senderId }
            .distinct()
        val profiles = loadProfiles(peerIds, refresh = true) + loadProfiles(senderIds, refresh = false)
        return AuthOutcome.Success(chats.mapNotNull { it.toDomain(profiles) })
    }

    override suspend fun getUser(userId: String): AuthOutcome<UserProfile> =
        call { api.getUser(userId) }.mapNotNull { it?.toDomain() }
            .also { if (it is AuthOutcome.Success) profileCache[userId] = it.value }

    override suspend fun getUsers(userIds: Collection<String>): Map<String, UserProfile> =
        loadProfiles(userIds.distinct(), refresh = false)

    override suspend fun getChat(chatId: String): AuthOutcome<ChatSummary> =
        call { api.getChat(chatId) }.resolveChat()

    override suspend fun createGroup(title: String, memberIds: List<String>): AuthOutcome<String> =
        call { api.createGroup(CreateGroupRequestDto(title, memberIds)) }.mapNotNull { it?.id }

    override suspend fun renameGroup(chatId: String, title: String): AuthOutcome<ChatSummary> =
        call { api.updateChat(chatId, UpdateChatRequestDto(title)) }.resolveChat()

    override suspend fun addMembers(chatId: String, userIds: List<String>): AuthOutcome<List<ChatMember>> =
        call { api.addMembers(chatId, AddMembersRequestDto(userIds)) }.mapNotNull { result ->
            result?.members.orEmpty().mapNotNull { member ->
                ChatMember(
                    userId = member.userId ?: return@mapNotNull null,
                    role = MemberRole.entries.firstOrNull { it.name == member.role } ?: MemberRole.MEMBER,
                    isOnline = member.online ?: false
                )
            }
        }

    override suspend fun leaveChat(chatId: String): AuthOutcome<Unit> =
        call { api.leaveChat(chatId) }.mapNotNull { }

    override suspend fun removeMember(chatId: String, userId: String): AuthOutcome<Unit> =
        call { api.removeMember(chatId, userId) }.mapNotNull { }

    override suspend fun getGroupMembers(chatId: String): AuthOutcome<GroupMembers> {
        // Read history backwards until the group_created event (the very first message).
        val history = mutableListOf<Message>()
        var beforeSeq: Long? = null
        var reachedStart = false
        for (page in 0 until MEMBER_HISTORY_MAX_PAGES) {
            val result = when (val outcome = getMessagesPage(chatId, beforeSeq, MAX_MESSAGE_PAGE_SIZE)) {
                is AuthOutcome.Failure -> return outcome
                is AuthOutcome.Success -> outcome.value
            }
            history += result.messages
            if (!result.hasMore || result.messages.isEmpty() ||
                result.messages.any { it.systemEvent?.kind == SystemEventKind.GROUP_CREATED }
            ) {
                reachedStart = true
                break
            }
            beforeSeq = result.messages.minOf { it.serverSeq }
        }

        val members = LinkedHashSet<String>()
        var ownerId: String? = null
        history.sortedBy { it.serverSeq }.forEach { message ->
            val event = message.systemEvent
            when (event?.kind) {
                SystemEventKind.GROUP_CREATED -> {
                    members += event.actorId
                    members += event.targetUserIds
                    ownerId = event.actorId
                }
                SystemEventKind.MEMBERS_ADDED -> members += event.targetUserIds
                SystemEventKind.MEMBER_REMOVED -> members -= event.targetUserIds.toSet()
                SystemEventKind.MEMBER_LEFT -> members -= event.targetUserIds.ifEmpty { listOf(event.actorId) }.toSet()
                SystemEventKind.OWNER_CHANGED -> event.targetUserIds.firstOrNull()?.let {
                    ownerId = it
                    members += it
                }
                // Without the start of history, anyone who spoke is (or was) a member.
                else -> if (!reachedStart && message.type != MessageType.SYSTEM) members += message.senderId
            }
        }

        val profiles = loadProfiles(members.toList(), refresh = true)
        return AuthOutcome.Success(
            GroupMembers(
                members = members.mapNotNull(profiles::get),
                unknownMemberIds = members.filterNot(profiles::containsKey),
                ownerId = ownerId?.takeIf { it in members },
                isComplete = reachedStart
            )
        )
    }

    override suspend fun setMuted(chatId: String, muted: Boolean): AuthOutcome<ChatSummary> =
        call { api.updateSettings(chatId, ChatSettingsRequestDto(muted)) }.resolveChat()

    override suspend fun getMessages(chatId: String, beforeSeq: Long?): AuthOutcome<MessagePage> =
        getMessagesPage(chatId, beforeSeq, MESSAGE_PAGE_SIZE)

    private suspend fun getMessagesPage(chatId: String, beforeSeq: Long?, limit: Int): AuthOutcome<MessagePage> =
        call { api.listMessages(chatId, beforeSeq, limit) }.mapNotNull { page ->
            page ?: return@mapNotNull null
            MessagePage(
                messages = page.messages.orEmpty().mapNotNull { it.toDomain() },
                hasMore = page.hasMore ?: false
            )
        }

    override suspend fun sendText(
        chatId: String,
        clientMessageId: String,
        text: String
    ): AuthOutcome<SentMessage> =
        call {
            api.sendMessage(chatId, SendMessageRequestDto(clientMessageId, MessageType.TEXT.name, text))
        }.mapNotNull { result ->
            SentMessage(
                serverId = result?.serverId ?: return@mapNotNull null,
                serverSeq = result.serverSeq ?: return@mapNotNull null,
                createdAt = result.serverCreatedAt ?: return@mapNotNull null
            )
        }

    override suspend fun markRead(chatId: String, upToSeq: Long): AuthOutcome<Unit> =
        call { api.markRead(chatId, SeqCursorDto(upToSeq)) }.mapNotNull { }

    override suspend fun searchUsers(query: String): AuthOutcome<List<UserProfile>> =
        call { api.searchUsers(query, SEARCH_LIMIT) }.mapNotNull { result ->
            result?.users.orEmpty().mapNotNull { it.toDomain() }
                .onEach { profileCache[it.id] = it }
        }

    override suspend fun openDirectChat(peerUserId: String): AuthOutcome<String> =
        call { api.getOrCreateDirectChat(DirectChatRequestDto(peerUserId)) }.mapNotNull { it?.id }

    override suspend fun getMe(): AuthOutcome<UserProfile> =
        call { api.getMe() }.mapNotNull { it?.toDomain() }

    override suspend fun setUsername(username: String): AuthOutcome<UserProfile> =
        call { api.updateMe(UpdateMeRequestDto(username = username)) }.mapNotNull { it?.toDomain() }

    override fun currentUserId(): String? = tokenPreferences.getUserId()

    /** Maps a success value; a null result means the body was malformed. */
    private inline fun <T, R : Any> AuthOutcome<T>.mapNotNull(transform: (T) -> R?): AuthOutcome<R> =
        when (this) {
            is AuthOutcome.Success -> transform(value)?.let { AuthOutcome.Success(it) }
                ?: AuthOutcome.Failure(AuthError.Unknown("Malformed response")).also {
                    Timber.e("Malformed chat response")
                }
            is AuthOutcome.Failure -> this
        }

    /** A single-chat response, with the DIRECT peer's profile resolved. */
    private suspend fun AuthOutcome<ChatDto?>.resolveChat(): AuthOutcome<ChatSummary> {
        val dto = (this as? AuthOutcome.Success)?.value
        val profiles = dto?.peerUserId?.let { loadProfiles(listOf(it), refresh = true) }.orEmpty()
        return mapNotNull { it?.toDomain(profiles) }
    }

    /**
     * Relay has no batch profile endpoint, so profiles are fetched one by one (a few at a time
     * to stay well inside the 300 req/min limit). A failed fetch falls back to the cache.
     * With [refresh] = false, cached profiles are used as is and only unknown users are fetched.
     */
    private suspend fun loadProfiles(userIds: List<String>, refresh: Boolean): Map<String, UserProfile> {
        val toFetch = if (refresh) userIds else userIds.filterNot(profileCache::containsKey)
        val semaphore = Semaphore(PROFILE_CONCURRENCY)
        coroutineScope {
            toFetch.map { id ->
                async {
                    semaphore.withPermit {
                        val outcome = call { api.getUser(id) }
                        val profile = (outcome as? AuthOutcome.Success)?.value?.toDomain()
                        if (profile != null) profileCache[id] = profile
                    }
                }
            }.awaitAll()
        }
        return userIds.mapNotNull { id -> profileCache[id]?.let { id to it } }.toMap()
    }

    private suspend fun <T> call(block: suspend () -> Response<T>): AuthOutcome<T?> =
        try {
            val response = block()
            if (response.isSuccessful) {
                AuthOutcome.Success(response.body())
            } else {
                AuthOutcome.Failure(response.toError())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Chat request failed: network")
            AuthOutcome.Failure(AuthError.Network)
        } catch (e: Exception) {
            Timber.e(e, "Chat request failed")
            AuthOutcome.Failure(AuthError.Unknown(e.message))
        }

    private fun Response<*>.toError(): AuthError {
        val error = parseError()
        return when {
            // TokenAuthenticator already tried to refresh; no session left means it was revoked.
            code() == 401 && !tokenPreferences.hasSession() -> AuthError.SessionExpired
            error?.code == "RATE_LIMITED" || code() == 429 ->
                AuthError.RateLimited(headers()["Retry-After"]?.trim()?.toIntOrNull())
            code() in 500..599 -> AuthError.ServiceUnavailable
            // VALIDATION_ERROR, USERNAME_TAKEN, FORBIDDEN ("You must be an admin…"), NOT_FOUND:
            // the server's message is user-readable.
            code() in setOf(400, 403, 404, 409) -> AuthError.Validation(error?.message)
            else -> AuthError.Unknown(error?.message)
        }
    }

    private fun Response<*>.parseError(): ErrorDto? = try {
        errorBody()?.charStream()?.use { gson.fromJson(it, ErrorDto::class.java) }
    } catch (e: JsonParseException) {
        null
    } catch (e: IOException) {
        null
    }

    private fun ChatDto.toDomain(profiles: Map<String, UserProfile>): ChatSummary? {
        val id = id ?: return null.also { Timber.w("Skipping chat without id") }
        val chatType = if (type == "GROUP") ChatType.GROUP else ChatType.DIRECT
        val preview = lastMessage?.toDomain()
        return ChatSummary(
            id = id,
            type = chatType,
            title = title,
            peer = peerUserId?.let(profiles::get),
            lastMessage = preview,
            lastMessageSender = preview?.senderId?.takeIf { chatType == ChatType.GROUP }?.let(profiles::get),
            lastActivityAt = lastActivityAt ?: 0L,
            unreadCount = unreadCount ?: 0,
            isMuted = muted ?: false
        )
    }

    private fun MessagePreviewDto.toDomain(): MessagePreview? {
        val messageType = MessageType.entries.firstOrNull { it.name == type } ?: MessageType.UNKNOWN
        return MessagePreview(
            senderId = senderId ?: return null,
            type = messageType,
            body = body,
            createdAt = createdAt ?: return null,
            isDeleted = deletedAt != null,
            systemEvent = if (messageType == MessageType.SYSTEM) parseSystemEvent(body) else null
        )
    }

    private fun MessageDto.toDomain(): Message? {
        val messageType = MessageType.entries.firstOrNull { it.name == type } ?: MessageType.UNKNOWN
        return Message(
            clientMessageId = clientMessageId ?: return null,
            serverId = serverId ?: return null,
            serverSeq = serverSeq ?: return null,
            senderId = senderId ?: return null,
            type = messageType,
            body = body,
            createdAt = createdAt ?: return null,
            isEdited = editedAt != null,
            isDeleted = deletedAt != null,
            systemEvent = if (messageType == MessageType.SYSTEM) parseSystemEvent(body) else null
        )
    }

    /** SYSTEM bodies are a JSON *string*; anything unparseable is simply not rendered as an event. */
    private fun parseSystemEvent(body: String?): SystemEvent? {
        val dto = try {
            body?.let { gson.fromJson(it, SystemBodyDto::class.java) }
        } catch (e: JsonParseException) {
            Timber.w(e, "Unparseable SYSTEM body")
            null
        } ?: return null
        return SystemEvent(
            kind = SystemEventKind.entries.firstOrNull { it.name.equals(dto.event, ignoreCase = true) }
                ?: SystemEventKind.UNKNOWN,
            actorId = dto.actorId ?: return null,
            targetUserIds = dto.targetUserIds.orEmpty(),
            title = dto.title
        )
    }

    private fun UserPublicDto.toDomain(): UserProfile? {
        return UserProfile(
            id = id ?: return null,
            displayName = displayName ?: username ?: return null,
            username = username,
            avatarMediaId = avatarMediaId,
            isOnline = online ?: false,
            lastSeenAt = lastSeenAt
        )
    }

    private companion object {
        /** Server maximum for `GET /v1/chats`. */
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 5
        const val PROFILE_CONCURRENCY = 6
        const val MESSAGE_PAGE_SIZE = 50

        /** Server maximum for `GET /v1/chats/{id}/messages`. */
        const val MAX_MESSAGE_PAGE_SIZE = 100

        /** Member lists read at most this many pages (5000 messages) back. */
        const val MEMBER_HISTORY_MAX_PAGES = 50
        const val SEARCH_LIMIT = 20
    }
}
