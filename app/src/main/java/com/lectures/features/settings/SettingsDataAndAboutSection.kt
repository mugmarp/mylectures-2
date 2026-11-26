package com.lectures.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.BuildConfig
import com.lectures.ui.AccentColorNames
import com.lectures.ui.AppThemeState
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.ThemeMode
import com.lectures.ui.accentDisplayColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable

@Composable
private fun SettingsSummaryHubCard(
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    title: String,
    subtitle: String,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    chevronContentDescription: String,
    isTightFont: Boolean,
    horizontalPadding: Dp,
    testTag: String,
    onClick: () -> Unit,
    trailingContent: @Composable () -> Unit
) {
    val ext = LocalExtendedColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .desktopHandCursor()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            val stackTrailing = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
            if (stackTrailing) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(iconContainerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = subtitle,
                                fontSize = 12.sp,
                                color = subtitleColor,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = chevronContentDescription,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Row(
                        modifier = Modifier.padding(start = 50.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trailingContent()
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(iconContainerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = subtitleColor,
                            maxLines = if (isTightFont) 2 else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trailingContent()
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = chevronContentDescription,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun SettingsAppearanceSection(
    appTheme: AppThemeState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onOpenAppearance: () -> Unit
) {
    SectionHeader("APPEARANCE & THEME", horizontalPadding = horizontalPadding + 4.dp)
    val activeAccentIdx = appTheme.accentIndex.coerceIn(0, AccentColorNames.lastIndex)
    val activeAccentName = AccentColorNames[activeAccentIdx]
    val displaySwatch = accentDisplayColors(appTheme.isDark)[activeAccentIdx]
    SettingsSummaryHubCard(
        icon = Icons.Outlined.Palette,
        iconTint = MaterialTheme.colorScheme.primary,
        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        title = "Appearance & Theme",
        subtitle = when (appTheme.themeMode) {
            ThemeMode.LIGHT -> "Light mode · $activeAccentName"
            ThemeMode.DARK -> "Dark mode · $activeAccentName"
            ThemeMode.SYSTEM -> "System (${if (appTheme.isDark) "Dark" else "Light"}) · $activeAccentName"
        },
        chevronContentDescription = "Open appearance and theme settings",
        isTightFont = isTightFont,
        horizontalPadding = horizontalPadding,
        testTag = "settings_appearance_card",
        onClick = onOpenAppearance
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(displaySwatch)
        )
        SettingsStatusPill(
            text = "Customize",
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
internal fun SettingsDataSummaryCard(
    uiState: SettingsUiState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onOpenDataAndBackup: () -> Unit
) {
    val ext = LocalExtendedColors.current
    SectionHeader("DATA & CLOUD BACKUP", horizontalPadding = horizontalPadding + 4.dp)
    val modePrefix = if (uiState.isCloudSyncActive) "Cloud synced" else "Guest mode"
    SettingsSummaryHubCard(
        icon = Icons.Outlined.CloudSync,
        iconTint = MaterialTheme.colorScheme.primary,
        iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        title = "Data, Cloud Sync & Reset",
        subtitle = "$modePrefix · ${uiState.userNotesCount} notes · ${uiState.userAssignmentsCount} tasks",
        chevronContentDescription = "Open data, cloud sync and reset settings",
        isTightFont = isTightFont,
        horizontalPadding = horizontalPadding,
        testTag = "settings_data_sync_card",
        onClick = onOpenDataAndBackup
    ) {
        SettingsStatusPill(
            text = if (uiState.isCloudSyncActive) "Synced" else "Local",
            containerColor = if (uiState.isCloudSyncActive) ext.successBg else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (uiState.isCloudSyncActive) ext.successText else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun SettingsAboutSummaryCard(
    uiState: SettingsUiState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onOpenAboutAndLegal: () -> Unit
) {
    val availableUpdate = uiState.availableUpdateInfo
    SectionHeader("ABOUT & UPDATES", horizontalPadding = horizontalPadding + 4.dp)
    SettingsSummaryHubCard(
        icon = if (availableUpdate != null) Icons.Outlined.SystemUpdate else Icons.Outlined.Info,
        iconTint = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        iconContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
        title = "About, Updates & Legal",
        subtitle = if (availableUpdate != null) {
            "Update v${availableUpdate.versionName} available · Releases & policies"
        } else {
            "v${BuildConfig.VERSION_NAME} · Timetable release, feedback & policies"
        },
        subtitleColor = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        chevronContentDescription = "Open about, updates and legal settings",
        isTightFont = isTightFont,
        horizontalPadding = horizontalPadding,
        testTag = "settings_about_legal_card",
        onClick = onOpenAboutAndLegal
    ) {
        SettingsStatusPill(
            text = if (availableUpdate != null) "Update" else "v${BuildConfig.VERSION_NAME}",
            containerColor = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (availableUpdate != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
