package com.example.overgram.data.repository

import androidx.room.withTransaction
import com.example.overgram.data.local.db.ChatDao
import com.example.overgram.data.local.db.MessageDao
import com.example.overgram.data.local.db.OverGramDatabase
import com.example.overgram.data.local.db.UserDao
import com.example.overgram.data.media.AvatarImageLoader
import com.example.overgram.data.media.EncodedImage
import com.example.overgram.data.media.BytesUploadSource
import com.example.overgram.data.media.LocalMediaStore
import com.example.overgram.data.media.MediaUploader
import com.example.overgram.data.remote.ApiCaller
import com.example.overgram.data.remote.api.ProfileApi
import com.example.overgram.data.remote.dto.UserMeDto
import com.example.overgram.data.remote.mapNotNull
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.ServerInfo
import com.example.overgram.domain.repository.ProfileRepository
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: ProfileApi,
    private val apiCaller: ApiCaller,
    private val imageLoader: AvatarImageLoader,
    private val uploader: MediaUploader,
    private val mediaStore: LocalMediaStore,
    private val db: OverGramDatabase,
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val userDao: UserDao
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
        val upload = uploader.upload(BytesUploadSource(image.bytes), "IMAGE", "image/jpeg", image.width, image.height)
        val mediaId = when (upload) {
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
        mediaStore.clearDownloads()
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
}
