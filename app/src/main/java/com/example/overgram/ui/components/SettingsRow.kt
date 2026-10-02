package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.IconSecondary
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.SectionGap
import com.example.overgram.ui.theme.SectionHeader
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/** Where row text starts (and dividers begin), as in Telegram's settings: icon column + gap. */
private val TextStart = 72.dp

/**
 * A Telegram settings row: grey glyph, title, optional second line, hairline divider under the
 * text. No card, no chevron — sections and dividers carry the structure.
 *
 * @param icon Null for value rows (e.g. the phone number under "Account"), still aligned to text.
 * @param subtitle Second line (a value or an explanation).
 * @param titleColor E.g. red for "Log out".
 * @param trailing E.g. a progress indicator.
 * @param showDivider False for the last row of a section.
 * @param onClick Null makes it a non-interactive info row.
 */
@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    titleColor: Color = TextPrimary,
    iconTint: Color = IconSecondary,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = if (subtitle != null) 64.dp else 52.dp)
                .padding(start = 22.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(TextStart - 22.dp - 24.dp))
            } else {
                Spacer(Modifier.width(TextStart - 22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing?.let {
                Spacer(Modifier.width(8.dp))
                it()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = DividerColor,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = TextStart)
            )
        }
    }
}

/** Blue section title ("Account", "Settings") on the list background, as in Telegram. */
@Composable
fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = SectionHeader,
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark)
            .padding(start = 22.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)
    )
}

/** The darker gap between sections. */
@Composable
fun SettingsSectionGap(modifier: Modifier = Modifier) {
    Spacer(
        modifier
            .fillMaxWidth()
            .heightIn(min = 12.dp)
            .background(SectionGap)
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsRowPreview() {
    OverGramTheme {
        Column {
            SettingsSectionHeader("Account")
            SettingsRow(title = "+998 90 000 00 01", subtitle = "Phone", onClick = {})
            SettingsRow(title = "@tadashi", subtitle = "Username", showDivider = false, onClick = {})
            SettingsSectionGap()
            SettingsRow(icon = Icons.Outlined.Person, title = "Edit profile", onClick = {})
            SettingsRow(icon = Icons.Outlined.Info, title = "Version 1.0", showDivider = false)
        }
    }
}
