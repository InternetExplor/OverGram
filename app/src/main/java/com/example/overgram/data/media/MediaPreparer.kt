package com.example.overgram.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.exifinterface.media.ExifInterface
import com.example.overgram.domain.model.MediaKind
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/** An attachment copied into the outbox, ready to upload. */
data class PreparedMedia(
    val file: File,
    val kind: MediaKind,
    val mimeType: String,
    val fileName: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null
) {
    val sizeBytes: Long get() = file.length()
}

sealed interface PrepareResult {
    data class Ready(val media: PreparedMedia) : PrepareResult
    data object TooLarge : PrepareResult
    data object Unreadable : PrepareResult
}

/**
 * Turns a picked item into an outbox file. Like Telegram: gallery photos are re-encoded to a
 * sensible size (a 12 MP original is several MB nobody needs in a chat); videos go as they are;
 * anything sent "as a file" goes byte for byte, whatever it is.
 */
@Singleton
class MediaPreparer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: LocalMediaStore
) {

    suspend fun prepare(uri: String, asFile: Boolean): PrepareResult = withContext(Dispatchers.IO) {
        val source = Uri.parse(uri)
        try {
            val (name, size) = nameAndSize(source)
            val mimeType = context.contentResolver.getType(source) ?: "application/octet-stream"
            val kind = when {
                asFile -> MediaKind.FILE
                mimeType.startsWith("image/") -> MediaKind.IMAGE
                mimeType.startsWith("video/") -> MediaKind.VIDEO
                else -> MediaKind.FILE
            }
            // Photos shrink when re-encoded; everything else must fit as it is.
            if (kind != MediaKind.IMAGE && size != null && size > MAX_SIZE_BYTES) return@withContext PrepareResult.TooLarge
            val prepared = when (kind) {
                MediaKind.IMAGE -> if (mimeType == "image/gif") copy(source, name, kind, mimeType) else photo(source, name)
                MediaKind.VIDEO -> video(copy(source, name, kind, mimeType))
                MediaKind.FILE -> copy(source, name, kind, mimeType)
            } ?: return@withContext PrepareResult.Unreadable
            when {
                prepared.sizeBytes == 0L -> {
                    store.discard(prepared.file.path)
                    PrepareResult.Unreadable
                }
                prepared.sizeBytes > MAX_SIZE_BYTES -> {
                    store.discard(prepared.file.path)
                    PrepareResult.TooLarge
                }
                else -> PrepareResult.Ready(prepared)
            }
        } catch (e: Exception) {
            Timber.w(e, "Couldn't prepare $uri")
            PrepareResult.Unreadable
        }
    }

    private fun nameAndSize(uri: Uri): Pair<String, Long?> {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }
                        ?.let { cursor.getString(it) }
                    val size = cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }
                        ?.let { cursor.getLong(it) }
                    return (name ?: uri.lastPathSegment ?: "file") to size
                }
            }
        return (uri.lastPathSegment ?: "file") to null
    }

    private fun copy(uri: Uri, name: String, kind: MediaKind, mimeType: String): PreparedMedia? {
        val target = store.newOutboxFile(name)
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { source -> target.outputStream().use { source.copyTo(it) } }
        return PreparedMedia(target, kind, mimeType, name)
    }

    /** Downsampled to [MAX_PHOTO_SIDE], upright (EXIF applied), JPEG. */
    private fun photo(uri: Uri, name: String): PreparedMedia? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_PHOTO_SIDE) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val scale = MAX_PHOTO_SIDE.toFloat() / max(decoded.width, decoded.height)
        val matrix = Matrix().apply {
            if (scale < 1f) postScale(scale, scale)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        val bitmap = if (matrix.isIdentity) decoded else {
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        }
        val target = store.newOutboxFile(name.substringBeforeLast('.') + ".jpg")
        target.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return PreparedMedia(target, MediaKind.IMAGE, "image/jpeg", target.name, bitmap.width, bitmap.height)
    }

    /** Fills in the frame size (as displayed, i.e. rotation applied) and duration. */
    private fun video(copied: PreparedMedia?): PreparedMedia? {
        copied ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(copied.file.path)
            fun meta(key: Int) = retriever.extractMetadata(key)?.toLongOrNull()
            val width = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
            val height = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
            val rotated = meta(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toInt() in setOf(90, 270)
            copied.copy(
                width = if (rotated) height else width,
                height = if (rotated) width else height,
                durationMs = meta(MediaMetadataRetriever.METADATA_KEY_DURATION)
            )
        } catch (e: Exception) {
            Timber.w(e, "No metadata for ${copied.file}")
            copied // still sendable, just without a known frame size
        } finally {
            retriever.release()
        }
    }

    companion object {
        /** Relay's per-file limit. */
        const val MAX_SIZE_BYTES = 100L * 1024 * 1024
        private const val MAX_PHOTO_SIDE = 1920
        private const val JPEG_QUALITY = 87
    }
}
