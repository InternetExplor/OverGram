package com.example.overgram.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.example.overgram.BuildConfig
import com.example.overgram.data.local.prefs.TokenPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import java.io.File
import java.io.IOException
import kotlin.math.max

/**
 * The preview frame of a video message. Relay generates no thumbnails, so the frame is taken
 * from the video itself: our local copy if there is one, otherwise the server's file (the
 * retriever only reads the bits it needs, via Range requests).
 */
data class VideoFrame(val mediaId: String?, val localPath: String?, val contentUri: String? = null)

class VideoFrameKeyer : Keyer<VideoFrame> {
    override fun key(data: VideoFrame, options: Options): String = "video-frame:${data.mediaId ?: data.localPath ?: data.contentUri}"
}

class VideoFrameFetcher(
    private val data: VideoFrame,
    private val options: Options,
    private val store: LocalMediaStore,
    private val tokenPreferences: TokenPreferences
) : Fetcher {

    override suspend fun fetch(): FetchResult = withContext(Dispatchers.IO) {
        val cached = data.mediaId?.let { File(thumbDir(options.context), "$it.jpg") }
        if (cached != null && cached.length() > 0) {
            return@withContext SourceResult(
                source = ImageSource(cached.toOkioPath()),
                mimeType = "image/jpeg",
                dataSource = DataSource.DISK
            )
        }
        val frame = extractFrame() ?: throw IOException("No frame for $data")
        cached?.let { file ->
            file.parentFile?.mkdirs()
            runCatching { file.outputStream().use { frame.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) } }
        }
        DrawableResult(
            drawable = BitmapDrawable(options.context.resources, frame),
            isSampled = true,
            dataSource = if (data.mediaId == null) DataSource.DISK else DataSource.NETWORK
        )
    }

    private fun extractFrame(): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            val local = data.localPath?.takeIf { File(it).exists() } ?: store.fileFor(data.mediaId)?.path
            when {
                local != null -> retriever.setDataSource(local)
                // Picked but not sent yet (the send preview).
                data.contentUri != null -> retriever.setDataSource(options.context, Uri.parse(data.contentUri))
                data.mediaId != null -> retriever.setDataSource(
                    BuildConfig.API_BASE_URL + "v1/media/" + data.mediaId,
                    tokenPreferences.getAccessToken()?.let { mapOf("Authorization" to "Bearer $it") }.orEmpty()
                )
                else -> return null
            }
            // Some decoders can't produce the very first (or a scaled) frame: fall back to any frame.
            val frame = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                runCatching { retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, MAX_SIDE, MAX_SIDE) }.getOrNull()
            } else {
                null
            })
                ?: runCatching { retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) }.getOrNull()
                ?: runCatching { retriever.frameAtTime }.getOrNull()
                ?: return null
            val scale = MAX_SIDE.toFloat() / max(frame.width, frame.height)
            if (scale >= 1f) frame else {
                Bitmap.createScaledBitmap(frame, (frame.width * scale).toInt(), (frame.height * scale).toInt(), true)
            }
        } catch (e: Exception) {
            // Unsupported codec, no access, offline: the bubble keeps its placeholder.
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    class Factory(
        private val store: LocalMediaStore,
        private val tokenPreferences: TokenPreferences
    ) : Fetcher.Factory<VideoFrame> {
        override fun create(data: VideoFrame, options: Options, imageLoader: ImageLoader): Fetcher =
            VideoFrameFetcher(data, options, store, tokenPreferences)
    }

    private companion object {
        const val MAX_SIDE = 640
        const val JPEG_QUALITY = 80

        fun thumbDir(context: Context) = File(context.cacheDir, "video-frames")
    }
}
