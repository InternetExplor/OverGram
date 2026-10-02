package com.example.overgram.data.repository

import androidx.room.withTransaction
import com.example.overgram.data.local.db.ChatDao
import com.example.overgram.data.local.db.MessageDao
import com.example.overgram.data.local.db.OverGramDatabase
import com.example.overgram.data.local.db.UserDao
import com.example.overgram.data.media.AvatarImageLoader
import com.example.overgram.data.media.EncodedImage
import com.example.overgram.data.remote.ApiCaller
import com.example.overgram.data.remote.api.ProfileApi
import com.example.overgram.data.remote.dto.OffsetMismatchDto
import com.example.overgram.data.remote.dto.StartUploadRequestDto
import com.example.overgram.data.remote.dto.UserMeDto
import com.example.overgram.data.remote.mapNotNull
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.ServerInfo
import com.example.overgram.domain.repository.ProfileRepository
import com.google.gson.Gson
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val apiCaller: ApiCaller,
    private val imageLoader: AvatarImageLoader,
    private val db: OverGramDatabase,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val gson: Gson
) : ProfileRepository {

    private val _me = MutableStateFlow<MyProfile?>(null)
    override val me: StateFlow<MyProfile?> = _me.asStateFlow()

    override suspend fun refresh(): AuthOutcome<MyProfile> =
        apiCaller.call { api.getMe() }.toProfile()

    override suspend fun update(displayName: String?, username: String?): AuthOutcome<MyProfile> {
        val body = JsonObject().apply {
            displayName?.let { addProperty("displayName", it) }
            username?.let { addProperty("username", it) }
        }
        if (body.size() == 0) return _me.value?.let { AuthOutcome.Success(it) } ?: refresh()
        return apiCaller.call { api.updateMe(body) }.toProfile()
    }

    override suspend fun setAvatar(imageUri: String): AuthOutcome<MyProfile> {
        val image = imageLoader.load(imageUri)
            ?: return AuthOutcome.Failure(AuthError.Unknown("Unreadable image"))
        val mediaId = when (val upload = upload(image)) {
            is AuthOutcome.Failure -> return upload
            is AuthOutcome.Success -> upload.value
        }
        val body = JsonObject().apply { addProperty("avatarMediaId", mediaId) }
        return apiCaller.call { api.updateMe(body) }.toProfile()
    }

    override suspend fun removeAvatar(): AuthOutcome<MyProfile> {
        val body = JsonObject().apply { add("avatarMediaId", JsonNull.INSTANCE) }
        return apiCaller.call { api.updateMe(body) }.toProfile()
    }

    override suspend fun serverInfo(): AuthOutcome<ServerInfo> =
        apiCaller.call { api.getServerInfo() }.mapNotNull { dto ->
            ServerInfo(
                version = dto?.version ?: return@mapNotNull null,
                pushEnabled = dto.pushEnabled ?: false
            )
        }

    override suspend fun clearCache() {
        db.withTransaction {
            messageDao.deleteAllConfirmed()
            chatDao.deleteAll()
            userDao.deleteAll()
        }
    }

    /**
     * Resumable upload: declare size + SHA-256, then PUT chunks at the server's confirmed
     * offset. On 409 OFFSET_MISMATCH resume from the offset the server reports. Returns the mediaId.
     */
    private suspend fun upload(image: EncodedImage): AuthOutcome<String> {
        val bytes = image.bytes
        val start = apiCaller.call {
            api.startUpload(
                StartUploadRequestDto(
                    kind = "IMAGE",
                    mimeType = "image/jpeg",
                    sizeBytes = bytes.size.toLong(),
                    sha256 = sha256Hex(bytes),
                    width = image.width,
                    height = image.height
                )
            )
        }
        val session = when (start) {
            is AuthOutcome.Failure -> return start
            is AuthOutcome.Success -> start.value
        }
        val uploadId = session?.uploadId ?: return malformed()
        val mediaId = session.mediaId ?: return malformed()
        val chunkSize = session.chunkSize?.takeIf { it > 0 } ?: DEFAULT_CHUNK_SIZE

        var offset = 0L
        var attempts = 0
        while (offset < bytes.size) {
            if (++attempts > MAX_CHUNK_ATTEMPTS) return AuthOutcome.Failure(AuthError.ServiceUnavailable)
            val end = min(bytes.size.toLong(), offset + chunkSize).toInt()
            val chunk = bytes.copyOfRange(offset.toInt(), end).toRequestBody(OCTET_STREAM)
            try {
                val response = api.uploadChunk(uploadId, offset, chunk)
                when {
                    response.isSuccessful -> offset = response.body()?.confirmedOffset ?: end.toLong()
                    response.code() == 409 -> {
                        // Never guess: resume exactly where the server says it is.
                        val mismatch = response.errorBody()?.charStream()?.use {
                            gson.fromJson(it, OffsetMismatchDto::class.java)
                        }
                        offset = mismatch?.currentOffset ?: return AuthOutcome.Failure(AuthError.ServiceUnavailable)
                    }
                    response.code() in 500..599 -> Unit // transient: retry the same chunk
                    else -> return AuthOutcome.Failure(AuthError.Unknown("Upload rejected: ${response.code()}"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Timber.w(e, "Avatar chunk upload failed")
                return AuthOutcome.Failure(AuthError.Network)
            }
        }
        return AuthOutcome.Success(mediaId)
    }

    private fun AuthOutcome<UserMeDto?>.toProfile(): AuthOutcome<MyProfile> =
        mapNotNull { dto ->
            MyProfile(
                id = dto?.id ?: return@mapNotNull null,
                displayName = dto.displayName ?: dto.username ?: return@mapNotNull null,
                username = dto.username,
                avatarMediaId = dto.avatarMediaId,
                phone = dto.phone.orEmpty()
            )
        }.also { if (it is AuthOutcome.Success) _me.value = it.value }

    private fun <T> malformed(): AuthOutcome<T> = AuthOutcome.Failure(AuthError.Unknown("Malformed response"))

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private companion object {
        val OCTET_STREAM = "application/octet-stream".toMediaType()
        const val DEFAULT_CHUNK_SIZE = 512 * 1024
        const val MAX_CHUNK_ATTEMPTS = 20
    }
}
