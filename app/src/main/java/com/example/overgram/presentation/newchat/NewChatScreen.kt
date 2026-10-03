package com.example.overgram.presentation.newchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.R
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.chat.ChatScreen
import com.example.overgram.presentation.chatlist.presenceText
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.AccentBright
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

data object NewChatScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: NewChatViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.openChat) {
            val args = state.openChat ?: return@LaunchedEffect
            viewModel.onChatOpened()
            // Replace, so "back" from the chat returns to the chat list.
            navigator.replace(ChatScreen(args.chatId, args.type, args.peerUserId, args.title))
        }

        LaunchedEffect(state.isSessionEnded) {
            if (state.isSessionEnded) navigator.replaceAll(PhoneEntryScreen)
        }

        NewChatContent(
            state = state,
            onBack = { navigator.pop() },
            onQueryChange = viewModel::onQueryChange,
            onUserClick = viewModel::onUserClick,
            onErrorShown = viewModel::onErrorShown,
            onEditUsername = viewModel::openUsernameDialog,
            onNewGroup = { navigator.push(SelectMembersScreen()) }
        )

        if (state.isUsernameDialogOpen) {
            UsernameDialog(
                state = state,
                onValueChange = viewModel::onUsernameChange,
                onSave = viewModel::saveUsername,
                onDismiss = viewModel::dismissUsernameDialog
            )
        }
    }
}

@Composable
fun NewChatContent(
    state: NewChatUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onErrorShown: () -> Unit,
    onEditUsername: () -> Unit,
    onNewGroup: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = state.error?.let { authErrorMessage(it) }
    LaunchedEffect(state.error) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onErrorShown()
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            OverGramTopBar(title = stringResource(R.string.new_chat_title), onBackClick = onBack)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.new_chat_search_hint), color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = if (state.isSearching) {
                    { CircularProgressIndicator(color = Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search
                ),
                colors = fieldColors()
            )

            MyUsernameRow(me = state.me, onEdit = onEditUsername)
            HorizontalDivider(color = DividerColor)
            NewGroupRow(onClick = onNewGroup)
            HorizontalDivider(color = DividerColor)

            when {
                state.results.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { user ->
                        UserRow(
                            user = user,
                            isOpening = state.openingUserId == user.id,
                            onClick = { onUserClick(user) }
                        )
                    }
                }
                state.searchedQuery != null && !state.isSearching -> HintText(
                    stringResource(R.string.new_chat_no_results, state.searchedQuery)
                )
                state.query.isBlank() -> HintText(stringResource(R.string.new_chat_hint))
            }
        }
    }
}

@Composable
private fun NewGroupRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(Dimens.AvatarMedium),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.GroupAdd, contentDescription = null, tint = AccentBright)
        }
        Spacer(Modifier.width(Dimens.SpacingLg))
        Text(
            text = stringResource(R.string.new_group),
            style = MaterialTheme.typography.titleMedium,
            color = AccentBright
        )
    }
}

@Composable
private fun MyUsernameRow(me: MyProfile?, onEdit: () -> Unit) {
    if (me == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.ScreenPadding, end = Dimens.SpacingSm, top = Dimens.SpacingSm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = me.username?.let { stringResource(R.string.new_chat_your_username, it) }
                ?: stringResource(R.string.new_chat_no_username),
            style = MaterialTheme.typography.bodyMedium,
            color = if (me.username == null) Accent else TextSecondary,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onEdit) {
            Text(
                stringResource(if (me.username == null) R.string.new_chat_set_username else R.string.new_chat_change_username),
                color = AccentBright
            )
        }
    }
}

@Composable
private fun UserRow(user: UserProfile, isOpening: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingMd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            name = user.displayName,
            imageUrl = mediaUrl(user.avatarMediaId),
            size = Dimens.AvatarMedium,
            isOnline = user.isOnline
        )
        Spacer(Modifier.width(Dimens.SpacingLg))
        Column(Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = listOfNotNull(user.username?.let { "@$it" }, presenceText(user)).joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isOpening) {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun HintText(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpacingXxl),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun UsernameDialog(
    state: NewChatUiState,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val error = when {
        state.isUsernameInvalid -> stringResource(R.string.username_invalid)
        state.usernameError != null -> authErrorMessage(state.usernameError)
        else -> null
    }
    AlertDialog(
        onDismissRequest = { if (!state.isSavingUsername) onDismiss() },
        containerColor = SurfaceDark,
        title = { Text(stringResource(R.string.username_dialog_title), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)) {
                OutlinedTextField(
                    value = state.usernameInput,
                    onValueChange = onValueChange,
                    prefix = { Text("@", color = TextSecondary) },
                    singleLine = true,
                    isError = error != null,
                    enabled = !state.isSavingUsername,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        autoCorrectEnabled = false,
                        imeAction = ImeAction.Done
                    ),
                    colors = fieldColors()
                )
                Text(
                    text = error ?: stringResource(R.string.username_dialog_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) ErrorRed else TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !state.isSavingUsername) {
                if (state.isSavingUsername) {
                    CircularProgressIndicator(color = Accent, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.username_dialog_save), color = Accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSavingUsername) {
                Text(stringResource(R.string.username_dialog_cancel), color = TextSecondary)
            }
        }
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Accent,
    unfocusedBorderColor = DividerColor,
    cursorColor = Accent
)

@Preview(showBackground = true)
@Composable
fun NewChatContentPreview() {
    OverGramTheme {
        NewChatContent(
            state = NewChatUiState(
                query = "demo",
                searchedQuery = "demo",
                me = MyProfile("me", "Tadashi", null, null, phone = "+998900000001"),
                results = listOf(
                    UserProfile("u2", "Demo User 2", "demo_user_2", null, isOnline = true, lastSeenAt = null),
                    UserProfile("u3", "Demo User 3", "demo_user_3", null, isOnline = false, lastSeenAt = null)
                )
            ),
            onBack = {}, onQueryChange = {}, onUserClick = {}, onErrorShown = {}, onEditUsername = {}, onNewGroup = {}
        )
    }
}
