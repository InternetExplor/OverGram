package com.example.overgram.data.media

import com.example.overgram.data.remote.ApiCaller
import com.example.overgram.data.remote.api.MediaApi
import com.example.overgram.data.remote.dto.ErrorDto
import com.example.overgram.data.remote.dto.OffsetMismatchDto
import com.example.overgram.data.remote.dto.StartUploadRequestDto
import com.example.overgram.domain.model.AuthError
import com.example.overgram.domain.model.AuthOutcome
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/** The bytes to upload: a file on disk (attachments) or an in-memory image (avatars). */
interface UploadSource {
    val size: Long

    /** Reads up to [length] bytes starting at [offset]. */
    fun read(offset: Long, length: Int): ByteArray

    fun sha256Hex(): String
}

class FileUploadSource(private val file: File) : UploadSource {
    override val size: Long get() = file.length()

    override fun read(offset: Long, length: Int): ByteArray = RandomAccessFile(file, "r").use { raf ->
        raf.seek(offset)
        val buffer = ByteArray(min(length.toLong(), size - offset).toInt())
        raf.readFully(buffer)
        buffer
    }

    override fun sha256Hex(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }
}

class BytesUploadSource(private val bytes: ByteArray) : UploadSource {
    override val size: Long get() = bytes.size.toLong()

    override fun read(offset: Long, length: Int): ByteArray =
        bytes.copyOfRange(offset.toInt(), min(bytes.size.toLong(), offset + length).toInt())

    override fun sha256Hex(): String = MessageDigest.getInstance("SHA-256").digest(bytes).toHex()
}

private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

/** A declared upload: the bytes go to [uploadId], the finished file is [mediaId]. */
data class UploadSession(val uploadId: String, val mediaId: String)

sealed interface ChunksResult {
    data object Done : ChunksResult

    /** The session is gone (expired, unknown, or the hash didn't match): start a new one. */
    data object SessionLost : ChunksResult

    /** [isCancelled] said stop. */
    data object Cancelled : ChunksResult

    data class Failed(val error: AuthError) : ChunksResult
}

/**
 * Relay's resumable upload: declare size + SHA-256, then PUT chunks at the server's confirmed
 * offset. A session survives app restarts: [sendChunks] with `resume = true` asks the server
 * where it is (`HEAD`) instead of guessing, and a 409 OFFSET_MISMATCH resumes from the offset
 * the server reports.
 */
