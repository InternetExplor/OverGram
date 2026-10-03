package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.UnreadCounter
import com.example.overgram.ui.theme.TextPrimary

/**
 * Unread counter pill (Telegram `chats_unreadCounter`; grey for muted chats).
 *
 * @param count Number of unread messages to display.
 * @param backgroundColor Background color of the badge (defaults to Accent).
 * @param textColor Text color inside the badge.
 */
@Composable
fun UnreadBadge(
    count: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = UnreadCounter,
    textColor: Color = TextPrimary
) {
    if (count <= 0) return

    val text = if (count > 99) "99+" else count.toString()

    Box(
        modifier = modifier
            .sizeIn(minWidth = Dimens.UnreadBadgeMinSize, minHeight = Dimens.UnreadBadgeMinSize)
            .clip(CircleShape)
            .background(backgroundColor)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun UnreadBadgePreview() {
    OverGramTheme {
        Row {
            UnreadBadge(count = 1)
            Spacer(modifier = Modifier.width(8.dp))
            UnreadBadge(count = 3)
            Spacer(modifier = Modifier.width(8.dp))
            UnreadBadge(count = 42)
            Spacer(modifier = Modifier.width(8.dp))
            UnreadBadge(count = 120)
        }
    }
}
