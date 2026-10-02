package com.example.overgram.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * "Liquid glass": surfaces that blur and tint what scrolls underneath them, with a thin light
 * edge. Real blur needs API 31+; below that Haze draws [GlassTokens.fallback] instead, which is
 * opaque enough to keep text readable.
 *
 * Contrast: white text needs ≥ 4.5:1 on the glass, so the tint stays dense (≈ 70 %) — the
 * blur is there for depth, not for see-through legibility of what's behind.
 */
object GlassTokens {
    /** Main tint for bars. Dense enough for AA contrast over any message content. */
    val Tint = Color(0xFF16162A).copy(alpha = 0.72f)

    /** Lighter tint for small chips (date separators, system notes) over the wallpaper. */
    val ChipTint = Color(0xFF232340).copy(alpha = 0.55f)

    /** What pre-Android-12 devices get instead of a blur. */
    val Fallback = Color(0xFF16162A).copy(alpha = 0.95f)

    val BlurRadius: Dp = 28.dp

    /** Specular edge: brighter at the top, fading out — the "liquid" rim. */
    val EdgeHighlight = Brush.verticalGradient(
        0f to Color.White.copy(alpha = 0.22f),
        0.5f to Color.White.copy(alpha = 0.06f),
        1f to Color.White.copy(alpha = 0.10f)
    )

    /** Hairline under/over full-width bars. */
    val Hairline = Color.White.copy(alpha = 0.07f)
}

fun glassStyle(tint: Color = GlassTokens.Tint): HazeStyle = HazeStyle(
    backgroundColor = BackgroundDark,
    tint = HazeTint(tint),
    blurRadius = GlassTokens.BlurRadius,
    noiseFactor = 0.06f,
    fallbackTint = HazeTint(GlassTokens.Fallback)
)

/**
 * The [HazeState] of the screen being drawn: bars that read it turn to glass. Screens without
 * it get the regular solid surfaces, so glass can be adopted screen by screen.
 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

/** Marks this content as what the screen's glass surfaces blur. */
fun Modifier.glassSource(state: HazeState?): Modifier =
    if (state != null) hazeSource(state) else this

/**
 * A glass surface: blur + tint of what's behind (or the solid fallback without a [state]),
 * clipped to [shape], with a light rim when [edge] is set.
 */
fun Modifier.glass(
    state: HazeState?,
    shape: Shape = RectangleShape,
    tint: Color = GlassTokens.Tint,
    edge: Boolean = true
): Modifier {
    val clipped = clip(shape)
    val surface = if (state != null) {
        clipped.hazeEffect(state = state, style = glassStyle(tint))
    } else {
        clipped.background(GlassTokens.Fallback, shape)
    }
    return if (edge) surface.border(1.dp, GlassTokens.EdgeHighlight, shape) else surface
}

/** Translucent glass without blur, for many small elements (bubbles, chips) where blur is too costly. */
fun Modifier.frosted(shape: Shape, tint: Color = GlassTokens.ChipTint): Modifier =
    clip(shape)
        .background(tint, shape)
        .border(1.dp, GlassTokens.EdgeHighlight, shape)

/** A 1 px light line along the bottom (top bars) or top (bottom bars) edge. */
fun Modifier.hairline(atBottom: Boolean): Modifier = drawWithContent {
    drawContent()
    val y = if (atBottom) size.height - 0.5f else 0.5f
    drawLine(GlassTokens.Hairline, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
}

/** The glass screen scaffolding needs a state per screen. */
@Composable
fun rememberGlassState(): HazeState = dev.chrisbanes.haze.rememberHazeState()
