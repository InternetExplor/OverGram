package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme

/**
 * Grid of common emoji shown in place of the keyboard. The system keyboard still has the
 * full set; this covers the quick picks.
 */
@Composable
fun EmojiPanel(
    onEmojiSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 44.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        contentPadding = PaddingValues(horizontal = Dimens.SpacingSm, vertical = Dimens.SpacingXs)
    ) {
        items(EMOJI) { emoji ->
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .clickable { onEmojiSelected(emoji) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 26.sp)
            }
        }
    }
}

private val EMOJI = listOf(
    "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰",
    "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥳", "😏", "😒",
    "😞", "😔", "😟", "😕", "🙁", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤯",
    "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑",
    "😬", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐", "🥴", "🤢", "🤮",
    "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿", "🤡", "💩", "👻", "💀", "👽", "🤖", "🎃", "😺",
    "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "👏", "🙌", "👐", "🤝", "🙏", "💪", "👋", "🤙", "👀",
    "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💔", "💯", "🔥", "✨", "⭐", "🎉", "🎁", "✅",
    "❌", "❓", "❗", "💤", "☕", "🍕", "🍔", "🍟", "🍩", "🍰", "🍺", "🍷", "⚽", "🎮", "🎧", "📚"
)

@Preview(showBackground = true)
@Composable
fun EmojiPanelPreview() {
    OverGramTheme {
        Box(Modifier.background(BackgroundDark)) {
            EmojiPanel(onEmojiSelected = {})
        }
    }
}
