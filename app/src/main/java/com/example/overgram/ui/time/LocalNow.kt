package com.example.overgram.ui.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

/**
 * "Now" for relative times ("last seen 5 minutes ago"). Provided by [ProvideTickingNow], which
 * advances it periodically so such texts keep counting instead of freezing when first drawn.
 */
val LocalNow = staticCompositionLocalOf { System.currentTimeMillis() }

@Composable
fun ProvideTickingNow(periodMs: Long = TICK_MS, content: @Composable () -> Unit) {
    val now by produceState(System.currentTimeMillis(), periodMs) {
        while (true) {
            // Align with whole minutes so "1 minute ago" flips when it should.
            delay(periodMs - System.currentTimeMillis() % periodMs)
            value = System.currentTimeMillis()
        }
    }
    CompositionLocalProvider(LocalNow provides now, content = content)
}

private const val TICK_MS = 60_000L
