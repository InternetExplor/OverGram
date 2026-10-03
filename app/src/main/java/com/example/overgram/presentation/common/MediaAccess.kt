package com.example.overgram.presentation.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.FileProvider
import com.example.overgram.data.media.LocalMediaStore
import com.example.overgram.data.media.VideoFrame
import com.example.overgram.domain.model.MediaAttachment
import com.example.overgram.domain.model.MediaKind
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import java.util.Locale

/** What media UI needs from the app graph (composables can't take constructor injection). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MediaEntryPoint {
    fun localMediaStore(): LocalMediaStore

    /** Authenticated (and token-refreshing) client: media downloads need the bearer token. */
    fun okHttpClient(): OkHttpClient
}

/** Null in previews, where there is no Hilt graph. */
@Composable
fun rememberMediaEntryPoint(): MediaEntryPoint? {
    if (LocalInspectionMode.current) return null
    val context = LocalContext.current.applicationContext
    return remember(context) { EntryPointAccessors.fromApplication(context, MediaEntryPoint::class.java) }
}

/** Our own copy (still uploading, or kept after sending / downloading), if the device has one. */
fun MediaAttachment.localFile(store: LocalMediaStore?): File? =
    localPath?.let(::File)?.takeIf { it.exists() } ?: store?.fileFor(mediaId)

/** What Coil should load for a photo or video bubble. */
fun MediaAttachment.previewModel(store: LocalMediaStore?): Any? = when (kind) {
    MediaKind.VIDEO -> VideoFrame(mediaId, localPath)
    else -> localFile(store) ?: mediaUrl(mediaId)
}

/** Hands [file] to another app ("Open with…"). False if nothing on the device can open it. */
fun openWithOtherApp(context: Context, file: File, mimeType: String): Boolean {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType.ifBlank { "*/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

/** "1.2 MB", in the device's locale and units. */
fun formatFileSize(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes)

/** "0:42", "12:05", "1:02:33". */
fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}
