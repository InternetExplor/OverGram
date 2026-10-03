package com.example.overgram.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.overgram.R
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.IconSecondary
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.SurfaceDark

/** The home screen's sections. (No Calls tab: the server has no calls API.) */
enum class OverGramBottomTab(
    @StringRes val title: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    Contacts(R.string.tab_contacts, Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle),
    Chats(R.string.tab_chats, Icons.AutoMirrored.Outlined.Chat, Icons.AutoMirrored.Filled.Chat),
    Settings(R.string.tab_settings, Icons.Outlined.Settings, Icons.Filled.Settings)
}

/**
 * Telegram-style tab bar: the header color, a hairline on top, and the selected tab in the
 * accent blue with a filled icon (others outlined, grey). Contacts · Chats · Settings, as in
 * Telegram. Each item spans the full bar height, so touch targets stay ≥ 48 dp.
 */
@Composable
fun OverGramBottomBar(
    selectedTab: OverGramBottomTab,
    onTabSelected: (OverGramBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .navigationBarsPadding()
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            OverGramBottomTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: OverGramBottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(if (selected) Accent else IconSecondary, label = "tab color")
    Column(
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) tab.selectedIcon else tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = stringResource(tab.title),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun OverGramBottomBarPreview() {
    OverGramTheme {
        OverGramBottomBar(selectedTab = OverGramBottomTab.Chats, onTabSelected = {})
    }
}
