package com.example.overgram.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Telegram's "Dark Blue" theme (Telegram for Android, assets/darkblue.attheme), mapped onto
 * OverGram's tokens. The attheme key each value comes from is noted alongside.
 */

/** Chat list / screen background. `windowBackgroundWhite` */
val BackgroundDark = Color(0xFF1D2733)

/** Top app bar, input panel, dialogs. `actionBarDefault` */
val SurfaceDark = Color(0xFF242D39)

/** Menus, raised sheets. `actionBarDefaultSubmenuBackground` */
val SurfaceElevatedDark = Color(0xFF283848)

/** Gaps between settings sections. `windowBackgroundGray` */
val SectionGap = Color(0xFF151E27)

/** Plain chat background behind the bubbles. `chat_wallpaper` */
val ChatWallpaper = Color(0xFF151E27)

/** FAB, buttons, switches. `chats_actionBackground` */
val Accent = Color(0xFF5FA3DE)

/** Links, blue text, unread counter, read ticks in the list. `windowBackgroundWhiteBlueText` */
val AccentBright = Color(0xFF64B5EF)

/** Section headers ("Account", "Settings"). `windowBackgroundWhiteBlueHeader` */
val SectionHeader = Color(0xFF79C4FC)

/** `chat_outBubble` / `chat_inBubble` */
val SentBubbleColor = Color(0xFF3E618A)
val ReceivedBubbleColor = Color(0xFF232E3B)

/** Time + ticks inside bubbles. `chat_outTimeText`, `chat_inTimeText`, `chat_outSentCheck` */
val OutgoingMeta = Color(0xFF8FBCDF)
val IncomingMeta = Color(0xFF8091A0)
val OutgoingCheck = Color(0xFF87C7FF)

/** `chat_messagePanelBackground`, `chat_messagePanelSend`, `chat_messagePanelIcons` (≈ opaque) */
val InputPanel = Color(0xFF212D3B)
val SendBlue = Color(0xFF229AF0)
val InputPanelIcon = Color(0xFF7F8E9D)

/** Date pills / system notes over the wallpaper. `chat_serviceBackground` */
val ServiceBackground = Color(0x82354251)

/** `chats_name`, `windowBackgroundWhiteBlackText` */
val TextPrimary = Color(0xFFFFFFFF)
val ChatName = Color(0xFFE9EEF4)

/** Header subtitle ("last seen …"), white at ~55 % over the bar. `actionBarDefaultSubtitle` */
val HeaderSubtitle = Color(0xFF9AA8B6)

/** `chats_message`, `windowBackgroundWhiteGrayText` */
val TextSecondary = Color(0xFF7D8B99)

/** `chats_date` */
val ListDate = Color(0xFF737F8B)

/** `chats_unreadCounter`, `chats_unreadCounterMuted`, `chats_muteIcon` */
val UnreadCounter = Color(0xFF64B5EF)
val UnreadCounterMuted = Color(0xFF3E5263)
val MutedIcon = Color(0xFF4E5F6A)

/** "online" in headers and the avatar dot. `chat_status` */
val OnlineBlue = Color(0xFF73BBF7)

val ErrorRed = Color(0xFFE86C6A)
val OnErrorRed = Color(0xFFFFFFFF)

/** Hairline between list rows (Telegram's `divider` is translucent black). */
val DividerColor = Color(0x59000000)
val IconSecondary = Color(0xFF8B99A7)

/**
 * Material 3 dark scheme from the Telegram palette, so stock M3 components (dialogs, menus,
 * text fields, snackbars) pick the right colors without per-call overrides.
 */
val OverGramDarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    primaryContainer = SentBubbleColor,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBright,
    onSecondary = TextPrimary,
    secondaryContainer = ReceivedBubbleColor,
    onSecondaryContainer = TextPrimary,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = BackgroundDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceElevatedDark,
    surfaceContainerHighest = SurfaceElevatedDark,
    surfaceContainerLow = BackgroundDark,
    inverseSurface = Color(0xFFE9EEF4),
    inverseOnSurface = BackgroundDark,
    outline = Color(0xFF3A4757),
    outlineVariant = DividerColor,
    error = ErrorRed,
    onError = OnErrorRed
)
