package com.example.overgram.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import kotlin.math.max

/**
 * The app's backdrop: near-black with a few soft glows. Glass needs something with color and
 * light behind it — over a flat color a blur is invisible. Kept dim so content stays the focus;
 * static (no animation) to cost nothing per frame.
 */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier, intensity: Float = 1f) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val extent = max(size.width, size.height)
            fun glow(color: Color, center: Offset, radius: Float) = drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = color.alpha * intensity), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            glow(PrimaryViolet.copy(alpha = 0.30f), Offset(size.width * 0.05f, size.height * 0.08f), extent * 0.55f)
            glow(Color(0xFF3D5AFE).copy(alpha = 0.18f), Offset(size.width * 1.0f, size.height * 0.45f), extent * 0.50f)
            glow(Color(0xFFB04BFF).copy(alpha = 0.14f), Offset(size.width * 0.15f, size.height * 0.95f), extent * 0.45f)
        }
    }
}

@Preview(showBackground = true, heightDp = 640)
@Composable
fun GlassBackdropPreview() {
    OverGramTheme { GlassBackdrop() }
}
