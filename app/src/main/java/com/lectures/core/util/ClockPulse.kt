package com.lectures.core.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/**
 * Ambient CompositionLocal providing real-time clock timestamp in milliseconds.
 * Pulses every 15 seconds while the UI is actively visible (STARTED) and immediately
 * refreshes upon returning from the background.
 */
val LocalTimePulse = compositionLocalOf { 0L }

/**
 * Composable utility that returns an observable clock pulse (timestamp in ms).
 * Shares an upstream [LocalTimePulse] when available (avoiding duplicate coroutines
 * across list cards), and pauses automatically whenever the lifecycle stops (`ON_STOP`).
 */
@Composable
fun rememberLiveClockPulse(intervalMillis: Long = 15_000L): Long {
    val ambientPulse = LocalTimePulse.current
    if (ambientPulse != 0L && intervalMillis == 15_000L) {
        return ambientPulse
    }
    return rememberLifecycleGatedClockPulse(intervalMillis)
}

@Composable
fun rememberLifecycleGatedClockPulse(intervalMillis: Long = 15_000L): Long {
    var clockPulse by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Only ticks while lifecycle is at least STARTED (zero background wakeups when backgrounded)
    LaunchedEffect(lifecycleOwner, intervalMillis) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            clockPulse = System.currentTimeMillis()
            while (true) {
                delay(intervalMillis)
                clockPulse = System.currentTimeMillis()
            }
        }
    }

    return clockPulse
}

@Composable
fun ProvideLiveClockPulse(
    intervalMillis: Long = 15_000L,
    content: @Composable () -> Unit
) {
    val pulse = rememberLifecycleGatedClockPulse(intervalMillis)
    CompositionLocalProvider(LocalTimePulse provides pulse, content = content)
}
