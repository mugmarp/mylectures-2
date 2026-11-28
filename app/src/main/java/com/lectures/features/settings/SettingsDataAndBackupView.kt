package com.lectures.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun DataAndBackupSettingsView(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    uiState: SettingsUiState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    bottomContentPadding: Dp = 32.dp,
    onSyncTimetable: () -> Unit,
    onOpenAccount: () -> Unit,
    onSyncUserData: () -> Unit,
    onExportBackup: (String) -> Unit,
    onImportBackup: () -> Unit = {},
    onRequestResetNotes: () -> Unit,
    onRequestResetTasks: () -> Unit,
    onRequestResetEvents: () -> Unit = {}
) {
    val ext = LocalExtendedColors.current
    val headerHorizontalPadding = horizontalPadding + 4.dp

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        SectionHeader("ACCOUNT & CLOUD SYNC", horizontalPadding = headerHorizontalPadding)
        CloudAccountSyncCard(
            uiState = uiState,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenAccount = onOpenAccount,
            onSyncUserData = onSyncUserData
        )

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("TIMETABLE & LOCAL BACKUP", horizontalPadding = headerHorizontalPadding)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackSyncBtn = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
                    val syncInfoRow: @Composable (Modifier) -> Unit = { rowMod ->
                        Row(
                            modifier = rowMod,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Outlined.Sync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Synchronize Timetable",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    uiState.lastSyncTime,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    val syncNowButton: @Composable (Modifier) -> Unit = { btnMod ->
                        Button(
                            onClick = onSyncTimetable,
                            enabled = !uiState.isSyncing,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = btnMod
                                .defaultMinSize(minHeight = 48.dp)
                                .desktopHandCursor(enabled = !uiState.isSyncing)
                                .testTag("settings_sync_timetable_btn")
                        ) {
                            Text(
                                if (uiState.isSyncing) "Syncing..." else "Sync Now",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (stackSyncBtn) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            syncInfoRow(Modifier.fillMaxWidth())
                            syncNowButton(Modifier.fillMaxWidth())
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            syncInfoRow(Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            syncNowButton(Modifier)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = ext.borderSubtle
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackExport = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
                    val exportAction: () -> Unit = {
                        val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                        onExportBackup("lectures_backup_$stamp.json")
                    }
                    val exportModifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .desktopHoverable()
                        .desktopHandCursor()
                        .clickable(onClick = exportAction)
                        .testTag("settings_export_backup_row")
                        .padding(vertical = 6.dp, horizontal = 4.dp)

                    val exportLeading: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    val exportText: @Composable (Modifier) -> Unit = { textMod ->
                        Column(modifier = textMod) {
                            Text(
                                "Export Data Backup (JSON)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Save notes, coursework & custom events offline",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val exportPill: @Composable () -> Unit = {
                        SettingsStatusPill(
                            text = "Export",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (stackExport) {
                        Column(
                            modifier = exportModifier,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                exportLeading()
                                Spacer(modifier = Modifier.width(12.dp))
                                exportText(Modifier.weight(1f))
                            }
                            Box(modifier = Modifier.padding(start = 50.dp)) {
                                exportPill()
                            }
                        }
                        } else {
                            Row(
                                modifier = exportModifier,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                exportLeading()
                                Spacer(modifier = Modifier.width(12.dp))
                                exportText(Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                exportPill()
                            }
                        }
                    }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = ext.borderSubtle
                )

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackImport = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
                    val importModifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .desktopHoverable()
                        .desktopHandCursor()
                        .clickable(onClick = onImportBackup)
                        .testTag("settings_import_backup_row")
                        .padding(vertical = 6.dp, horizontal = 4.dp)

                    val importLeading: @Composable () -> Unit = {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Upload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    val importText: @Composable (Modifier) -> Unit = { textMod ->
                        Column(modifier = textMod) {
                            Text(
                                "Import Data Backup (JSON)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Restore exported notes, coursework & custom events",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val importPill: @Composable () -> Unit = {
                        SettingsStatusPill(
                            text = "Restore",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (stackImport) {
                        Column(
                            modifier = importModifier,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                importLeading()
                                Spacer(modifier = Modifier.width(12.dp))
                                importText(Modifier.weight(1f))
                            }
                            Box(modifier = Modifier.padding(start = 50.dp)) {
                                importPill()
                            }
                        }
                    } else {
                        Row(
                            modifier = importModifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            importLeading()
                            Spacer(modifier = Modifier.width(12.dp))
                            importText(Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            importPill()
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("DANGER ZONE", color = MaterialTheme.colorScheme.error, horizontalPadding = headerHorizontalPadding)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DangerResetRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Reset All Lecture Notes",
                    subtitle = "Permanently delete saved notes and attachments",
                    isTightFont = isTightFont,
                    testTag = "settings_reset_notes_row",
                    onClick = onRequestResetNotes
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = ext.borderSubtle
                )

                DangerResetRow(
                    icon = Icons.Outlined.AlarmOff,
                    title = "Reset Academic Tasks & Alarms",
                    subtitle = "Cancel all scheduled alarms and clear coursework",
                    isTightFont = isTightFont,
                    testTag = "settings_reset_tasks_row",
                    onClick = onRequestResetTasks
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = ext.borderSubtle
                )

                DangerResetRow(
                    icon = Icons.Outlined.EventBusy,
                    title = "Reset Personal Activities & Events",
                    subtitle = "Cancel activity alarms and clear custom timetable events",
                    isTightFont = isTightFont,
                    testTag = "settings_reset_events_row",
                    onClick = onRequestResetEvents
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
private fun DangerResetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isTightFont: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackPill = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
        val rowModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .desktopHoverable()
            .desktopHandCursor()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 8.dp, horizontal = 6.dp)
        val resetPill: @Composable () -> Unit = {
            SettingsStatusPill(
                text = "Reset",
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                contentColor = MaterialTheme.colorScheme.error
            )
        }

        if (stackPill) {
            Column(
                modifier = rowModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(modifier = Modifier.padding(start = 38.dp)) {
                    resetPill()
                }
            }
        } else {
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                resetPill()
            }
        }
    }
}
