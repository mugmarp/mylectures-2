package com.lectures.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.lectures.ui.components.desktopHoverable

@Composable
internal fun SettingsAppearanceSection(
    appTheme: AppThemeState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onOpenAppearance: () -> Unit
) {
    val ext = LocalExtendedColors.current
    SectionHeader("APPEARANCE & THEME", horizontalPadding = horizontalPadding + 4.dp)
    val activeAccentIdx = appTheme.accentIndex.coerceIn(0, AccentColorNames.lastIndex)
    val activeAccentName = AccentColorNames[activeAccentIdx]
    val displaySwatch = accentDisplayColors(appTheme.isDark)[activeAccentIdx]
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .clickable(onClick = onOpenAppearance)
            .testTag("settings_appearance_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Appearance & Theme",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (appTheme.themeMode) {
                        ThemeMode.LIGHT -> "Light mode · $activeAccentName"
                        ThemeMode.DARK -> "Dark mode · $activeAccentName"
                        ThemeMode.SYSTEM -> "System (${if (appTheme.isDark) "Dark" else "Light"}) · $activeAccentName"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isTightFont) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(displaySwatch)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Customize",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .clickable(onClick = onOpenDataAndBackup)
            .testTag("settings_data_sync_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudSync,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Data, Cloud Sync & Reset",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val modePrefix = if (uiState.isCloudSyncActive) "Cloud synced" else "Guest mode"
                Text(
                    text = "$modePrefix · ${uiState.userNotesCount} notes · ${uiState.userAssignmentsCount} tasks",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isTightFont) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (uiState.isCloudSyncActive) ext.successBg else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (uiState.isCloudSyncActive) "Synced" else "Local",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.isCloudSyncActive) ext.successText else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open data, cloud sync and reset settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
internal fun SettingsAboutSummaryCard(
    uiState: SettingsUiState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onOpenAboutAndLegal: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val availableUpdate = uiState.availableUpdateInfo
    SectionHeader("ABOUT & UPDATES", horizontalPadding = horizontalPadding + 4.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .clickable(onClick = onOpenAboutAndLegal)
            .testTag("settings_about_legal_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (availableUpdate != null) Icons.Outlined.SystemUpdate else Icons.Outlined.Info,
                    contentDescription = null,
                    tint = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "About, Updates & Legal",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (availableUpdate != null) {
                        "Update v${availableUpdate.versionName} available · Releases & policies"
                    } else {
                        "v${BuildConfig.VERSION_NAME} · Timetable release, feedback & policies"
                    },
                    fontSize = 12.sp,
                    color = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isTightFont) 2 else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (availableUpdate != null) "Update" else "v${BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (availableUpdate != null) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open about, updates and legal settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
