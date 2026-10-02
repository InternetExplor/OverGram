package com.example.overgram.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * OverGram dark violet Material 3 theme.
 * Configured as dark-only as required for the OverGram dark violet style.
 */
@Composable
fun OverGramTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = OverGramDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = SurfaceDark.toArgb()
                window.navigationBarColor = BackgroundDark.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = OverGramTypography,
        shapes = OverGramShapes,
        content = content
    )
}
