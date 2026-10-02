package com.example.overgram.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.overgram.ui.theme.LocalHazeState
import com.example.overgram.ui.theme.glassSource
import com.example.overgram.ui.theme.rememberGlassState

/**
 * A [Scaffold] whose bars are glass: the backdrop and [content] form the layer the bars blur.
 *
 * Content is laid out full-screen, behind the bars. For lists, pass the given padding as
 * `contentPadding` (not as a padding modifier) so items scroll under the bars and show through;
 * for static content a padding modifier is fine.
 */
@Composable
fun GlassScreen(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    backdropIntensity: Float = 1f,
    content: @Composable (PaddingValues) -> Unit
) {
    val hazeState = rememberGlassState()
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = topBar,
            bottomBar = bottomBar,
            floatingActionButton = floatingActionButton,
            snackbarHost = snackbarHost
        ) { innerPadding ->
            Box(
                Modifier
                    .fillMaxSize()
                    .glassSource(hazeState)
            ) {
                GlassBackdrop(intensity = backdropIntensity)
                content(innerPadding)
            }
        }
    }
}
