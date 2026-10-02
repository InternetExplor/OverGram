package com.example.overgram.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/**
 * OverGram Material 3 Dark Violet Color Palette.
 */
val BackgroundDark = Color(0xFF12121F)
val SurfaceDark = Color(0xFF1A1A2E)
val SurfaceElevatedDark = Color(0xFF232336)

val PrimaryViolet = Color(0xFF7C5CFC)
val PrimaryVioletLight = Color(0xFF8B6FF5)

val SentBubbleColor = Color(0xFF7C5CFC)
val ReceivedBubbleColor = Color(0xFF2A2A3D)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0B8)

val OnlineStatusViolet = Color(0xFF8B6FF5)
val ErrorRed = Color(0xFFE53935)
val OnErrorRed = Color(0xFFFFFFFF)

val DividerColor = Color(0xFF252538)
val IconSecondary = Color(0xFFA0A0B8)

/**
 * Material 3 Dark Color Scheme mapping for OverGram messenger.
 */
val OverGramDarkColorScheme = darkColorScheme(
    primary = PrimaryViolet,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryVioletLight,
    onPrimaryContainer = TextPrimary,
    secondary = PrimaryVioletLight,
    onSecondary = TextPrimary,
    secondaryContainer = ReceivedBubbleColor,
    onSecondaryContainer = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = OnErrorRed
)
