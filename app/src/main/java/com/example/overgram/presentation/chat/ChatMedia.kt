package com.example.overgram.presentation.chat

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.overgram.R
import com.example.overgram.data.media.DownloadState
import com.example.overgram.data.media.VideoFrame
import com.example.overgram.domain.model.MediaAttachment
import com.example.overgram.domain.model.MediaKind
import com.example.overgram.presentation.chatlist.formatClock
import com.example.overgram.presentation.common.formatDuration
import com.example.overgram.presentation.common.formatFileSize
import com.example.overgram.presentation.common.localFile
import com.example.overgram.presentation.common.openWithOtherApp
import com.example.overgram.presentation.common.previewModel
import com.example.overgram.presentation.common.rememberMediaEntryPoint
import com.example.overgram.ui.components.BubbleStatus
import com.example.overgram.ui.components.FileBubble
import com.example.overgram.ui.components.QuoteContent
import com.example.overgram.ui.components.TransferState
import com.example.overgram.ui.components.VisualMediaBubble
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.AttachFile
import com.example.overgram.ui.theme.AttachGallery
import com.example.overgram.ui.theme.SendBlue
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary
import kotlinx.coroutines.flow.MutableStateFlow

/** A photo, video or document message. */
@Composable
internal fun MediaMessageBubble(
    item: ChatMessageItem,
    media: MediaAttachment,
    senderName: String?,
    senderColor: Color,
    status: BubbleStatus,
    uploadProgress: Float?,
    actions: ChatActions,
    reply: QuoteContent? = null,
    onReplyClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val store = rememberMediaEntryPoint()?.localMediaStore()
    val sending = item.outgoingState == OutgoingState.Sending
    val failed = item.outgoingState == OutgoingState.Failed
    val timestamp = formatClock(item.createdAt)
    val onCancel = if (sending) ({ actions.onCancelSending(item.clientMessageId) }) else null

    if (media.kind == MediaKind.FILE) {
        val downloads by (store?.downloads ?: remember { MutableStateFlow(emptyMap()) }).collectAsState()
        val download = media.mediaId?.let { downloads[it] }
        val local = remember(media, download) { media.localFile(store) }
        val fileName = item.body?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chats_preview_file)
        // Open the file once a download the user asked for finishes.
        var openWhenDownloaded by remember { mutableStateOf(false) }
        LaunchedEffect(download) {
            if (!openWhenDownloaded) return@LaunchedEffect
            when (download) {
                is DownloadState.Done -> {
                    openWhenDownloaded = false
                    if (!openWithOtherApp(context, download.file, media.mimeType)) actions.onMediaNotice(MediaNotice.NoAppToOpen)
                }
                DownloadState.Failed -> {
                    openWhenDownloaded = false
                    actions.onMediaNotice(MediaNotice.DownloadFailed)
                }
                else -> Unit
            }
        }
        val transfer = when {
            sending -> TransferState.Running(uploadProgress)
            failed -> TransferState.Failed
            download is DownloadState.Running -> TransferState.Running(download.fraction)
            local != null -> TransferState.Idle
            download is DownloadState.Failed -> TransferState.Failed
            else -> TransferState.Remote
        }
        val total = formatFileSize(context, media.sizeBytes)
        val sizeText = (transfer as? TransferState.Running)?.progress
            ?.let { stringResource(R.string.media_progress, formatFileSize(context, (media.sizeBytes * it).toLong()), total) }
            ?: total
        FileBubble(
            fileName = fileName,
            sizeText = sizeText,
            timestamp = timestamp,
            isSent = item.isOutgoing,
            status = status,
            isEdited = item.isEdited,
            senderName = senderName,
            senderColor = senderColor,
            transfer = transfer,
            onCancel = onCancel,
            reply = reply,
            onReplyClick = onReplyClick,
            onLongClick = onLongClick,
            onClick = {
                when {
                    failed -> actions.onRetryMessage(item.clientMessageId)
                    sending || download is DownloadState.Running -> Unit
                    local != null -> if (!openWithOtherApp(context, local, media.mimeType)) {
                        actions.onMediaNotice(MediaNotice.NoAppToOpen)
                    }
                    media.mediaId != null && store != null -> {
                        openWhenDownloaded = true
                        store.download(media.mediaId, fileName)
                    }
                }
            }
        )
    } else {
        val isVideo = media.kind == MediaKind.VIDEO
        val model = remember(media) { media.previewModel(store) }
        VisualMediaBubble(
            model = model,
            isVideo = isVideo,
            width = media.width,
            height = media.height,
            timestamp = timestamp,
            isSent = item.isOutgoing,
            caption = item.body?.takeIf { it.isNotBlank() },
            durationText = if (isVideo) media.durationMs?.let(::formatDuration) else null,
            status = status,
            isEdited = item.isEdited,
            senderName = senderName,
            senderColor = senderColor,
            transfer = when {
                sending -> TransferState.Running(uploadProgress)
                failed -> TransferState.Failed
                else -> TransferState.Idle
            },
            reply = reply,
            onReplyClick = onReplyClick,
            onLongClick = onLongClick,
            onCancel = onCancel,
            onClick = {
                when {
                    failed -> actions.onRetryMessage(item.clientMessageId)
                    sending -> Unit
                    else -> actions.onOpenMedia(media)
                }
            }
        )
    }
}

