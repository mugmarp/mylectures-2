package com.lectures.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val AccentColors = listOf(
    Color(0xFF2563EB), // Royal Blue (Default)
    Color(0xFF047857), // Emerald Green
    Color(0xFF7C3AED), // Academic Violet
    Color(0xFFB45309), // Warm Amber
    Color(0xFF0369A1)  // Ocean Teal
)

val AccentColorNames = listOf(
    "Royal Blue",
    "Emerald",
    "Violet",
    "Amber",
    "Ocean Teal"
)

val AccentDarkColors = listOf(
    Color(0xFF3B82F6),
    Color(0xFF10B981),
    Color(0xFFA78BFA),
    Color(0xFFF59E0B),
    Color(0xFF38BDF8)
)

fun accentDisplayColors(isDark: Boolean): List<Color> =
    if (isDark) AccentDarkColors else AccentColors

/**
 * Picks high-contrast foreground ink (`>= 4.5:1` WCAG AA) for any filled accent background.
 */
fun contrastOnColor(background: Color): Color =
    if (background.luminance() > 0.18f) Color(0xFF0B1120) else Color.White

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

private val AccentTertiaryLight = listOf(
    Color(0xFF4F46E5),
    Color(0xFF0D9488),
    Color(0xFF2563EB),
    Color(0xFFB45309),
    Color(0xFF2563EB)
)

private val AccentTertiaryDark = listOf(
    Color(0xFF818CF8),
    Color(0xFF2DD4BF),
    Color(0xFF60A5FA),
    Color(0xFFFBBF24),
    Color(0xFF7DD3FC)
)

enum class ThemeMode(val title: String, val subtitle: String) {
    LIGHT("Light", "Clean & bright daylight"),
    DARK("Dark", "Deep charcoal & eye comfort"),
    SYSTEM("System", "Match device settings")
}

@Immutable
data class AppExtendedColors(
    val surfaceBase: Color = Color(0xFFF8FAFC),
    val surfaceCard: Color = Color.White,
    val surfaceElevated: Color = Color(0xFFF1F5F9),
    val surfaceSubtle: Color = Color(0xFFEEF2F6),
    val borderSubtle: Color = Color(0xFFE2E8F0),
    val borderStrong: Color = Color(0xFFCBD5E1),
    val textPrimary: Color = Color(0xFF0F172A),
    val textSecondary: Color = Color(0xFF475569),
    val textMuted: Color = Color(0xFF64748B),
    val successBg: Color = Color(0xFFF0FDF4),
    val successBorder: Color = Color(0xFFBBF7D0),
    val successText: Color = Color(0xFF166534),
    val successIcon: Color = Color(0xFF16A34A),
    val warningBg: Color = Color(0xFFFEF3C7),
    val warningBorder: Color = Color(0xFFF59E0B),
    val warningText: Color = Color(0xFF92400E),
    val warningIcon: Color = Color(0xFFD97706),
    val dangerBg: Color = Color(0xFFFEE2E2),
    val dangerBorder: Color = Color(0xFFFCA5A5),
    val dangerText: Color = Color(0xFFB91C1C),
    val dangerIcon: Color = Color(0xFFDC2626),
    val infoBg: Color = Color(0xFFEFF6FF),
    val infoBorder: Color = Color(0xFFBFDBFE),
    val infoText: Color = Color(0xFF1D4ED8)
)

private val LightExtendedColors = AppExtendedColors()

private val DarkExtendedColors = AppExtendedColors(
    surfaceBase = Color(0xFF101216),
    surfaceCard = Color(0xFF181B22),
    surfaceElevated = Color(0xFF20242D),
    surfaceSubtle = Color(0xFF14171D),
    borderSubtle = Color(0xFF2B303C),
    borderStrong = Color(0xFF3B4254),
    textPrimary = Color(0xFFF1F5F9),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    successBg = Color(0xFF064E3B).copy(alpha = 0.55f),
    successBorder = Color(0xFF059669),
    successText = Color(0xFFA7F3D0),
    successIcon = Color(0xFF34D399),
    warningBg = Color(0xFF451A03).copy(alpha = 0.65f),
    warningBorder = Color(0xFFD97706),
    warningText = Color(0xFFFDE68A),
    warningIcon = Color(0xFFFBBF24),
    dangerBg = Color(0xFF7F1D1D).copy(alpha = 0.45f),
    dangerBorder = Color(0xFFEF4444).copy(alpha = 0.55f),
    dangerText = Color(0xFFFCA5A5),
    dangerIcon = Color(0xFFF87171),
    infoBg = Color(0xFF1E3A8A).copy(alpha = 0.40f),
    infoBorder = Color(0xFF3B82F6).copy(alpha = 0.45f),
    infoText = Color(0xFF93C5FD)
)

