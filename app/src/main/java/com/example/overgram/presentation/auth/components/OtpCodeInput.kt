package com.example.overgram.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.IconContainerShape
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextPrimary

/**
 * OTP entry rendered as [length] individual boxes.
 *
 * Backed by a single text field so typing moves focus to the next box, backspace moves back,
 * and pasting / SMS autofill of the whole code works, which per-box fields handle poorly.
 */
@Composable
fun OtpCodeInput(
    code: String,
    onCodeChange: (String) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    length: Int = 5,
    isError: Boolean = false,
    enabled: Boolean = true,
    autoFocus: Boolean = true,
    onDone: () -> Unit = {}
) {
    val description = contentDescription
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }

    BasicTextField(
        value = code,
        onValueChange = { onCodeChange(it.filter(Char::isDigit).take(length)) },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .semantics { this.contentDescription = description },
        // The field's own text is never drawn; the boxes below render the code instead.
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm, Alignment.CenterHorizontally)
            ) {
                repeat(length) { index ->
                    val isActive = isFocused && index == code.length.coerceAtMost(length - 1)
                    OtpBox(
                        char = code.getOrNull(index),
                        isActive = isActive,
                        isError = isError
                    )
                }
            }
        }
    )
}

@Composable
private fun RowScope.OtpBox(char: Char?, isActive: Boolean, isError: Boolean) {
    val borderColor = when {
        isError -> ErrorRed
        isActive -> Accent
        else -> DividerColor
    }
    Box(
        modifier = Modifier
            // Shrinks to fit 6 boxes on narrow screens, capped at 52dp on wider ones.
            .weight(1f, fill = false)
            .widthIn(max = 52.dp)
            .fillMaxWidth()
            .height(60.dp)
            .background(SurfaceDark, IconContainerShape)
            .border(if (isActive || isError) 2.dp else 1.dp, borderColor, IconContainerShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = char?.toString().orEmpty(),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
    }
}
