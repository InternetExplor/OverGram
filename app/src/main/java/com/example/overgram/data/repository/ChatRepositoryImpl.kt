package com.example.overgram.data.repository

import com.example.overgram.data.local.db.ChatDao
import com.example.overgram.data.local.db.UserDao
import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.mapper.toDomain
import com.example.overgram.data.mapper.toEntity
import com.example.overgram.data.remote.ApiCaller
import com.example.overgram.data.remote.api.ChatApi
import com.example.overgram.data.remote.dto.AddMembersRequestDto
import com.example.overgram.data.remote.dto.ChatDto
import com.example.overgram.data.remote.dto.ChatSettingsRequestDto
import com.example.overgram.data.remote.dto.CreateGroupRequestDto
import com.example.overgram.data.remote.dto.DirectChatRequestDto
import com.example.overgram.data.remote.dto.UpdateChatRequestDto
import com.example.overgram.data.remote.mapNotNull
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatMember
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.GroupMembers
import com.example.overgram.domain.model.MemberRole
import com.example.overgram.domain.model.Message
import com.example.overgram.domain.model.MessageType
import com.example.overgram.domain.model.SystemEventKind
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.domain.repository.ChatRepository
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val api: ChatApi,
    private val apiCaller: ApiCaller,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val tokenPreferences: TokenPreferences,
    private val gson: Gson
) : ChatRepository {

    /** Profiles known this session; also persisted in the users table for offline names. */
    private val profileCache = ConcurrentHashMap<String, UserProfile>()

    override fun observeChats(): Flow<List<ChatSummary>> =
        combine(chatDao.observeAll(), userDao.observeAll()) { chats, users ->
            val profiles = users.associate { it.id to it.toDomain() }
            chats.map { it.toDomain(profiles, gson) }
        }.distinctUntilChanged()

    override suspend fun refreshChats(): AuthOutcome<Unit> {
        val chats = mutableListOf<ChatDto>()
        var cursor: String? = null
        var pages = 0
        do {
            when (val page = apiCaller.call { api.listChats(PAGE_SIZE, cursor) }) {
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
        loadProfiles(peerIds, refresh = true)
        loadProfiles(senderIds, refresh = false)
        chatDao.replaceAll(chats.mapNotNull { it.toEntity() })
        return AuthOutcome.Success(Unit)
    }

    override suspend fun getUser(userId: String): AuthOutcome<UserProfile> =
        apiCaller.call { api.getUser(userId) }.mapNotNull { it?.toDomain() }
            .also { if (it is AuthOutcome.Success) remember(listOf(it.value)) }

    override suspend fun getUsers(userIds: Collection<String>): Map<String, UserProfile> =
        loadProfiles(userIds.distinct(), refresh = false)

    override suspend fun getChat(chatId: String): AuthOutcome<ChatSummary> =
        apiCaller.call { api.getChat(chatId) }.resolveChat()

    override suspend fun createGroup(title: String, memberIds: List<String>): AuthOutcome<String> =
        apiCaller.call { api.createGroup(CreateGroupRequestDto(title, memberIds)) }.mapNotNull { it?.id }

    override suspend fun renameGroup(chatId: String, title: String): AuthOutcome<ChatSummary> =
        apiCaller.call { api.updateChat(chatId, UpdateChatRequestDto(title)) }.resolveChat()

    override suspend fun addMembers(chatId: String, userIds: List<String>): AuthOutcome<List<ChatMember>> =
        apiCaller.call { api.addMembers(chatId, AddMembersRequestDto(userIds)) }.mapNotNull { result ->
            result?.members.orEmpty().mapNotNull { member ->
                ChatMember(
                    userId = member.userId ?: return@mapNotNull null,
                    role = MemberRole.entries.firstOrNull { it.name == member.role } ?: MemberRole.MEMBER,
                    isOnline = member.online ?: false
                )
            }
        }

    override suspend fun leaveChat(chatId: String): AuthOutcome<Unit> =
        apiCaller.call { api.leaveChat(chatId) }.mapNotNull { }
            .also { if (it is AuthOutcome.Success) chatDao.delete(chatId) }

    override suspend fun removeMember(chatId: String, userId: String): AuthOutcome<Unit> =
        apiCaller.call { api.removeMember(chatId, userId) }.mapNotNull { }

    override suspend fun getGroupMembers(chatId: String): AuthOutcome<GroupMembers> {
        // Read history backwards until the group_created event (the very first message).
        val history = mutableListOf<Message>()
        var beforeSeq: Long? = null
        var reachedStart = false
        for (attempt in 1..MEMBER_HISTORY_MAX_PAGES) {
            val page = when (
                val outcome = apiCaller.call { api.listMessages(chatId, beforeSeq, MAX_MESSAGE_PAGE_SIZE) }
            ) {
                is AuthOutcome.Failure -> return outcome
                is AuthOutcome.Success -> outcome.value
            }
            val messages = page?.messages.orEmpty().mapNotNull { it.toDomain(gson) }
            history += messages
            if (page?.hasMore != true || messages.isEmpty() ||
                messages.any { it.systemEvent?.kind == SystemEventKind.GROUP_CREATED }
            ) {
                reachedStart = true
                break
            }
            beforeSeq = messages.minOf { it.serverSeq }
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
        apiCaller.call { api.updateSettings(chatId, ChatSettingsRequestDto(muted)) }.resolveChat()

    override suspend fun searchUsers(query: String): AuthOutcome<List<UserProfile>> =
        apiCaller.call { api.searchUsers(query, SEARCH_LIMIT) }.mapNotNull { result ->
            result?.users.orEmpty().mapNotNull { it.toDomain() }
        }.also { if (it is AuthOutcome.Success) remember(it.value) }

    override suspend fun openDirectChat(peerUserId: String): AuthOutcome<String> =
        apiCaller.call { api.getOrCreateDirectChat(DirectChatRequestDto(peerUserId)) }.mapNotNull { it?.id }

    override fun currentUserId(): String? = tokenPreferences.getUserId()

    /** A single-chat response, with the DIRECT peer's profile resolved. Also refreshes the stored row. */
    private suspend fun AuthOutcome<ChatDto?>.resolveChat(): AuthOutcome<ChatSummary> {
        val dto = (this as? AuthOutcome.Success)?.value
        val profiles = dto?.peerUserId?.let { loadProfiles(listOf(it), refresh = true) }.orEmpty()
        dto?.toEntity()?.let { chatDao.upsert(it) }
        return mapNotNull { it?.toEntity()?.toDomain(profiles, gson) }
    }

    /**
     * Relay has no batch profile endpoint, so profiles are fetched one by one (a few at a time
     * to stay well inside the 300 req/min limit). A failed fetch falls back to what's known.
     * With [refresh] = false, known profiles are used as is and only unknown users are fetched.
     */
    private suspend fun loadProfiles(userIds: List<String>, refresh: Boolean): Map<String, UserProfile> {
        if (userIds.isEmpty()) return emptyMap()
        // Fill the session cache from the database first (names survive app restarts).
        val missingInMemory = userIds.filterNot(profileCache::containsKey)
        if (missingInMemory.isNotEmpty()) {
            userDao.get(missingInMemory).forEach { profileCache[it.id] = it.toDomain() }
        }
        val toFetch = if (refresh) userIds else userIds.filterNot(profileCache::containsKey)
        val fetched = ConcurrentHashMap<String, UserProfile>()
        val semaphore = Semaphore(PROFILE_CONCURRENCY)
        coroutineScope {
            toFetch.map { id ->
                async {
                    semaphore.withPermit {
                        val outcome = apiCaller.call { api.getUser(id) }
                        (outcome as? AuthOutcome.Success)?.value?.toDomain()?.let { fetched[id] = it }
                    }
                }
            }.awaitAll()
        }
        if (fetched.isNotEmpty()) remember(fetched.values)
        return userIds.mapNotNull { id -> profileCache[id]?.let { id to it } }.toMap()
    }

    private suspend fun remember(profiles: Collection<UserProfile>) {
        profiles.forEach { profileCache[it.id] = it }
        try {
            userDao.upsert(profiles.map { it.toEntity() })
        } catch (e: Exception) {
            Timber.w(e, "Couldn't store profiles")
        }
    }

    private companion object {
        /** Server maximum for `GET /v1/chats`. */
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 5
        const val PROFILE_CONCURRENCY = 6

        /** Server maximum for `GET /v1/chats/{id}/messages`. */
        const val MAX_MESSAGE_PAGE_SIZE = 100

        /** Member lists read at most this many pages (5000 messages) back. */
        const val MEMBER_HISTORY_MAX_PAGES = 50
        const val SEARCH_LIMIT = 20
    }
}
