package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.ReceivedBubbleColor
import com.example.overgram.ui.theme.TextPrimary

/** What a reply quotes: whose message ([title]) and a line of it ([text]). */
data class QuoteContent(
    val title: String,
    val text: String,
    /** The quoted sender's name color (Telegram colors quotes by author). */
    val color: Color = Accent
)

private val QuoteShape = RoundedCornerShape(4.dp)

/**
 * A quoted message, Telegram style: a colored bar, the author in that color and one line of the
 * message on a faint tint of it. Used inside reply bubbles and on the strip above the input.
 */
@Composable
fun MessageQuote(
    quote: QuoteContent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .background(quote.color.copy(alpha = QUOTE_TINT_ALPHA), QuoteShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(quote.color, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
        )
        Column(Modifier.padding(start = 6.dp, end = 8.dp, top = 3.dp, bottom = 3.dp)) {
            Text(
                text = quote.title,
                style = MaterialTheme.typography.titleSmall,
                color = quote.color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = quote.text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val QUOTE_TINT_ALPHA = 0.12f

@Preview(showBackground = true)
@Composable
fun MessageQuotePreview() {
    OverGramTheme {
        Surface(color = ReceivedBubbleColor) {
            MessageQuote(
                quote = QuoteContent("Ada Lovelace", "Давай завтра в 10, у входа в библиотеку"),
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}
