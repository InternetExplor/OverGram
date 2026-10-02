package com.example.overgram.presentation.auth

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Displays the 9-digit local part as "90 123 45 67" while the underlying value stays digits only.
 */
object PhoneVisualTransformation : VisualTransformation {

    /** Indices in the raw digits before which a space is shown. */
    private val breaks = intArrayOf(2, 5, 7)

    fun format(digits: String): String = buildString {
        digits.forEachIndexed { index, char ->
            if (index in breaks) append(' ')
            append(char)
        }
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = format(raw)

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int =
                (offset + breaks.count { it < offset && it < raw.length }).coerceAtMost(formatted.length)

            override fun transformedToOriginal(offset: Int): Int {
                // Space positions in the formatted string: break index + number of earlier spaces.
                val spaces = breaks.filter { it < raw.length }.mapIndexed { i, b -> b + i }
                return (offset - spaces.count { it < offset }).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
