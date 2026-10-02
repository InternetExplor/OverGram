package com.example.overgram.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.example.overgram.BuildConfig
import com.example.overgram.R
import com.example.overgram.domain.model.AuthError
import com.example.overgram.presentation.auth.components.AuthErrorText
import com.example.overgram.presentation.auth.components.AuthHeader
import com.example.overgram.presentation.auth.components.AuthPrimaryButton
import com.example.overgram.presentation.auth.components.AuthTextAction
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.CardShape
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.ErrorRed
import com.example.overgram.ui.theme.OverGramTheme
import com.example.overgram.ui.theme.Accent
import com.example.overgram.ui.theme.SurfaceDark
import com.example.overgram.ui.theme.TextPrimary
import com.example.overgram.ui.theme.TextSecondary

data object PhoneEntryScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = authViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        LaunchedEffect(state.step) {
            if (state.step == AuthStep.OtpEntry && navigator.lastItem is PhoneEntryScreen) {
                navigator.push(OtpVerifyScreen)
            }
        }

        PhoneEntryContent(
            state = state,
            onPhoneChange = viewModel::onPhoneChange,
            onGetCode = viewModel::requestCode,
            onUseTestNumber = viewModel::fillTestPhone
        )
    }
}

@Composable
fun PhoneEntryContent(
    state: AuthUiState,
    onPhoneChange: (String) -> Unit,
    onGetCode: () -> Unit,
    onUseTestNumber: () -> Unit,
    showDebugHints: Boolean = BuildConfig.DEBUG
) {
    val uriHandler = LocalUriHandler.current

    Scaffold(containerColor = BackgroundDark) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingXxl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg)
        ) {
            Spacer(Modifier.height(Dimens.SpacingXxl))
            AuthHeader(
                title = stringResource(R.string.auth_phone_title),
                subtitle = stringResource(R.string.auth_phone_subtitle)
            )
            Spacer(Modifier.height(Dimens.SpacingSm))

            TextField(
                value = state.phoneDigits,
                onValueChange = onPhoneChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
                singleLine = true,
                isError = state.error != null,
                textStyle = MaterialTheme.typography.bodyLarge,
                leadingIcon = {
                    Text(
                        text = AuthUiState.COUNTRY_PREFIX,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.auth_phone_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary
                    )
                },
                visualTransformation = PhoneVisualTransformation,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onGetCode() }),
                shape = CardShape,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    disabledContainerColor = SurfaceDark,
                    errorContainerColor = SurfaceDark,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = ErrorRed,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    disabledTextColor = TextSecondary,
                    errorTextColor = TextPrimary,
                    cursorColor = Accent,
                    errorCursorColor = Accent
                )
            )

            state.error?.let { error ->
                val botUrl = (error as? AuthError.TelegramNotLinked)?.botUrl
                AuthErrorText(
                    message = authErrorMessage(error, state.cooldownSeconds),
                    actionText = botUrl?.let { stringResource(R.string.auth_open_telegram_bot) },
                    onAction = { botUrl?.let(uriHandler::openUri) }
                )
            }

            AuthPrimaryButton(
                text = if (state.isCooldownActive) {
                    stringResource(R.string.auth_get_code_countdown, state.cooldownSeconds)
                } else {
                    stringResource(R.string.auth_get_code)
                },
                onClick = onGetCode,
                enabled = state.canRequestCode,
                isLoading = state.isLoading
            )

            if (showDebugHints) {
                AuthTextAction(
                    text = stringResource(R.string.auth_debug_use_test_number),
                    onClick = onUseTestNumber,
                    enabled = !state.isLoading
                )
            }
        }
    }
}

@Preview(name = "Phone – empty", showBackground = true)
@Composable
private fun PhoneEntryEmptyPreview() {
    OverGramTheme {
        PhoneEntryContent(AuthUiState(), {}, {}, {}, showDebugHints = true)
    }
}

@Preview(name = "Phone – valid", showBackground = true)
@Composable
private fun PhoneEntryValidPreview() {
    OverGramTheme {
        PhoneEntryContent(AuthUiState(phoneDigits = "901234567"), {}, {}, {}, showDebugHints = false)
    }
}

@Preview(name = "Phone – loading", showBackground = true)
@Composable
private fun PhoneEntryLoadingPreview() {
    OverGramTheme {
        PhoneEntryContent(AuthUiState(phoneDigits = "901234567", isLoading = true), {}, {}, {}, showDebugHints = false)
    }
}

@Preview(name = "Phone – rate limited", showBackground = true)
@Composable
private fun PhoneEntryRateLimitedPreview() {
    OverGramTheme {
        PhoneEntryContent(
            state = AuthUiState(
                phoneDigits = "901234567",
                error = AuthError.RateLimited(),
                cooldownSeconds = 42,
                cooldownPhoneDigits = "901234567"
            ),
            onPhoneChange = {}, onGetCode = {}, onUseTestNumber = {},
            showDebugHints = false
        )
    }
}

@Preview(name = "Phone – Telegram not linked", showBackground = true)
@Composable
private fun PhoneEntryTelegramPreview() {
    OverGramTheme {
        PhoneEntryContent(
            state = AuthUiState(
                phoneDigits = "901234567",
                error = AuthError.TelegramNotLinked("https://t.me/relay_bootcamp_otp_bot?start=link")
            ),
            onPhoneChange = {}, onGetCode = {}, onUseTestNumber = {},
            showDebugHints = false
        )
    }
}
