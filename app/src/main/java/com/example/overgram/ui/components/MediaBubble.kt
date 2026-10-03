package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.overgram.R
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.AccentBright
import com.example.overgram.ui.theme.ChatWallpaper
import com.example.overgram.ui.theme.IncomingMeta
import com.example.overgram.ui.theme.MediaOverlay
import com.example.overgram.ui.theme.MediaPlaceholder
import com.example.overgram.ui.theme.OutgoingFileIcon
import com.example.overgram.ui.theme.OutgoingMeta
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.ReceivedBubbleColor
import com.example.overgram.ui.theme.ReceivedBubbleShape
import com.example.overgram.ui.theme.SentBubbleColor
import com.example.overgram.ui.theme.SentBubbleShape
import com.example.overgram.ui.theme.TextPrimary

/** Where a transfer (our upload, or a download) stands. */
sealed interface TransferState {
    /** Nothing to do (on the device, or shown from the network as needed). */
    data object Idle : TransferState
    /** Not on the device yet: tap downloads it. */
    data object Remote : TransferState
    /** [progress] null while unknown. */
    data class Running(val progress: Float?) : TransferState
    data object Failed : TransferState
}

private val MaxMediaWidth = 260.dp
private val MaxMediaHeight = 320.dp
private val MinMediaWidth = 140.dp
private val MinMediaHeight = 110.dp
private val MediaCorner = 13.dp

/**
 * A photo or video message, Telegram style: the picture fills the bubble; with no caption the
 * time sits in a dark pill on the picture, with one the caption follows below it like text.
 *
 * @param model What Coil loads: a local File, a media URL, or a video frame request.
 * @param width Pixel size, for the bubble's proportions (a square when unknown).
 * @param durationText Videos: shown in the top-left corner.
 * @param transfer [TransferState.Running] while uploading: a progress ring that cancels on tap.
 * @param onCancel Cancels the upload.
 * @param reply The message this one answers, quoted above the picture.
 * @param onLongClick Opens the message menu.
 */
@Composable
fun VisualMediaBubble(
    model: Any?,
    isVideo: Boolean,
    width: Int?,
    height: Int?,
    timestamp: String,
    isSent: Boolean,
    modifier: Modifier = Modifier,
    caption: String? = null,
    durationText: String? = null,
    status: BubbleStatus = BubbleStatus.Sent,
    isEdited: Boolean = false,
    senderName: String? = null,
    senderColor: Color = AccentBright,
    reply: QuoteContent? = null,
    onReplyClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    transfer: TransferState = TransferState.Idle,
    onClick: () -> Unit = {},
    onCancel: (() -> Unit)? = null
) {
    val (boxWidth, boxHeight) = mediaSize(width, height)
    val metaColor = if (isSent) OutgoingMeta else IncomingMeta
    BubbleContainer(isSent = isSent, modifier = modifier) {
        Column(Modifier.width(boxWidth)) {
            if (senderName != null) SenderName(senderName, senderColor, Modifier.padding(start = 7.dp, end = 7.dp, top = 3.dp, bottom = 3.dp))
            if (reply != null) {
                MessageQuote(reply, onClick = onReplyClick, modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 3.dp, bottom = 4.dp))
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(boxHeight)
                    .clip(RoundedCornerShape(MediaCorner))
                    .background(MediaPlaceholder)
                    .bubbleClicks(onClick, onLongClick)
            ) {
                SubcomposeAsyncImage(
                    model = model,
                    contentDescription = stringResource(if (isVideo) R.string.chats_preview_video else R.string.chats_preview_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { Box(Modifier.fillMaxSize().background(MediaPlaceholder)) },
                    error = { Box(Modifier.fillMaxSize().background(MediaPlaceholder)) }
                )
                if (durationText != null) {
                    OverlayPill(durationText, Modifier.align(Alignment.TopStart).padding(6.dp))
                }
                when (transfer) {
                    is TransferState.Running -> TransferButton(
                        transfer = transfer,
                        onClick = onCancel ?: {},
                        modifier = Modifier.align(Alignment.Center)
                    )
                    TransferState.Failed -> RoundOverlayIcon(Modifier.align(Alignment.Center)) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                    }
                    else -> if (isVideo) {
                        RoundOverlayIcon(Modifier.align(Alignment.Center)) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource(R.string.media_play),
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
                if (caption == null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .background(MediaOverlay, RoundedCornerShape(50))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BubbleMeta(
                            text = bubbleMetaText(timestamp, isEdited, isSent = false),
                            isSent = isSent,
                            status = status,
                            color = Color.White,
                            checkColor = Color.White
                        )
                    }
                }
            }
            if (caption != null) {
                Box(Modifier.padding(start = 7.dp, end = 5.dp, top = 5.dp, bottom = 3.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append(caption)
                            withStyle(SpanStyle(color = Color.Transparent, fontSize = MetaSize)) {
                                append("  " + bubbleMetaText(timestamp, isEdited, isSent))
                            }
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    BubbleMeta(
                        text = bubbleMetaText(timestamp, isEdited, isSent = false),
                        isSent = isSent,
                        status = status,
                        color = metaColor,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

/**
 * A document: a round icon (download / progress / file), the name and the size.
 *
 * @param sizeText "1.2 MB", or "0.4 / 1.2 MB" while transferring.
 * @param onClick Downloads, or opens once it's on the device.
 * @param onCancel Cancels our upload.
 * @param reply The message this one answers, quoted above the file.
 * @param onLongClick Opens the message menu.
 */
@Composable
fun FileBubble(
    fileName: String,
    sizeText: String,
    timestamp: String,
    isSent: Boolean,
    modifier: Modifier = Modifier,
    status: BubbleStatus = BubbleStatus.Sent,
    isEdited: Boolean = false,
    senderName: String? = null,
    senderColor: Color = AccentBright,
    reply: QuoteContent? = null,
    onReplyClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    transfer: TransferState = TransferState.Idle,
    onClick: () -> Unit = {},
    onCancel: (() -> Unit)? = null
) {
    val metaColor = if (isSent) OutgoingMeta else IncomingMeta
    val circleColor = if (isSent) OutgoingFileIcon else Accent
    BubbleContainer(isSent = isSent, modifier = modifier) {
        Column(Modifier.widthIn(max = 280.dp).padding(start = 7.dp, end = 5.dp, top = 5.dp, bottom = 3.dp)) {
            if (senderName != null) SenderName(senderName, senderColor, Modifier.padding(bottom = 4.dp))
            if (reply != null) MessageQuote(reply, onClick = onReplyClick, modifier = Modifier.padding(bottom = 6.dp))
            Box {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.bubbleClicks(onClick, onLongClick)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(circleColor)
                            .then(
                                if (transfer is TransferState.Running && onCancel != null) {
                                    Modifier.clickable(onClick = onCancel)
                                } else {
                                    Modifier
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when (transfer) {
                            is TransferState.Running -> {
                                ProgressRing(transfer.progress, Modifier.size(40.dp))
                                if (onCancel != null) {
                                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.media_cancel), tint = Color.White)
                                }
                            }
                            TransferState.Remote -> Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = stringResource(R.string.media_download),
                                tint = Color.White
                            )
                            TransferState.Failed -> Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
                            TransferState.Idle -> Icon(
                                Icons.AutoMirrored.Filled.InsertDriveFile,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.widthIn(min = 120.dp)) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            maxLines = 1,
                            // Keeps the extension visible: "Лабораторн…3.pdf".
                            overflow = TextOverflow.MiddleEllipsis
                        )
                        Text(
                            text = buildAnnotatedString {
                                append(sizeText)
                                withStyle(SpanStyle(color = Color.Transparent, fontSize = MetaSize)) {
                                    append("    " + bubbleMetaText(timestamp, isEdited, isSent))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = metaColor,
                            maxLines = 1
                        )
                    }
                }
                BubbleMeta(
                    text = bubbleMetaText(timestamp, isEdited, isSent = false),
                    isSent = isSent,
                    status = status,
                    color = metaColor,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 1.dp)
                )
            }
        }
    }
}

@Composable
private fun BubbleContainer(isSent: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = if (isSent) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = if (isSent) SentBubbleShape else ReceivedBubbleShape,
            color = if (isSent) SentBubbleColor else ReceivedBubbleColor
        ) {
            Box(Modifier.padding(3.dp)) { content() }
        }
    }
}

@Composable
private fun SenderName(name: String, color: Color, modifier: Modifier) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleSmall,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}

/** A dark circle with a progress ring and ✕: cancels the transfer. */
@Composable
private fun TransferButton(transfer: TransferState.Running, onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundOverlayIcon(modifier.clickable(onClick = onClick)) {
        ProgressRing(transfer.progress, Modifier.size(42.dp))
        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.media_cancel), tint = Color.White)
    }
}

