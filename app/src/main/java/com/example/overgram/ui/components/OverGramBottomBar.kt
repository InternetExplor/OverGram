package com.example.overgram.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.R
import com.example.overgram.ui.theme.LocalHazeState
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.PrimaryVioletLight
import com.example.overgram.ui.theme.TextSecondary
import com.example.overgram.ui.theme.glass

/** The home screen's sections. (No Calls tab: the server has no calls API.) */
enum class OverGramBottomTab(@StringRes val title: Int, val icon: ImageVector) {
    Chats(R.string.tab_chats, Icons.AutoMirrored.Filled.Comment),
    Contacts(R.string.tab_contacts, Icons.Default.People),
    Settings(R.string.tab_settings, Icons.Default.Settings)
}

private val CapsuleShape = RoundedCornerShape(percent = 50)
private val BarHeight = 64.dp

/**
 * Floating glass tab bar: a capsule above the content (which scrolls underneath and shows
 * through, blurred), with a selection pill that slides between tabs on a soft spring.
 * Items are full-height, so each touch target is ≥ 48 dp.
 */
@Composable
fun OverGramBottomBar(
    selectedTab: OverGramBottomTab,
    onTabSelected: (OverGramBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = OverGramBottomTab.entries
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .height(BarHeight)
                .glass(LocalHazeState.current, CapsuleShape)
                .padding(5.dp)
        ) {
            val itemWidth = maxWidth / tabs.size
            val indicatorOffset by animateDpAsState(
                targetValue = itemWidth * selectedTab.ordinal,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
                label = "tab indicator"
            )
            // The sliding "drop" behind the selected tab.
            Box(
                modifier = Modifier
                    .offset(x = indicatorOffset)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            listOf(PrimaryViolet.copy(alpha = 0.38f), PrimaryViolet.copy(alpha = 0.22f))
                        ),
                        CapsuleShape
                    )
            )
            Row(Modifier.fillMaxSize()) {
                tabs.forEach { tab ->
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
}

@Composable
private fun TabItem(
    tab: OverGramBottomTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(tab.title)
    val color by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        label = "tab color"
    )
    Column(
        modifier = modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 36.dp, color = PrimaryVioletLight)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = tab.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF12121F)
@Composable
fun OverGramBottomBarPreview() {
    OverGramTheme {
        Box {
            GlassBackdrop(Modifier.height(120.dp))
            OverGramBottomBar(selectedTab = OverGramBottomTab.Contacts, onTabSelected = {})
        }
    }
}

