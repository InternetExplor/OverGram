package com.example.overgram.data.repository

import com.example.overgram.data.local.prefs.TokenPreferences
import com.example.overgram.data.remote.api.ChatApi
import com.example.overgram.data.remote.dto.ChatDto
import com.example.overgram.data.remote.dto.ErrorDto
import com.example.overgram.data.remote.dto.MessagePreviewDto
import com.example.overgram.data.remote.dto.UserPublicDto
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.ChatSummary
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.MessagePreview
import com.example.overgram.domain.model.MessageType
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

        val peerIds = chats.mapNotNull { it.peerUserId }.distinct()
        val profiles = loadProfiles(peerIds)
        return AuthOutcome.Success(chats.mapNotNull { it.toDomain(profiles) })
    }

    override fun currentUserId(): String? = tokenPreferences.getUserId()

    /**
     * Relay has no batch profile endpoint, so peers are fetched one by one (a few at a time
     * to stay well inside the 300 req/min limit). A failed fetch falls back to the cache.
     */
    private suspend fun loadProfiles(userIds: List<String>): Map<String, UserProfile> {
        val semaphore = Semaphore(PROFILE_CONCURRENCY)
        coroutineScope {
            userIds.map { id ->
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
        return ChatSummary(
            id = id,
            type = chatType,
            title = title,
            peer = peerUserId?.let(profiles::get),
            lastMessage = lastMessage?.toDomain(),
            lastActivityAt = lastActivityAt ?: 0L,
            unreadCount = unreadCount ?: 0,
            isMuted = muted ?: false
        )
    }

    private fun MessagePreviewDto.toDomain(): MessagePreview? {
        return MessagePreview(
            senderId = senderId ?: return null,
            type = MessageType.entries.firstOrNull { it.name == type } ?: MessageType.UNKNOWN,
            body = body,
            createdAt = createdAt ?: return null,
            isDeleted = deletedAt != null
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
    }
}
