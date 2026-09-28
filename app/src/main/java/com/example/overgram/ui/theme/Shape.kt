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

// Message bubble shapes (~18dp with smaller corner on sender/receiver side)
val SentBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 18.dp,
    bottomEnd = 4.dp
)

val ReceivedBubbleShape = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 18.dp,
    bottomStart = 4.dp,
    bottomEnd = 18.dp
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
