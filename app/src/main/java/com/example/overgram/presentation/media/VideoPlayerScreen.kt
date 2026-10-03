package com.example.overgram.presentation.media

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.R
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.presentation.common.rememberMediaEntryPoint
import java.io.File

/**
 * Full-screen playback. A video we have on the device plays from the file; otherwise it streams
 * from `GET /v1/media/{id}` through the app's OkHttp client (bearer token, refresh) — the
 * endpoint serves Range requests, so seeking doesn't download the whole file.
 */
data class VideoPlayerScreen(val mediaId: String?, val localPath: String?) : Screen {

    override val key: ScreenKey = "video:${mediaId ?: localPath}"

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val media = rememberMediaEntryPoint() ?: return
        val player = remember {
            val upstream = OkHttpDataSource.Factory(media.okHttpClient())
            val dataSources = DefaultDataSource.Factory(context, upstream)
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSources))
                .build()
                .apply {
                    val local = localPath?.let(::File)?.takeIf { it.exists() } ?: media.localMediaStore().fileFor(mediaId)
                    val uri = local?.let(Uri::fromFile) ?: mediaUrl(mediaId)?.let(Uri::parse)
                    if (uri != null) setMediaItem(MediaItem.fromUri(uri))
                    prepare()
                    playWhenReady = true
                }
        }
        DisposableEffect(player) {
            onDispose { player.release() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                factory = { viewContext ->
                    PlayerView(viewContext).apply {
                        this.player = player
                        setShowNextButton(false)
                        setShowPreviousButton(false)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            IconButton(
                onClick = { navigator.pop() },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.media_viewer_back),
                    tint = Color.White
                )
            }
        }
    }
}
