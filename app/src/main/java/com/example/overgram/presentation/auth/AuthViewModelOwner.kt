package com.example.overgram.presentation.auth

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * The phone and OTP screens are separate Voyager screens that share one [AuthViewModel],
 * so it is scoped to the hosting activity rather than to either screen.
 * [AuthViewModel.onLoginHandled] resets it once the flow finishes.
 */
@Composable
fun authViewModel(): AuthViewModel =
    hiltViewModel(viewModelStoreOwner = LocalContext.current.findActivity())

private fun Context.findActivity(): ComponentActivity {
    var context = this
    while (context is ContextWrapper) {
        if (context is ComponentActivity) return context
        context = context.baseContext
    }
    error("Auth screens must be hosted in a ComponentActivity")
}
