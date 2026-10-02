package com.example.overgram.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/** A JPEG ready to upload. */
class EncodedImage(val bytes: ByteArray, val width: Int, val height: Int)

/**
 * Turns a picked photo into a small square avatar: center crop, at most [SIZE] px, JPEG.
 * Avatars are shown as small circles, so uploading a 12 MP original would only waste data.
 */
@Singleton
class AvatarImageLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun load(uri: String): EncodedImage? = withContext(Dispatchers.IO) {
        try {
            val bitmap = decode(Uri.parse(uri)) ?: return@withContext null
            val side = min(bitmap.width, bitmap.height)
            val square = Bitmap.createBitmap(
                bitmap,
                (bitmap.width - side) / 2,
                (bitmap.height - side) / 2,
                side,
                side
            )
            val target = min(side, SIZE)
            val scaled = if (target == side) square else Bitmap.createScaledBitmap(square, target, target, true)
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            EncodedImage(out.toByteArray(), scaled.width, scaled.height)
        } catch (e: Exception) {
            Timber.w(e, "Couldn't read picked image")
            null
        }
    }

    /** Decodes downsampled, respecting EXIF rotation where the platform can (API 28+). */
    private fun decode(uri: Uri): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val shortSide = min(info.size.width, info.size.height)
                if (shortSide > SIZE * 2) {
                    val ratio = shortSide / (SIZE * 2)
                    decoder.setTargetSize(info.size.width / ratio, info.size.height / ratio)
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (min(bounds.outWidth, bounds.outHeight) / (sample * 2) >= SIZE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private companion object {
        const val SIZE = 512
        const val JPEG_QUALITY = 88
    }
}
