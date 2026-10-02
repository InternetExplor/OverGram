package com.example.overgram.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.CardShape
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.IconContainerShape
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * Settings menu row with a rounded icon container, title, subtitle, and navigation chevron.
 *
 * @param icon Vector icon for the option.
 * @param title Settings option title.
 * @param subtitle Optional description text under the title.
 * @param iconBackgroundColor Background color for the rounded icon container.
 * @param iconTintColor Tint color for the icon.
 * @param onClick Click listener callback.
 */
@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconBackgroundColor: Color = PrimaryViolet.copy(alpha = 0.2f),
    iconTintColor: Color = PrimaryViolet,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onClick),
        color = SurfaceDark,
        shape = CardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.ScreenPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.SettingsIconContainerSize)
                    .clip(IconContainerShape)
                    .background(iconBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingLg))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!subtitle.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimens.SpacingSm))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open",
                tint = TextSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsRowPreview() {
    OverGramTheme {
        Surface(color = BackgroundDark) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Уведомления",
                    subtitle = "Звуки, вибрация, предпросмотр"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsRow(
                    icon = Icons.Default.Lock,
                    title = "Конфиденциальность",
                    subtitle = "Блокировка, чат, звонки"
                )
                Spacer(modifier = Modifier.height(12.dp))
                SettingsRow(
                    icon = Icons.Default.Palette,
                    title = "Внешний вид",
                    subtitle = "Тема, акцентный цвет, фон"
                )
            }
        }
    }
}