@Composable
private fun ProgressRing(progress: Float?, modifier: Modifier) {
    if (progress == null) {
        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = modifier)
    } else {
        // Never an empty ring: "something is happening" from the first moment.
        CircularProgressIndicator(
            progress = { progress.coerceIn(0.03f, 1f) },
            color = Color.White,
            trackColor = Color.Transparent,
            strokeWidth = 2.dp,
            modifier = modifier
        )
    }
}

@Composable
private fun RoundOverlayIcon(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MediaOverlay),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun OverlayPill(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontSize = MetaSize,
        color = Color.White,
        modifier = modifier
            .background(MediaOverlay, RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** Keeps the picture's proportions within Telegram-like bounds (very tall or wide ones get cropped). */
private fun mediaSize(width: Int?, height: Int?): Pair<Dp, Dp> {
    if (width == null || height == null || width <= 0 || height <= 0) return MaxMediaWidth to MaxMediaWidth
    val ratio = width.toFloat() / height
    return if (ratio >= MaxMediaWidth / MaxMediaHeight) {
        MaxMediaWidth to (MaxMediaWidth / ratio).coerceAtLeast(MinMediaHeight)
    } else {
        (MaxMediaHeight * ratio).coerceAtLeast(MinMediaWidth) to MaxMediaHeight
    }
}

@Preview(showBackground = true)
@Composable
fun MediaBubblePreview() {
    OverGramTheme {
        Surface(color = ChatWallpaper) {
            Column(Modifier.padding(vertical = 16.dp)) {
                VisualMediaBubble(model = null, isVideo = false, width = 1600, height = 1200, timestamp = "16:45", isSent = true, status = BubbleStatus.Read)
                VisualMediaBubble(
                    model = null, isVideo = true, width = 720, height = 1280, timestamp = "16:46", isSent = false,
                    durationText = "0:42", caption = "Смотри что было вчера", senderName = "Ada"
                )
                VisualMediaBubble(
                    model = null, isVideo = false, width = 1000, height = 1000, timestamp = "16:47", isSent = true,
                    status = BubbleStatus.Sending, transfer = TransferState.Running(0.4f)
                )
                FileBubble(fileName = "Лабораторная работа №3.pdf", sizeText = "1.2 MB", timestamp = "16:48", isSent = false, transfer = TransferState.Remote)
                FileBubble(fileName = "report.docx", sizeText = "0.4 / 1.1 MB", timestamp = "16:49", isSent = true, status = BubbleStatus.Sending, transfer = TransferState.Running(0.36f))
            }
        }
    }
}
