package com.lectures.ui

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Centralized motion tokens and reduced-motion utilities for Lectures.
 * Keeps springs, fades, and directional slides cohesive across mobile and desktop.
 */
object MotionSpec {
    const val THEME_TRANSITION_MS = 220
    const val STATE_COLOR_MS = 180
    const val HOVER_MS = 120
    const val TAB_FADE_IN_MS = 170
    const val TAB_FADE_OUT_MS = 110
    const val SCREEN_ENTER_MS = 230
    const val SCREEN_EXIT_MS = 150
    const val DIALOG_ENTER_MS = 180
    const val RAIL_WIDTH_MS = 200
    const val CHART_BAR_MS = 520

    fun <T> standardSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)

    fun <T> bouncyCheckSpring(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium)

    fun <T> stateTween(durationMillis: Int = STATE_COLOR_MS, reducedMotion: Boolean = false): FiniteAnimationSpec<T> =
        if (reducedMotion) snap() else tween(durationMillis = durationMillis)

    fun fadeTransform(
        enterMs: Int = TAB_FADE_IN_MS,
        exitMs: Int = TAB_FADE_OUT_MS,
        reducedMotion: Boolean = false
    ): ContentTransform {
        if (reducedMotion) {
            return EnterTransition.None togetherWith ExitTransition.None
        }
        return fadeIn(animationSpec = tween(enterMs)) togetherWith fadeOut(animationSpec = tween(exitMs))
    }

    fun directionalSlideTransform(
        forward: Boolean,
        reducedMotion: Boolean = false,
        useFadeOnly: Boolean = false
    ): ContentTransform {
        if (reducedMotion) {
            return EnterTransition.None togetherWith ExitTransition.None
        }
        if (useFadeOnly) {
            return fadeTransform(
                enterMs = TAB_FADE_IN_MS,
                exitMs = TAB_FADE_OUT_MS,
                reducedMotion = reducedMotion
            )
        }
        val dir = if (forward) 1 else -1
        return (slideInHorizontally(animationSpec = standardSpring()) { (dir * it * 0.25f).toInt() } +
            fadeIn(animationSpec = tween(SCREEN_ENTER_MS)))
            .togetherWith(
                slideOutHorizontally(animationSpec = standardSpring()) { (-dir * it * 0.18f).toInt() } +
                    fadeOut(animationSpec = tween(SCREEN_EXIT_MS))
            )
    }
}

private fun readPrefersReducedMotion(context: android.content.Context): Boolean {
    return try {
        val scale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        scale == 0f
    } catch (_: Exception) {
        false
    }
}

/**
 * Checks whether the device has animations disabled via Accessibility / Developer Options
 * (`Settings.Global.ANIMATOR_DURATION_SCALE == 0f`) and reacts dynamically to changes.
 */
@Composable
fun rememberPrefersReducedMotion(): Boolean {
    val context = LocalContext.current
    var prefersReducedMotion by remember(context) {
        mutableStateOf(readPrefersReducedMotion(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        prefersReducedMotion = readPrefersReducedMotion(context)
    }

    DisposableEffect(context) {
        val resolver = context.contentResolver
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                prefersReducedMotion = readPrefersReducedMotion(context)
            }
        }
        runCatching {
            resolver.registerContentObserver(uri, false, observer)
        }
        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }

    return prefersReducedMotion
}