/** What the user picked, waiting in the send sheet. */
private data class PendingAttachments(val uris: List<String>, val asFile: Boolean)

/**
 * The 📎 flow, like Telegram's: a sheet with Gallery / File, the system picker, then a preview
 * sheet with a caption before anything is sent.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AttachFlow(
    isMenuOpen: Boolean,
    onDismissMenu: () -> Unit,
    onSend: (uris: List<String>, caption: String?, asFile: Boolean) -> Unit
) {
    var pendingUris by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    var pendingAsFile by rememberSaveable { mutableStateOf(false) }
    val pending = pendingUris?.let { PendingAttachments(it, pendingAsFile) }

    val galleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_PICKED)
    ) { uris ->
        if (uris.isNotEmpty()) {
            pendingAsFile = false
            pendingUris = ArrayList(uris.map(Uri::toString))
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            pendingAsFile = true
            pendingUris = ArrayList(uris.take(MAX_PICKED).map(Uri::toString))
        }
    }

    if (isMenuOpen) {
        ModalBottomSheet(onDismissRequest = onDismissMenu, containerColor = SurfaceDark) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachOption(Icons.Default.Image, stringResource(R.string.attach_gallery), AttachGallery) {
                    onDismissMenu()
                    galleryPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }
                AttachOption(Icons.AutoMirrored.Filled.InsertDriveFile, stringResource(R.string.attach_file), AttachFile) {
                    onDismissMenu()
                    filePicker.launch(arrayOf("*/*"))
                }
            }
        }
    }

    if (pending != null) {
        SendAttachmentsSheet(
            pending = pending,
            onDismiss = { pendingUris = null },
            onSend = { caption ->
                pendingUris = null
                onSend(pending.uris, caption, pending.asFile)
            }
        )
    }
}

@Composable
private fun AttachOption(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SendAttachmentsSheet(
    pending: PendingAttachments,
    onDismiss: () -> Unit,
    onSend: (caption: String?) -> Unit
) {
    val context = LocalContext.current
    var caption by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SurfaceDark) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = pluralStringResource(R.plurals.media_send_title, pending.uris.size, pending.uris.size),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            if (pending.asFile) {
                pending.uris.forEach { uri ->
                    val (name, size) = remember(uri) { nameAndSize(context, uri) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(42.dp).clip(CircleShape).background(Accent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.InsertDriveFile, contentDescription = null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.MiddleEllipsis)
                            if (size != null) {
                                Text(formatFileSize(context, size), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    items(pending.uris) { uri ->
                        val isVideo = remember(uri) { context.contentResolver.getType(Uri.parse(uri))?.startsWith("video/") == true }
                        SubcomposeAsyncImage(
                            model = if (isVideo) VideoFrame(mediaId = null, localPath = null, contentUri = uri) else uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(if (pending.uris.size == 1) 200.dp else 96.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SurfaceElevatedDark),
                            error = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(if (isVideo) Icons.Default.Movie else Icons.Default.Image, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
            ) {
                if (!pending.asFile) {
                    TextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text(stringResource(R.string.media_caption_placeholder), color = TextSecondary) },
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = Accent
                        ),
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                IconButton(
                    onClick = { onSend(caption.takeIf { !pending.asFile }) },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SendBlue)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.media_send), tint = Color.White)
                }
            }
        }
    }
}

private fun nameAndSize(context: Context, uri: String): Pair<String, Long?> {
    val parsed = Uri.parse(uri)
    return try {
        context.contentResolver.query(parsed, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let(cursor::getString)
                val size = cursor.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong)
                (name ?: parsed.lastPathSegment.orEmpty()) to size
            }
    } catch (e: Exception) {
        null
    } ?: (parsed.lastPathSegment.orEmpty() to null)
}

private const val MAX_PICKED = 10
