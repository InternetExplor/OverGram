package com.example.overgram.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.R
import com.example.overgram.domain.model.MyProfile
import com.example.overgram.presentation.auth.authErrorMessage
import com.example.overgram.presentation.common.formatPhone
import com.example.overgram.presentation.common.mediaUrl
import com.example.overgram.ui.components.Avatar
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.DividerColor
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

data object EditProfileScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel: EditProfileViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.isSaved) {
            if (state.isSaved) navigator.pop()
        }

        // The system photo picker: no storage permission needed.
        val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            uri?.let { viewModel.onAvatarPicked(it.toString()) }
        }

        EditProfileContent(
            state = state,
            onBack = { navigator.pop() },
            onNameChange = viewModel::onNameChange,
            onUsernameChange = viewModel::onUsernameChange,
            onSave = viewModel::save,
            onPickPhoto = {
                pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemovePhoto = viewModel::onRemoveAvatar,
            onErrorShown = viewModel::onErrorShown
        )
    }
}

@Composable
fun EditProfileContent(
    state: EditProfileUiState,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onUsernameChange: (String) -> Unit,
    onSave: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onErrorShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
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
                title = stringResource(R.string.profile_title),
                onBackClick = onBack,
                actions = {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = Accent,
                            strokeWidth = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = Dimens.SpacingLg)
                                .size(20.dp)
                        )
                    } else {
                        TextButton(onClick = onSave, enabled = state.canSave) {
                            Text(
                                stringResource(R.string.action_save),
                                color = if (state.canSave) Accent else TextSecondary
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
                .verticalScroll(rememberScrollState())
                .padding(Dimens.ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingMd)
        ) {
            AvatarEditor(
                state = state,
                onPickPhoto = onPickPhoto,
                onRemovePhoto = onRemovePhoto
            )
            Spacer(Modifier.height(Dimens.SpacingSm))

            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_name)) },
                singleLine = true,
                isError = state.isNameBlank,
                supportingText = if (state.isNameBlank) {
                    { Text(stringResource(R.string.profile_name_required), color = ErrorRed) }
                } else null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                colors = fieldColors()
            )

            val usernameMessage = when {
                state.usernameError != null -> authErrorMessage(state.usernameError)
                state.usernameProblem == UsernameProblem.Invalid -> stringResource(R.string.username_invalid)
                state.usernameProblem == UsernameProblem.CannotRemove -> stringResource(R.string.profile_username_cannot_remove)
                else -> null
            }
            OutlinedTextField(
                value = state.username,
                onValueChange = onUsernameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_username)) },
                prefix = { Text("@", color = TextSecondary) },
                singleLine = true,
                isError = usernameMessage != null,
                supportingText = {
                    Text(
                        text = usernameMessage ?: stringResource(R.string.profile_username_hint),
                        color = if (usernameMessage != null) ErrorRed else TextSecondary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done
                ),
                colors = fieldColors()
            )

            state.me?.phone?.takeIf { it.isNotEmpty() }?.let { phone ->
                Column(Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.profile_phone),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(formatPhone(phone), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                    Text(
                        stringResource(R.string.profile_phone_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

/** Big avatar with a camera badge; tap for "Choose photo" / "Remove photo". */
@Composable
private fun AvatarEditor(state: EditProfileUiState, onPickPhoto: () -> Unit, onRemovePhoto: () -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val hasPhoto = state.me?.avatarMediaId != null
    Box(contentAlignment = Alignment.Center) {
        Avatar(
            name = state.name.ifBlank { state.me?.displayName.orEmpty() },
            imageUrl = mediaUrl(state.me?.avatarMediaId),
            size = Dimens.AvatarExtraLarge,
            onClick = { if (!state.isUpdatingAvatar) menuOpen = true }
        )
        if (state.isUpdatingAvatar) {
            Box(
                modifier = Modifier
                    .size(Dimens.AvatarExtraLarge)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TextPrimary, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .background(Accent, CircleShape)
                .border(2.dp, BackgroundDark, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PhotoCamera,
                contentDescription = stringResource(R.string.profile_change_photo),
                tint = TextPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.profile_choose_photo)) },
                leadingIcon = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    onPickPhoto()
                }
            )
            if (hasPhoto) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.profile_remove_photo), color = ErrorRed) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                    onClick = {
                        menuOpen = false
                        onRemovePhoto()
                    }
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = Accent,
    unfocusedBorderColor = DividerColor,
    focusedLabelColor = Accent,
    unfocusedLabelColor = TextSecondary,
    cursorColor = Accent
)

@Preview(showBackground = true)
@Composable
fun EditProfileContentPreview() {
    val me = MyProfile("me", "Tadashi", "tadashi", null, "+998900000001")
    OverGramTheme {
        EditProfileContent(
            state = EditProfileUiState(me = me, name = "Tadashi Ozaki", username = "tadashi"),
            onBack = {}, onNameChange = {}, onUsernameChange = {}, onSave = {},
            onPickPhoto = {}, onRemovePhoto = {}, onErrorShown = {}
        )
    }
}