val LocalExtendedColors = compositionLocalOf { LightExtendedColors }

val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
    titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp),
    labelMedium = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

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

    val targetPrimary = if (isDark) primaryDark else primaryColor
    val targetOnPrimary = contrastOnColor(targetPrimary)
    val targetTertiary = if (isDark) AccentTertiaryDark[safeAccentIndex] else AccentTertiaryLight[safeAccentIndex]
    val targetBackground = if (isDark) DarkExtendedColors.surfaceBase else LightExtendedColors.surfaceBase
    val targetOnBackground = if (isDark) DarkExtendedColors.textPrimary else LightExtendedColors.textPrimary
    val targetSurface = if (isDark) DarkExtendedColors.surfaceCard else LightExtendedColors.surfaceCard
    val targetSurfaceVariant = if (isDark) DarkExtendedColors.surfaceElevated else LightExtendedColors.surfaceElevated
    val targetOnSurfaceVariant = if (isDark) DarkExtendedColors.textMuted else LightExtendedColors.textMuted

    val animSpec = tween<Color>(durationMillis = 220)
    val animatedPrimary by animateColorAsState(targetPrimary, animSpec, label = "theme_primary")
    val animatedBackground by animateColorAsState(targetBackground, animSpec, label = "theme_bg")
    val animatedSurface by animateColorAsState(targetSurface, animSpec, label = "theme_surface")
    val animatedSurfaceVariant by animateColorAsState(targetSurfaceVariant, animSpec, label = "theme_surface_var")
    val animatedOnSurface by animateColorAsState(targetOnBackground, animSpec, label = "theme_on_surface")

    val colorScheme: ColorScheme = if (isDark) {
        darkColorScheme(
            primary = animatedPrimary,
            onPrimary = targetOnPrimary,
            primaryContainer = darkContainer,
            onPrimaryContainer = onDarkContainer,
            secondary = animatedPrimary,
            onSecondary = targetOnPrimary,
            secondaryContainer = darkContainer,
            onSecondaryContainer = onDarkContainer,
            tertiary = targetTertiary,
            onTertiary = contrastOnColor(targetTertiary),
            background = animatedBackground,
            onBackground = animatedOnSurface,
            surface = animatedSurface,
            onSurface = animatedOnSurface,
            surfaceVariant = animatedSurfaceVariant,
            onSurfaceVariant = targetOnSurfaceVariant,
            surfaceContainerLowest = Color(0xFF0C0E12),
            surfaceContainerLow = Color(0xFF14171D),
            surfaceContainer = animatedSurface,
            surfaceContainerHigh = animatedSurfaceVariant,
            surfaceContainerHighest = Color(0xFF282D38),
            outline = Color(0xFF2E3442),
            outlineVariant = Color(0xFF242935),
            error = Color(0xFFF87171),
            onError = Color(0xFF450A0A),
            errorContainer = Color(0xFF7F1D1D).copy(alpha = 0.55f),
            onErrorContainer = Color(0xFFFECACA),
            scrim = Color.Black.copy(alpha = 0.65f)
        )
    } else {
        lightColorScheme(
            primary = animatedPrimary,
            onPrimary = targetOnPrimary,
            primaryContainer = lightContainer,
            onPrimaryContainer = onLightContainer,
            secondary = animatedPrimary,
            onSecondary = targetOnPrimary,
            secondaryContainer = lightContainer,
            onSecondaryContainer = onLightContainer,
            tertiary = targetTertiary,
            onTertiary = contrastOnColor(targetTertiary),
            background = animatedBackground,
            onBackground = animatedOnSurface,
            surface = animatedSurface,
            onSurface = animatedOnSurface,
            surfaceVariant = animatedSurfaceVariant,
            onSurfaceVariant = targetOnSurfaceVariant,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = Color(0xFFF8FAFC),
            surfaceContainer = Color(0xFFF1F5F9),
            surfaceContainerHigh = Color(0xFFE8EEF5),
            surfaceContainerHighest = Color(0xFFE2E8F0),
            outline = Color(0xFFE2E8F0),
            outlineVariant = Color(0xFFF1F5F9),
            error = Color(0xFFDC2626),
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B),
            scrim = Color(0xFF0F172A).copy(alpha = 0.45f)
        )
    }

    val extendedColors = if (isDark) DarkExtendedColors else LightExtendedColors

    val themeState = AppThemeState(
        themeMode = themeMode,
        isDark = isDark,
        accentIndex = accentIndex,
        onThemeModeChange = onThemeModeChange,
        onAccentChange = onAccentChange
    )

    CompositionLocalProvider(
        LocalAppTheme provides themeState,
        LocalExtendedColors provides extendedColors,
        LocalOverscrollConfiguration provides null
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content
            )
        }
    }
}