@Singleton
class MediaUploader @Inject constructor(
    private val api: MediaApi,
    private val apiCaller: ApiCaller,
    private val gson: Gson
) {

    suspend fun start(
        source: UploadSource,
        kind: String,
        mimeType: String,
        width: Int? = null,
        height: Int? = null,
        durationMs: Long? = null
    ): AuthOutcome<UploadSession> {
        val sha256 = withContext(Dispatchers.IO) { source.sha256Hex() }
        val start = apiCaller.call {
            api.startUpload(
                StartUploadRequestDto(
                    kind = kind,
                    mimeType = mimeType,
                    sizeBytes = source.size,
                    sha256 = sha256,
                    width = width,
                    height = height,
                    durationMs = durationMs
                )
            )
        }
        return when (start) {
            is AuthOutcome.Failure -> start
            is AuthOutcome.Success -> {
                val uploadId = start.value?.uploadId
                val mediaId = start.value?.mediaId
                if (uploadId == null || mediaId == null) {
                    AuthOutcome.Failure(AuthError.Unknown("Malformed response"))
                } else {
                    AuthOutcome.Success(UploadSession(uploadId, mediaId))
                }
            }
        }
    }

    /**
     * Sends the remaining chunks. [onProgress] gets the confirmed byte count; [isCancelled] is
     * checked between chunks.
     */
    suspend fun sendChunks(
        source: UploadSource,
        uploadId: String,
        resume: Boolean,
        onProgress: suspend (Long) -> Unit = {},
        isCancelled: suspend () -> Boolean = { false }
    ): ChunksResult = withContext(Dispatchers.IO) {
        val size = source.size
        try {
            var offset = if (resume) {
                when (val confirmed = confirmedOffset(uploadId)) {
                    null -> return@withContext ChunksResult.SessionLost
                    else -> confirmed
                }
            } else {
                0L
            }
            var failures = 0
            while (offset < size) {
                if (isCancelled()) return@withContext ChunksResult.Cancelled
                onProgress(offset)
                val chunk = source.read(offset, CHUNK_SIZE).toRequestBody(OCTET_STREAM)
                val response = api.uploadChunk(uploadId, offset, chunk)
                when {
                    response.isSuccessful -> {
                        offset = response.body()?.confirmedOffset ?: (offset + chunk.contentLength())
                        failures = 0
                    }
                    response.code() == 409 -> {
                        // Never guess: resume exactly where the server says it is.
                        val mismatch = response.errorBody()?.charStream()?.use {
                            gson.fromJson(it, OffsetMismatchDto::class.java)
                        }
                        offset = mismatch?.currentOffset
                            ?: confirmedOffset(uploadId)
                            ?: return@withContext ChunksResult.SessionLost
                    }
                    response.code() in 500..599 -> {
                        if (++failures > MAX_CHUNK_FAILURES) return@withContext ChunksResult.Failed(AuthError.ServiceUnavailable)
                        delay(failures * RETRY_BACKOFF_MS) // transient: same chunk again
                    }
                    // UPLOAD_EXPIRED, a sha256 mismatch, an unknown session: only a fresh start helps.
                    response.code() == 400 || response.code() == 404 -> {
                        val code = response.errorBody()?.charStream()?.use { gson.fromJson(it, ErrorDto::class.java) }?.code
                        Timber.w("Upload $uploadId lost: ${response.code()} $code")
                        return@withContext ChunksResult.SessionLost
                    }
                    response.code() == 429 -> return@withContext ChunksResult.Failed(AuthError.RateLimited())
                    else -> return@withContext ChunksResult.Failed(AuthError.Unknown("Upload rejected: ${response.code()}"))
                }
            }
            onProgress(size)
            ChunksResult.Done
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Timber.w(e, "Chunk upload failed")
            ChunksResult.Failed(AuthError.Network)
        }
    }

    /** Uploads a small in-memory file in one go (avatars). Returns the mediaId. */
    suspend fun upload(source: UploadSource, kind: String, mimeType: String, width: Int?, height: Int?): AuthOutcome<String> {
        val session = when (val start = start(source, kind, mimeType, width, height)) {
            is AuthOutcome.Failure -> return start
            is AuthOutcome.Success -> start.value
        }
        return when (val result = sendChunks(source, session.uploadId, resume = false)) {
            ChunksResult.Done -> AuthOutcome.Success(session.mediaId)
            is ChunksResult.Failed -> AuthOutcome.Failure(result.error)
            ChunksResult.SessionLost, ChunksResult.Cancelled -> AuthOutcome.Failure(AuthError.ServiceUnavailable)
        }
    }

    /** The server's confirmed offset, or null if the session no longer exists. Throws on network errors. */
    private suspend fun confirmedOffset(uploadId: String): Long? {
        val response = api.uploadOffset(uploadId)
        if (!response.isSuccessful) {
            if (response.code() in 500..599) throw IOException("HEAD upload: ${response.code()}")
            return null
        }
        return response.headers()["Upload-Offset"]?.trim()?.toLongOrNull()
    }

    private companion object {
        val OCTET_STREAM = "application/octet-stream".toMediaType()

        /** Relay's chunk limit (512 KiB); also what `POST /v1/media/uploads` reports. */
        const val CHUNK_SIZE = 512 * 1024
        const val MAX_CHUNK_FAILURES = 5
        const val RETRY_BACKOFF_MS = 1_000L
    }
}
