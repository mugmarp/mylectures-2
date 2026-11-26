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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
    onRequestResetNotes: () -> Unit,
    onRequestResetTasks: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val headerHorizontalPadding = horizontalPadding + 4.dp

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        SectionHeader("ACCOUNT & CLOUD SYNC", horizontalPadding = headerHorizontalPadding)
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
                if (uiState.isCloudSyncActive && uiState.userAccountEmail != null) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stackActions = maxWidth < 370.dp || isTightFont
                        val initial = uiState.userAccountEmail.firstOrNull()?.uppercaseChar() ?: 'U'
                        val identityRow: @Composable (Modifier) -> Unit = { rowMod ->
                            Row(
                                modifier = rowMod,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initial.toString(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Signed in as",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        uiState.userAccountEmail,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(ext.successIcon, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            uiState.userDataBackupStatus,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = ext.successText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                        val syncCloudButton: @Composable (Modifier) -> Unit = { btnMod ->
                            FilledTonalButton(
                                onClick = onSyncUserData,
                                enabled = !uiState.isBackingUpUserData,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = btnMod
                                    .defaultMinSize(minHeight = 48.dp)
                                    .desktopHandCursor(enabled = !uiState.isBackingUpUserData)
                                    .testTag("settings_sync_cloud_btn")
                            ) {
                                if (uiState.isBackingUpUserData) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Text(
                                        "Sync Cloud",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        val manageButton: @Composable (Modifier) -> Unit = { btnMod ->
                            Button(
                                onClick = onOpenAccount,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = btnMod
                                    .defaultMinSize(minHeight = 48.dp)
                                    .desktopHandCursor()
                                    .testTag("settings_manage_account_btn")
                            ) {
                                Text(
                                    "Manage",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (stackActions) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                identityRow(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .desktopHoverable()
                                        .desktopHandCursor()
                                        .clickable(onClick = onOpenAccount)
                                        .padding(vertical = 4.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    syncCloudButton(Modifier.weight(1f))
                                    manageButton(Modifier.weight(1f))
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .desktopHoverable()
                                    .desktopHandCursor()
                                    .clickable(onClick = onOpenAccount)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                identityRow(Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    syncCloudButton(Modifier)
                                    manageButton(Modifier)
                                }
                            }
                        }
                    }
                } else {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val stackSignIn = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
                        val guestInfoRow: @Composable (Modifier) -> Unit = { rowMod ->
                            Row(
                                modifier = rowMod,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.CloudSync,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Google Account & Cloud Backup",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "Guest mode · Link account to sync across devices",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        val signInBtn: @Composable (Modifier) -> Unit = { btnMod ->
                            Button(
                                onClick = onOpenAccount,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = btnMod
                                    .defaultMinSize(minHeight = 48.dp)
                                    .desktopHandCursor()
                                    .testTag("settings_google_signin_btn")
                            ) {
                                Icon(Icons.Outlined.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (stackSignIn) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                guestInfoRow(
                                    Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .desktopHoverable()
                                        .desktopHandCursor()
                                        .clickable(onClick = onOpenAccount)
                                        .padding(vertical = 4.dp)
                                )
                                signInBtn(Modifier.fillMaxWidth())
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .desktopHoverable()
                                    .desktopHandCursor()
                                    .clickable(onClick = onOpenAccount)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                guestInfoRow(Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(8.dp))
                                signInBtn(Modifier)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackPreserved = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
                    val preservedModifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .desktopHoverable()
                        .desktopHandCursor()
                        .clickable(onClick = onOpenAccount)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                    val preservedLabel = "Preserved: ${uiState.userNotesCount} Notes · ${uiState.userEventsCount} Events · ${uiState.userAssignmentsCount} Tasks"
                    val statusLabel = if (uiState.isCloudSyncActive) "Synced" else "Offline-Safe"

                    if (stackPreserved) {
                        Column(
                            modifier = preservedModifier,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = preservedLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            SettingsStatusPill(
                                text = statusLabel,
                                containerColor = ext.successBg,
                                contentColor = ext.successText
                            )
                        }
                    } else {
                        Row(
                            modifier = preservedModifier,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preservedLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            SettingsStatusPill(
                                text = statusLabel,
                                containerColor = ext.successBg,
                                contentColor = ext.successText
                            )
                        }
                    }
                }
            }
        }

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
            }
        }

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
private fun DangerResetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
