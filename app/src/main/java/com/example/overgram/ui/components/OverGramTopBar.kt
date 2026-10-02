package com.example.overgram.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.example.overgram.R
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OnlineStatusViolet
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * Custom TopBar for OverGram screens.
 *
 * @param title Screen title or contact name.
 * @param subtitle Optional subtitle, e.g., "online" status or contact info.
 * @param onBackClick Navigation back click listener. If provided, back button is shown.
 * @param leadContent Optional composable content displayed before title (e.g. Chat Avatar).
 * @param onTitleClick Makes the title clickable, e.g. to open chat info.
 * @param highlightSubtitle Accent colour for the subtitle ("online", "typing…").
 * @param actions Action icons displayed on the right side of the top bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverGramTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    leadContent: (@Composable () -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    highlightSubtitle: Boolean = subtitle.equals("online", ignoreCase = true),
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column(
                modifier = if (onTitleClick != null) Modifier.clickable(onClick = onTitleClick) else Modifier
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highlightSubtitle) OnlineStatusViolet else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = {
            // The slot is a Box: without the Row the avatar is drawn on top of the back arrow.
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = TextPrimary
                        )
                    }
                }
                if (leadContent != null) {
                    if (onBackClick == null) {
                        Spacer(modifier = Modifier.width(Dimens.SpacingLg))
                    }
                    leadContent()
                    Spacer(modifier = Modifier.width(Dimens.SpacingMd))
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BackgroundDark,
            scrolledContainerColor = BackgroundDark
        )
    )
}

@Preview(showBackground = true)
@Composable
fun OverGramTopBarMainPreview() {
    OverGramTheme {
        OverGramTopBar(
            title = "OverGram",
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextPrimary)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OverGramTopBarChatPreview() {
    OverGramTheme {
        OverGramTopBar(
            title = "Озодбек",
            subtitle = "online",
            onBackClick = {},
            leadContent = {
                Avatar(name = "Озодбек", size = Dimens.AvatarSmall)
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Call, contentDescription = "Call", tint = TextPrimary)
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = TextPrimary)
                }
            }
        )
    }
}
