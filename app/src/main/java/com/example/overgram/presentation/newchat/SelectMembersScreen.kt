package com.example.overgram.presentation.newchat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
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
import androidx.compose.ui.res.pluralStringResource
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
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.R
import com.example.overgram.domain.model.ChatType
import com.example.overgram.domain.model.UserProfile
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.chat.ChatScreen
import com.example.overgram.presentation.chatlist.ChatListScreen
import com.example.overgram.presentation.chatlist.presenceText
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.PrimaryViolet
import com.example.overgram.ui.theme.SurfaceElevatedDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

/**
 * Picks users by username. With [addToChatId] = null it creates a new group (and asks for a
 * name); otherwise it adds the picked users to that group.
 */
data class SelectMembersScreen(val addToChatId: String? = null) : Screen {

    override val key: ScreenKey = "select-members:${addToChatId ?: "new"}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = hiltViewModel<SelectMembersViewModel, SelectMembersViewModel.Factory>(
            key = key,
            creationCallback = { factory -> factory.create(addToChatId) }
        )
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.doneChatId) {
            val chatId = state.doneChatId ?: return@LaunchedEffect
            viewModel.onDoneHandled()
            if (state.isCreating) {
                // List → new chat → this screen becomes list → the new group.
                navigator.popUntil { it is ChatListScreen }
                navigator.push(ChatScreen(chatId, ChatType.GROUP, peerUserId = null, title = state.title.trim()))
            } else {
                navigator.pop()
            }
        }
        LaunchedEffect(state.isSessionEnded) {
            if (state.isSessionEnded) navigator.replaceAll(PhoneEntryScreen)
        }

        SelectMembersContent(
            state = state,
            onBack = { navigator.pop() },
            onTitleChange = viewModel::onTitleChange,
            onQueryChange = viewModel::onQueryChange,
            onToggle = viewModel::toggle,
            onSubmit = viewModel::submit,
            onErrorShown = viewModel::onErrorShown
        )
    }
}

@Composable
fun SelectMembersContent(
    state: SelectMembersUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onToggle: (UserProfile) -> Unit,
    onSubmit: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val titleFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (state.isCreating) titleFocus.requestFocus() }
    val snackbarMessage = state.error?.let { authErrorMessage(it) }
    LaunchedEffect(state.error) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage)
            onErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        topBar = {
            OverGramTopBar(
                title = stringResource(if (state.isCreating) R.string.new_group else R.string.add_members_title),
                subtitle = state.selected.size.takeIf { it > 0 }?.let {
                    pluralStringResource(R.plurals.members_selected, it, it)
                },
                onBackClick = onBack,
                actions = {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            color = PrimaryViolet,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = Dimens.SpacingLg)
                                .size(20.dp)
                        )
                    } else {
                        TextButton(onClick = onSubmit, enabled = state.canSubmit) {
                            Text(
                                stringResource(if (state.isCreating) R.string.new_group_create else R.string.add_members_confirm),
                                color = if (state.canSubmit) PrimaryViolet else TextSecondary
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            if (state.isCreating) {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = onTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenPadding)
                        .focusRequester(titleFocus),
                    placeholder = { Text(stringResource(R.string.new_group_title_hint), color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    colors = fieldColors()
                )
                Spacer(Modifier.height(Dimens.SpacingSm))
            }

            if (state.selected.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenPadding),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpacingSm)
                ) {
                    items(state.selected, key = { it.id }) { user ->
                        InputChip(
                            selected = true,
                            onClick = { onToggle(user) },
                            label = { Text(user.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            avatar = {
                                Avatar(
                                    name = user.displayName,
                                    imageUrl = mediaUrl(user.avatarMediaId),
                                    size = InputChipDefaults.AvatarSize
                                )
                            },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = SurfaceElevatedDark,
                                selectedLabelColor = TextPrimary,
                                selectedTrailingIconColor = TextSecondary
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingSm),
                placeholder = { Text(stringResource(R.string.new_chat_search_hint), color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                trailingIcon = if (state.isSearching) {
                    { CircularProgressIndicator(color = PrimaryViolet, modifier = Modifier.size(20.dp), strokeWidth = 2.dp) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search
                ),
                colors = fieldColors()
            )
            HorizontalDivider(color = DividerColor)

            when {
                state.results.isNotEmpty() -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { user ->
                        SelectableUserRow(
                            user = user,
                            isSelected = state.isSelected(user),
                            onToggle = { onToggle(user) }
                        )
                    }
                }
                state.searchedQuery != null && !state.isSearching -> Hint(
                    stringResource(R.string.new_chat_no_results, state.searchedQuery)
                )
                state.selected.isEmpty() -> Hint(
                    if (state.isCreating) stringResource(R.string.new_group_pick_members)
                    else stringResource(R.string.new_chat_hint)
                )
            }
        }
    }
}

@Composable
private fun SelectableUserRow(user: UserProfile, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = Dimens.ScreenPadding, end = Dimens.SpacingSm, top = Dimens.SpacingSm, bottom = Dimens.SpacingSm),
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
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = PrimaryViolet, uncheckedColor = TextSecondary)
        )
    }
}

@Composable
private fun Hint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpacingXxl),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryViolet,
    unfocusedBorderColor = DividerColor,
    cursorColor = PrimaryViolet
)

@Preview(showBackground = true)
@Composable
fun SelectMembersContentPreview() {
    val ada = UserProfile("u1", "Ada Lovelace", "ada", null, isOnline = true, lastSeenAt = null)
    val bob = UserProfile("u2", "Bob", "bob", null, isOnline = false, lastSeenAt = null)
    OverGramTheme {
        SelectMembersContent(
            state = SelectMembersUiState(
                addToChatId = null,
                title = "Учеба | TUIT",
                query = "a",
                searchedQuery = "a",
                results = listOf(ada, bob),
                selected = listOf(ada)
            ),
            onBack = {}, onTitleChange = {}, onQueryChange = {}, onToggle = {}, onSubmit = {}, onErrorShown = {}
        )
    }
}
