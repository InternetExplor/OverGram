package com.example.overgram.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.overgram.R
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextSecondary

/** The home screen's sections. (No Calls tab: the server has no calls API.) */
enum class OverGramBottomTab(@StringRes val title: Int, val icon: ImageVector) {
    Chats(R.string.tab_chats, Icons.AutoMirrored.Filled.Comment),
    Contacts(R.string.tab_contacts, Icons.Default.People),
    Settings(R.string.tab_settings, Icons.Default.Settings)
}

/**
 * Main bottom navigation bar: Chats, Contacts, Settings.
 *
 * @param selectedTab The currently active tab.
 * @param onTabSelected Callback invoked when user selects a tab.
 */
@Composable
fun OverGramBottomBar(
    selectedTab: OverGramBottomTab,
    onTabSelected: (OverGramBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BackgroundDark,
        contentColor = TextSecondary
    ) {
        OverGramBottomTab.entries.forEach { tab ->
            val title = stringResource(tab.title)
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = title) },
                label = { Text(text = title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryViolet,
                    selectedTextColor = PrimaryViolet,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SurfaceDark
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun OverGramBottomBarPreview() {
    OverGramTheme {
        OverGramBottomBar(
            selectedTab = OverGramBottomTab.Chats,
            onTabSelected = {}
        )
    }
}
