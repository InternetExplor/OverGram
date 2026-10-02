package com.example.overgram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import com.example.overgram.domain.usecase.HasSessionUseCase
import com.example.overgram.domain.usecase.SetRealtimeActiveUseCase
import com.example.overgram.presentation.auth.PhoneEntryScreen
import com.example.overgram.presentation.chatlist.ChatListScreen
import com.example.overgram.ui.theme.OverGramTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var hasSession: HasSessionUseCase

    @Inject
    lateinit var setRealtimeActive: SetRealtimeActiveUseCase

    override fun onStart() {
        super.onStart()
        // Online while visible: the socket is also what makes peers see us as online.
        if (hasSession()) setRealtimeActive(true)
    }

    override fun onStop() {
        super.onStop()
        // A rotation would only bounce the socket (and our presence) for nothing.
        if (!isChangingConfigurations) setRealtimeActive(false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val startScreen = if (hasSession()) ChatListScreen else PhoneEntryScreen
        setContent {
            OverGramTheme {
                Navigator(startScreen) { navigator ->
                    SlideTransition(navigator)
                }
            }
        }
    }
}
