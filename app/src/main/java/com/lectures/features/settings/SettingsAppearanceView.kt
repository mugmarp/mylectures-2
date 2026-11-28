package com.lectures.features.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.AccentColorNames
import com.lectures.ui.AppThemeState
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.ThemeMode
import com.lectures.ui.accentDisplayColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberPrefersReducedMotion

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSettingsView(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    appTheme: AppThemeState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    bottomContentPadding: Dp = 24.dp,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectAccent: (Int) -> Unit
) {
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val swatches = accentDisplayColors(appTheme.isDark)
    val selectedAccent = appTheme.accentIndex.coerceIn(0, AccentColorNames.lastIndex)
    val headerHorizontalPadding = horizontalPadding + 4.dp

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        SectionHeader("THEME MODE", horizontalPadding = headerHorizontalPadding)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                val wrapThemeCards = maxWidth < 280.dp || (isTightFont && maxWidth < 360.dp)
                if (wrapThemeCards) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        maxItemsInEachRow = 2
                    ) {
                        ThemeSelectionCard(
                            title = "Light",
                            subtitle = "Bright daylight",
                            icon = Icons.Default.LightMode,
                            isSelected = appTheme.themeMode == ThemeMode.LIGHT,
                            previewBgColor = Color(0xFFF8FAFC),
                            previewCardColor = Color.White,
                            previewTextColor = Color(0xFF0F172A),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_light"),
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) }
                        )

                        ThemeSelectionCard(
                            title = "Dark",
                            subtitle = "Night comfort",
                            icon = Icons.Default.DarkMode,
                            isSelected = appTheme.themeMode == ThemeMode.DARK,
                            previewBgColor = Color(0xFF101216),
                            previewCardColor = Color(0xFF181B22),
                            previewTextColor = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_dark"),
                            onClick = { onSelectThemeMode(ThemeMode.DARK) }
                        )

                        ThemeSelectionCard(
                            title = "System",
                            subtitle = "Auto adapt",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = appTheme.themeMode == ThemeMode.SYSTEM,
                            previewBgColor = Color(0xFFE2E8F0),
                            previewCardColor = Color(0xFF181B22),
                            previewTextColor = Color(0xFF1E293B),
                            isSplitSystemPreview = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_system"),
                            onClick = { onSelectThemeMode(ThemeMode.SYSTEM) }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeSelectionCard(
                            title = "Light",
                            subtitle = "Bright daylight",
                            icon = Icons.Default.LightMode,
                            isSelected = appTheme.themeMode == ThemeMode.LIGHT,
                            previewBgColor = Color(0xFFF8FAFC),
                            previewCardColor = Color.White,
                            previewTextColor = Color(0xFF0F172A),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_light"),
                            onClick = { onSelectThemeMode(ThemeMode.LIGHT) }
                        )

                        ThemeSelectionCard(
                            title = "Dark",
                            subtitle = "Night comfort",
                            icon = Icons.Default.DarkMode,
                            isSelected = appTheme.themeMode == ThemeMode.DARK,
                            previewBgColor = Color(0xFF101216),
                            previewCardColor = Color(0xFF181B22),
                            previewTextColor = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_dark"),
                            onClick = { onSelectThemeMode(ThemeMode.DARK) }
                        )

                        ThemeSelectionCard(
                            title = "System",
                            subtitle = "Auto adapt",
                            icon = Icons.Default.BrightnessAuto,
                            isSelected = appTheme.themeMode == ThemeMode.SYSTEM,
                            previewBgColor = Color(0xFFE2E8F0),
                            previewCardColor = Color(0xFF181B22),
                            previewTextColor = Color(0xFF1E293B),
                            isSplitSystemPreview = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("theme_mode_system"),
                            onClick = { onSelectThemeMode(ThemeMode.SYSTEM) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader("ACCENT COLOR", horizontalPadding = headerHorizontalPadding)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Highlight color for buttons, chips, and active indicators",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                val swatchItem: @Composable (Int, Color, Modifier) -> Unit = { idx, color, colMod ->
                    val name = AccentColorNames.getOrElse(idx) { "Color $idx" }
                    val selected = selectedAccent == idx
                    val swatchScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = if (reducedMotion) snap() else MotionSpec.bouncyCheckSpring(),
                        label = "accent_swatch_scale_$idx"
                    )
                    val ringWidth by animateDpAsState(
                        targetValue = if (selected) 3.dp else 0.dp,
                        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                        label = "accent_ring_w_$idx"
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = colMod
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .desktopHandCursor()
                            .clickable { onSelectAccent(idx) }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .semantics { contentDescription = "Select $name accent color" }
                            .testTag("accent_swatch_$idx")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .graphicsLayer {
                                    scaleX = swatchScale
                                    scaleY = swatchScale
                                }
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (ringWidth > 0.dp) {
                                        Modifier.border(ringWidth, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = selected,
                                enter = if (reducedMotion) fadeIn() else scaleIn(animationSpec = MotionSpec.bouncyCheckSpring()) + fadeIn(),
                                exit = if (reducedMotion) fadeOut() else scaleOut() + fadeOut()
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = contrastOnColor(color),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = name,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useFlow = maxWidth < 280.dp || (isTightFont && maxWidth < 360.dp)
                    if (useFlow) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            swatches.forEachIndexed { idx, color ->
                                swatchItem(idx, color, Modifier)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            swatches.forEachIndexed { idx, color ->
                                swatchItem(idx, color, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader("LIVE STUDY PREVIEW", horizontalPadding = headerHorizontalPadding)
        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .testTag("appearance_live_preview_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "CSC 3102 · LECTURE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = ext.textMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "20:00 – 22:00",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = ext.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Algorithms & Night Revision",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Focus on graph shortest-path invariants and O(E log V) proofs.",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = ext.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = if (appTheme.isDark) "Dark Readability Active" else "Daylight Mode Active",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ext.successBg,
                        border = BorderStroke(1.dp, ext.successBorder),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            text = "WCAG Contrast OK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ext.successText,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}
