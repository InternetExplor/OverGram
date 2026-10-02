package com.example.overgram.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Message bubble and UI component shapes for OverGram messenger.
 */
val CardShape = RoundedCornerShape(16.dp)
val AvatarShape = CircleShape
val UnreadBadgeShape = CircleShape

// Telegram bubbles: 16 dp corners, a tighter corner where the tail would be
val SentBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 6.dp
)

val ReceivedBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 6.dp,
    bottomEnd = 16.dp
)

val InputBarShape = RoundedCornerShape(24.dp)
val IconContainerShape = RoundedCornerShape(12.dp)

val OverGramShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = IconContainerShape,
    large = CardShape,
    extraLarge = InputBarShape
)
