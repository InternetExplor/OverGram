package com.example.overgram.presentation.chatlist

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import com.example.overgram.MainScreenContent

/**
 * Voyager destination for the existing chat list UI, so the auth flow can route to it.
 */
data object ChatListScreen : Screen {

    @Composable
    override fun Content() {
        MainScreenContent()
    }
}
