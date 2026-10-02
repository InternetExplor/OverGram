package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OnlineBlue
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.TextPrimary
import kotlin.math.abs

/**
 * Circle Avatar with Coil image loading, initials fallback, and optional online status dot.
 *
 * @param name Full name or chat title, used for initials fallback.
 * @param imageUrl Optional URL for profile image.
 * @param size Avatar diameter (defaults to Dimens.AvatarMedium = 48dp).
 * @param isOnline Whether to show the online status indicator dot.
 * @param onClick Optional click callback.
 */
@Composable
fun Avatar(
    name: String,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    size: Dp = Dimens.AvatarMedium,
    isOnline: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .size(size)
            .then(clickModifier),
        contentAlignment = Alignment.Center
    ) {
        // Initials always sit underneath: they show while the photo loads, and stay if it
        // can't be loaded (no access, offline, not ready).
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(avatarBrush(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getInitials(name),
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                fontSize = (size.value * 0.38f).sp
            )
        }
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        }

        if (isOnline) {
            val dotSize = size * 0.26f
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp)
                    .background(OnlineBlue, CircleShape)
                    .border(
                        width = Dimens.OnlineDotBorder,
                        color = BackgroundDark,
                        shape = CircleShape
                    )
            )
        }
    }
}

private fun getInitials(name: String): String {
    if (name.isBlank()) return "?"
    val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

/**
 * Telegram's seven avatar gradients (top to bottom), picked by name like Telegram picks by id,
 * so a person keeps the same color everywhere.
 */
private val AvatarGradients = listOf(
    Color(0xFFFF845E) to Color(0xFFD45246), // red
    Color(0xFFFEBB5B) to Color(0xFFF68136), // orange
    Color(0xFFB694F9) to Color(0xFF6C61DF), // violet
    Color(0xFF9AD164) to Color(0xFF46BA43), // green
    Color(0xFF53EDD6) to Color(0xFF28C9B7), // cyan
    Color(0xFF5CAFFA) to Color(0xFF408ACF), // blue
    Color(0xFFFF8AAC) to Color(0xFFD95574)  // pink
)

private fun avatarBrush(name: String): Brush {
    val (top, bottom) = AvatarGradients[abs(name.hashCode()) % AvatarGradients.size]
    return Brush.verticalGradient(listOf(top, bottom))
}

@Preview(showBackground = true)
@Composable
fun AvatarPreview() {
    OverGramTheme {
        Row {
            Avatar(name = "ÐÐ·Ð¾Ð´Ð±ÐµÐº", isOnline = true)
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(name = "ChatGPT", isOnline = false)
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(name = "ÐÐ°ÑÐ°", size = Dimens.AvatarLarge, isOnline = true)
        }
    }
}
