package com.example.overgram.data.media

import android.content.Context
import com.example.overgram.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

/** State of a file download, by mediaId. */
sealed interface DownloadState {
    /** [fraction] is null while the size isn't known. */
    data class Running(val fraction: Float?) : DownloadState
    data class Done(val file: File) : DownloadState
    data object Failed : DownloadState
}

/**
 * Files on this device:
 * - **outbox** (`files/outbox`): our copies of attachments still being sent. Not a cache — losing
 *   one would lose the message — so they live in app files until the upload is done;
 * - **media** (`cache/media/<mediaId>/<name>`): finished uploads and downloaded files, so our own
 *   photos show without a round trip and a downloaded document opens instantly the next time.
 */
@Singleton
class LocalMediaStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {

    private val outboxDir get() = File(context.filesDir, "outbox")
    private val mediaDir get() = File(context.cacheDir, "media")

    /** Downloads outlive the screen that started them. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    /** A new, empty place for an attachment we're about to send. */
    fun newOutboxFile(fileName: String): File {
        val dir = File(outboxDir, UUID.randomUUID().toString()).apply { mkdirs() }
        return File(dir, safeName(fileName))
    }

    /** The finished or downloaded file for [mediaId], if it's on the device. */
    fun fileFor(mediaId: String?): File? {
        if (mediaId.isNullOrBlank()) return null
        return File(mediaDir, mediaId).listFiles()
            ?.firstOrNull { it.isFile && !it.name.endsWith(PARTIAL_SUFFIX) }
    }

    /** The upload of [file] (in the outbox) finished as [mediaId]: keep it as that media's local copy. */
    fun adopt(file: File, mediaId: String) {
        val dir = File(mediaDir, mediaId).apply { mkdirs() }
        val target = File(dir, file.name)
        if (!file.renameTo(target)) {
            // Different volume or a leftover target: copy instead.
            runCatching { file.copyTo(target, overwrite = true) }.onFailure { Timber.w(it, "Couldn't keep $file") }
        }
        discard(file.path)
    }

    /** Deletes an outbox file (a cancelled or finished upload) together with its folder. */
    fun discard(path: String?) {
        val file = path?.let(::File) ?: return
        file.delete()
        val parent = file.parentFile
        if (parent != null && parent.parentFile == outboxDir) parent.deleteRecursively()
    }

    /** Starts downloading [mediaId] unless it's on the device already or running; watch [downloads]. */
    fun download(mediaId: String, fileName: String) {
        fileFor(mediaId)?.let { existing ->
            _downloads.update { it + (mediaId to DownloadState.Done(existing)) }
            return
        }
        if (_downloads.value[mediaId] is DownloadState.Running) return
        _downloads.update { it + (mediaId to DownloadState.Running(null)) }
        scope.launch {
            val result = try {
                fetch(mediaId, fileName)?.let { DownloadState.Done(it) } ?: DownloadState.Failed
            } catch (e: IOException) {
                Timber.w(e, "Download of $mediaId failed")
                DownloadState.Failed
            }
            _downloads.update { it + (mediaId to result) }
        }
    }

    private suspend fun fetch(mediaId: String, fileName: String): File? {
        val request = Request.Builder().url(BuildConfig.API_BASE_URL + "v1/media/" + mediaId).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Timber.w("Download of $mediaId: HTTP ${response.code}")
                return null
            }
            val body = response.body ?: return null
            val total = body.contentLength().takeIf { it > 0 }
            val dir = File(mediaDir, mediaId).apply { mkdirs() }
            val target = File(dir, safeName(fileName))
            val partial = File(dir, target.name + PARTIAL_SUFFIX)
            body.byteStream().use { input ->
                partial.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var copied = 0L
                    var lastReported = 0f
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        if (total != null) {
                            val fraction = copied.toFloat() / total
                            // ~100 updates per file is plenty for a progress ring.
                            if (fraction - lastReported >= 0.01f) {
                                lastReported = fraction
                                _downloads.update { it + (mediaId to DownloadState.Running(fraction)) }
                            }
                        }
                    }
                }
            }
            if (!partial.renameTo(target)) return null
            return target
        }
    }

    /** "Clear cache": downloaded and sent files; the outbox (still unsent) stays. */
    fun clearDownloads() {
        mediaDir.deleteRecursively()
        _downloads.value = emptyMap()
    }

    /** Logout: nothing of this account stays on the device. */
    fun clearAll() {
        clearDownloads()
        outboxDir.deleteRecursively()
    }

    private fun safeName(name: String): String =
        name.replace(UNSAFE_CHARS, "_").trim().take(MAX_NAME_LENGTH).ifEmpty { "file" }

    private companion object {
        const val PARTIAL_SUFFIX = ".part"
        const val BUFFER_SIZE = 64 * 1024
        const val MAX_NAME_LENGTH = 120
        val UNSAFE_CHARS = Regex("""[\\/:*?"<>|\u0000-\u001f]""")
    }
}
