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
import com.example.overgram.ui.theme.OnlineStatusViolet
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceElevatedDark
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
                .background(getAvatarBackgroundColor(name)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getInitials(name),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.4f).sp
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
                    .background(OnlineStatusViolet, CircleShape)
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

private fun getAvatarBackgroundColor(name: String): Color {
    val colors = listOf(
        PrimaryViolet,
        Color(0xFF5C6BC0),
        Color(0xFF7E57C2),
        Color(0xFFAB47BC),
        SurfaceElevatedDark
    )
    val index = abs(name.hashCode()) % colors.size
    return colors[index]
}

@Preview(showBackground = true)
@Composable
fun AvatarPreview() {
    OverGramTheme {
        Row {
            Avatar(name = "Озодбек", isOnline = true)
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(name = "ChatGPT", isOnline = false)
            Spacer(modifier = Modifier.width(8.dp))
            Avatar(name = "Даша", size = Dimens.AvatarLarge, isOnline = true)
        }
    }
}
