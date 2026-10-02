package com.example.overgram.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.overgram.BuildConfig
import com.example.overgram.R
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.ServerInfo
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.common.formatPhone
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.components.SettingsRow
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

@Composable
fun SettingsTab(
    onEditProfile: () -> Unit,
    onSessionEnded: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSessionEnded) {
        if (state.isSessionEnded) onSessionEnded()
    }

    SettingsContent(
        state = state,
        onEditProfile = onEditProfile,
        onRetry = viewModel::refresh,
        onClearCache = viewModel::clearCache,
        onCacheClearedShown = viewModel::onCacheClearedShown,
        onLogout = viewModel::logout,
        bottomBar = bottomBar
    )
}

@Composable
fun SettingsContent(
    state: SettingsUiState,
    onEditProfile: () -> Unit,
    onRetry: () -> Unit,
    onClearCache: () -> Unit,
    onCacheClearedShown: () -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clearedText = stringResource(R.string.settings_cache_cleared)
    LaunchedEffect(state.cacheCleared) {
        if (state.cacheCleared) {
            snackbarHostState.showSnackbar(clearedText)
            onCacheClearedShown()
        }
    }
    var confirmClearCache by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = { OverGramTopBar(title = stringResource(R.string.tab_settings)) },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingSm),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
        ) {
            ProfileHeader(state = state, onClick = onEditProfile, onRetry = onRetry)

            SectionTitle(stringResource(R.string.settings_section_account))
            SettingsRow(
                icon = Icons.Default.Person,
                title = stringResource(R.string.settings_edit_profile),
                subtitle = stringResource(R.string.settings_edit_profile_subtitle),
                onClick = onEditProfile
            )

            SectionTitle(stringResource(R.string.settings_section_storage))
            SettingsRow(
                icon = Icons.Default.DeleteSweep,
                title = stringResource(R.string.settings_clear_cache),
                subtitle = stringResource(R.string.settings_clear_cache_subtitle),
                trailing = if (state.isClearingCache) {
                    { CircularProgressIndicator(color = PrimaryViolet, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) }
                } else null,
                onClick = { confirmClearCache = true }
            )

            SectionTitle(stringResource(R.string.settings_section_about))
            SettingsRow(
                icon = Icons.Default.Info,
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
                onClick = null
            )
            SettingsRow(
                icon = Icons.Default.Cloud,
                title = stringResource(R.string.settings_server),
                subtitle = state.serverInfo?.let { info ->
                    stringResource(
                        R.string.settings_server_version,
                        info.version,
                        stringResource(if (info.pushEnabled) R.string.settings_push_available else R.string.settings_push_unavailable)
                    )
                } ?: stringResource(R.string.settings_server_unknown),
                onClick = null
            )

            Spacer(Modifier.height(Dimens.SpacingSm))
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(R.string.settings_logout),
                titleColor = ErrorRed,
                iconTintColor = ErrorRed,
                iconBackgroundColor = ErrorRed.copy(alpha = 0.15f),
                trailing = if (state.isLoggingOut) {
                    { CircularProgressIndicator(color = ErrorRed, strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) }
                } else null,
                onClick = { confirmLogout = true }
            )
            Spacer(Modifier.height(Dimens.SpacingLg))
        }
    }

    if (confirmClearCache) {
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_cache_confirm_title),
            text = stringResource(R.string.settings_clear_cache_confirm_text),
            confirm = stringResource(R.string.settings_clear_cache_confirm),
            onConfirm = {
                confirmClearCache = false
                onClearCache()
            },
            onDismiss = { confirmClearCache = false }
        )
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(R.string.settings_logout_confirm_title),
            text = stringResource(R.string.settings_logout_confirm_text),
            confirm = stringResource(R.string.settings_logout),
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false }
        )
    }
}

@Composable
private fun ProfileHeader(state: SettingsUiState, onClick: () -> Unit, onRetry: () -> Unit) {
    val me = state.me
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpacingSm)
            .clickable(enabled = me != null, onClick = onClick)
            .padding(vertical = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = me?.displayName.orEmpty(),
            imageUrl = mediaUrl(me?.avatarMediaId),
            size = Dimens.AvatarLarge
        )
        Spacer(Modifier.width(Dimens.SpacingLg))
        Column(Modifier.weight(1f)) {
            when {
                me != null -> {
                    Text(
                        text = me.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (me.phone.isNotEmpty()) {
                        Text(formatPhone(me.phone), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                    Text(
                        text = me.username?.let { "@$it" } ?: stringResource(R.string.settings_no_username),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (me.username == null) PrimaryViolet else TextSecondary
                    )
                }
                state.loadError != null -> {
                    Text(authErrorMessage(state.loadError), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.chats_retry), color = PrimaryViolet) }
                }
                else -> CircularProgressIndicator(color = PrimaryViolet, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
            }
        }
        if (me != null) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = PrimaryViolet,
        modifier = Modifier.padding(top = Dimens.SpacingMd, start = Dimens.SpacingXs)
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(title, color = TextPrimary) },
        text = { Text(text, color = TextSecondary) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirm, color = ErrorRed) } },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = TextSecondary) }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun SettingsContentPreview() {
    OverGramTheme {
        SettingsContent(
            state = SettingsUiState(
                me = MyProfile("me", "Tadashi", "tadashi", null, "+998900000001"),
                serverInfo = ServerInfo("1.1.0", pushEnabled = true)
            ),
            onEditProfile = {}, onRetry = {}, onClearCache = {}, onCacheClearedShown = {}, onLogout = {}
        )
    }
}
