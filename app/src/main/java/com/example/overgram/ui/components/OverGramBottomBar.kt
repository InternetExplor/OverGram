package com.example.overgram.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextSecondary

/**
 * Bottom navigation item model for OverGram bottom bar.
 */
data class OverGramBottomTab(
    val title: String,
    val icon: ImageVector
)

val OverGramBottomTabs = listOf(
    OverGramBottomTab("Чаты", Icons.AutoMirrored.Filled.Comment),
    OverGramBottomTab("Контакты", Icons.Default.People),
    OverGramBottomTab("Звонки", Icons.Default.Call),
    OverGramBottomTab("Настройки", Icons.Default.Settings)
)

/**
 * Main bottom navigation bar with 4 tabs: Chats, Contacts, Calls, Settings.
 *
 * @param selectedTab Index of the currently active tab (0..3).
 * @param onTabSelected Callback invoked when user selects a tab.
 */
@Composable
fun OverGramBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = BackgroundDark,
        contentColor = TextSecondary
    ) {
        OverGramBottomTabs.forEachIndexed { index, tab ->
            val isSelected = index == selectedTab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(text = tab.title)
                },
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
            selectedTab = 0,
            onTabSelected = {}
        )
    }
}
