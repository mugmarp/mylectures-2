package com.lectures.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val AccentColors = listOf(
    Color(0xFF2563EB), // Royal Blue (Default)
    Color(0xFF059669), // Emerald Green
    Color(0xFF7C3AED), // Academic Violet
    Color(0xFFD97706), // Warm Amber
    Color(0xFF0284C7)  // Ocean Teal
)

val AccentColorNames = listOf(
    "Royal Blue",
    "Emerald",
    "Violet",
    "Amber",
    "Ocean Teal"
)

private val AccentDarkColors = listOf(
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFA78BFA),
    Color(0xFFF59E0B),
    Color(0xFF38BDF8)
)

private val AccentLightContainers = listOf(
    Color(0xFFE2EDFE) to Color(0xFF1D4ED8),
    Color(0xFFD1FAE5) to Color(0xFF047857),
    Color(0xFFEDE9FE) to Color(0xFF6D28D9),
    Color(0xFFFEF3C7) to Color(0xFFB45309),
    Color(0xFFE0F2FE) to Color(0xFF0369A1)
)

private val AccentDarkContainers = listOf(
    Color(0xFF1E3258) to Color(0xFFBFDBFE),
    Color(0xFF064E3B) to Color(0xFFA7F3D0),
    Color(0xFF3B0764) to Color(0xFFDDD6FE),
    Color(0xFF451A03) to Color(0xFFFDE68A),
    Color(0xFF0C4A6E) to Color(0xFFBAE6FD)
)

enum class ThemeMode(val title: String, val subtitle: String) {
    LIGHT("Light", "Clean & bright daylight"),
    DARK("Dark", "Deep charcoal & eye comfort"),
    SYSTEM("System", "Match device settings")
}

data class AppThemeState(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val isDark: Boolean = false,
    val accentIndex: Int = 0,
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onAccentChange: (Int) -> Unit = {}
)

val LocalAppTheme = compositionLocalOf { AppThemeState() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean? = null,
    themeMode: ThemeMode = ThemeMode.LIGHT,
    accentIndex: Int = 0,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onAccentChange: (Int) -> Unit = {},
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = if (darkTheme != null) {
        darkTheme
    } else {
        when (themeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> systemInDark
        }
    }

    val safeAccentIndex = accentIndex.coerceIn(0, AccentColors.lastIndex)
    val primaryColor = AccentColors[safeAccentIndex]
    val primaryDark = AccentDarkColors[safeAccentIndex]
    val (lightContainer, onLightContainer) = AccentLightContainers[safeAccentIndex]
    val (darkContainer, onDarkContainer) = AccentDarkContainers[safeAccentIndex]

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = primaryDark,
            onPrimary = Color.White,
            primaryContainer = darkContainer,
            onPrimaryContainer = onDarkContainer,
            secondary = primaryDark,
            onSecondary = Color.White,
            background = Color(0xFF101216),       // Neutral deep charcoal/obsidian
            onBackground = Color(0xFFF8FAFC),     // Pure crisp text
            surface = Color(0xFF181B22),          // Elevated crisp dark surface
            onSurface = Color(0xFFF8FAFC),        // Pure crisp text
            surfaceVariant = Color(0xFF20242D),   // Neutral elevated container
            onSurfaceVariant = Color(0xFF94A3B8), // Readable secondary text
            outline = Color(0xFF2E3442),          // Crisp subtle border
            outlineVariant = Color(0xFF20242D)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = lightContainer,
            onPrimaryContainer = onLightContainer,
            secondary = primaryColor,
            onSecondary = Color.White,
            background = Color(0xFFF8FAFC),       // Slate 50 daylight
            onBackground = Color(0xFF0F172A),     // Slate 900 dark text
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFF1F5F9),   // Slate 100
            onSurfaceVariant = Color(0xFF64748B), // Slate 500
            outline = Color(0xFFE2E8F0),          // Slate 200
            outlineVariant = Color(0xFFF1F5F9)
        )
    }

    val themeState = AppThemeState(
        themeMode = themeMode,
        isDark = isDark,
        accentIndex = accentIndex,
        onThemeModeChange = onThemeModeChange,
        onAccentChange = onAccentChange
    )

    CompositionLocalProvider(
        LocalAppTheme provides themeState,
        LocalOverscrollConfiguration provides null
    ) {
        MaterialTheme(
            colorScheme = colorScheme
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content
            )
        }
    }
}
