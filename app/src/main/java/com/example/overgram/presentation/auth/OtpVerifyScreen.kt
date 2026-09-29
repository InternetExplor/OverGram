package com.example.overgram.presentation.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import com.example.overgram.presentation.auth.components.OtpCodeInput
import com.example.overgram.presentation.chatlist.ChatListScreen
import com.example.overgram.ui.components.OverGramTopBar
import com.example.overgram.ui.theme.BackgroundDark
import com.example.overgram.ui.theme.Dimens
import com.example.overgram.ui.theme.OverGramTheme

data object OtpVerifyScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = authViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        // The ViewModel's step drives navigation, so back must go through it too.
        BackHandler(onBack = viewModel::backToPhoneEntry)

        LaunchedEffect(state.step) {
            if (navigator.lastItem !is OtpVerifyScreen) return@LaunchedEffect
            when (state.step) {
                // Also covers process death: the restored stack has this screen but the state is fresh.
                AuthStep.PhoneEntry -> navigator.pop()
                AuthStep.Success -> {
                    navigator.replaceAll(ChatListScreen)
                    viewModel.onLoginHandled()
                }
                AuthStep.OtpEntry -> Unit
            }
        }

        OtpVerifyContent(
            state = state,
            onCodeChange = viewModel::onCodeChange,
            onVerify = viewModel::verify,
            onResend = viewModel::resendCode,
            onBack = viewModel::backToPhoneEntry,
            onFillTestCode = viewModel::fillTestCode
        )
    }
}

@Composable
fun OtpVerifyContent(
    state: AuthUiState,
    onCodeChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onBack: () -> Unit,
    onFillTestCode: () -> Unit,
    showDebugHints: Boolean = BuildConfig.DEBUG,
    autoFocus: Boolean = true
) {
    Scaffold(
        containerColor = BackgroundDark,
        topBar = {
            OverGramTopBar(title = "", onBackClick = onBack)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.SpacingXl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpacingLg)
        ) {
            AuthHeader(
                title = stringResource(R.string.auth_otp_title),
                subtitle = stringResource(
                    R.string.auth_otp_subtitle,
                    "${AuthUiState.COUNTRY_PREFIX} ${PhoneVisualTransformation.format(state.phoneDigits)}"
                )
            )

            OtpCodeInput(
                code = state.code,
                onCodeChange = onCodeChange,
                contentDescription = stringResource(
                    R.string.auth_otp_boxes_description,
                    state.otpLength
                ),
                length = state.otpLength,
                isError = state.error is AuthError.InvalidCode || state.needsNewCode,
                enabled = !state.isLoading && !state.needsNewCode,
                autoFocus = autoFocus,
                onDone = onVerify
            )

            state.error?.let { error ->
                AuthErrorText(message = authErrorMessage(error, state.cooldownSeconds))
            }

            if (state.needsNewCode) {
                // Locked/expired: the only way forward is a fresh code, so make that the primary action.
                AuthPrimaryButton(
                    text = if (state.isCooldownActive) {
                        stringResource(R.string.auth_request_new_code_countdown, state.cooldownSeconds)
                    } else {
                        stringResource(R.string.auth_request_new_code)
                    },
                    onClick = onResend,
                    enabled = !state.isCooldownActive,
                    isLoading = state.isLoading
                )
            } else {
                AuthPrimaryButton(
                    text = stringResource(R.string.auth_verify),
                    onClick = onVerify,
                    enabled = state.canVerify,
                    isLoading = state.isLoading
                )
                AuthTextAction(
                    text = if (state.isCooldownActive) {
                        stringResource(R.string.auth_resend_code_countdown, state.cooldownSeconds)
                    } else {
                        stringResource(R.string.auth_resend_code)
                    },
                    onClick = onResend,
                    enabled = !state.isCooldownActive && !state.isLoading
                )
            }

            AuthTextAction(
                text = stringResource(R.string.auth_change_number),
                onClick = onBack,
                enabled = !state.isLoading
            )

            if (showDebugHints && state.isTestNumber && !state.needsNewCode) {
                AuthTextAction(
                    text = stringResource(R.string.auth_debug_fill_code),
                    onClick = onFillTestCode,
                    enabled = !state.isLoading
                )
            }
        }
    }
}

private val previewState = AuthUiState(
    step = AuthStep.OtpEntry,
    phoneDigits = "900000001",
    cooldownSeconds = 24,
    cooldownPhoneDigits = "900000001"
)

@Preview(name = "OTP – entering", showBackground = true)
@Composable
private fun OtpEnteringPreview() {
    OverGramTheme {
        OtpVerifyContent(
            state = previewState.copy(code = "111"),
            onCodeChange = {}, onVerify = {}, onResend = {}, onBack = {}, onFillTestCode = {},
            showDebugHints = true, autoFocus = false
        )
    }
}

@Preview(name = "OTP – real number, 6 digits", showBackground = true, widthDp = 360)
@Composable
private fun OtpSixDigitPreview() {
    OverGramTheme {
        OtpVerifyContent(
            state = previewState.copy(phoneDigits = "901234567", cooldownPhoneDigits = "901234567", code = "4821"),
            onCodeChange = {}, onVerify = {}, onResend = {}, onBack = {}, onFillTestCode = {},
            showDebugHints = false, autoFocus = false
        )
    }
}

@Preview(name = "OTP – loading", showBackground = true)
@Composable
private fun OtpLoadingPreview() {
    OverGramTheme {
        OtpVerifyContent(
            state = previewState.copy(code = "11111", isLoading = true),
            onCodeChange = {}, onVerify = {}, onResend = {}, onBack = {}, onFillTestCode = {},
            showDebugHints = false, autoFocus = false
        )
    }
}

@Preview(name = "OTP – wrong code", showBackground = true)
@Composable
private fun OtpWrongCodePreview() {
    OverGramTheme {
        OtpVerifyContent(
            state = previewState.copy(error = AuthError.InvalidCode(remainingAttempts = 3), cooldownSeconds = 0),
            onCodeChange = {}, onVerify = {}, onResend = {}, onBack = {}, onFillTestCode = {},
            showDebugHints = false, autoFocus = false
        )
    }
}

@Preview(name = "OTP – locked", showBackground = true)
@Composable
private fun OtpLockedPreview() {
    OverGramTheme {
        OtpVerifyContent(
            state = previewState.copy(code = "22222", error = AuthError.OtpLocked, cooldownSeconds = 0),
            onCodeChange = {}, onVerify = {}, onResend = {}, onBack = {}, onFillTestCode = {},
            showDebugHints = false, autoFocus = false
        )
    }
}
