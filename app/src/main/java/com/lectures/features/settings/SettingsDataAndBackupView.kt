package com.lectures.features.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Download
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
    onSyncTimetable: () -> Unit,
    onOpenAccount: () -> Unit,
    onSyncUserData: () -> Unit,
    onExportBackup: (String) -> Unit,
    onRequestResetNotes: () -> Unit,
    onRequestResetTasks: () -> Unit
) {
    val ext = LocalExtendedColors.current

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(vertical = 14.dp)
    ) {
        SectionHeader("ACCOUNT & CLOUD SYNC")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (uiState.isCloudSyncActive && uiState.userAccountEmail != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .desktopHoverable()
                            .clickable(onClick = onOpenAccount)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val initial = uiState.userAccountEmail.firstOrNull()?.uppercaseChar() ?: 'U'
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalButton(
                                onClick = onSyncUserData,
                                enabled = !uiState.isBackingUpUserData,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .desktopHandCursor(enabled = !uiState.isBackingUpUserData)
                            ) {
                                if (uiState.isBackingUpUserData) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("Sync Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Button(
                                onClick = onOpenAccount,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    contentColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .desktopHandCursor()
                            ) {
                                Text("Manage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .desktopHoverable()
                            .clickable(onClick = onOpenAccount)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CloudSync,
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onOpenAccount,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .desktopHandCursor()
                                .testTag("settings_google_signin_btn")
                        ) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .desktopHandCursor()
                        .clickable(onClick = onOpenAccount)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Preserved: ${uiState.userNotesCount} Notes · ${uiState.userEventsCount} Events · ${uiState.userAssignmentsCount} Tasks",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .background(ext.successBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (uiState.isCloudSyncActive) "SYNCED" else "OFFLINE-SAFE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = ext.successText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("TIMETABLE & LOCAL BACKUP")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                                Icons.Default.Sync,
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSyncTimetable,
                        enabled = !uiState.isSyncing,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor(enabled = !uiState.isSyncing)
                    ) {
                        Text(
                            if (uiState.isSyncing) "Syncing..." else "Sync Now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .desktopHoverable()
                        .clickable {
                            val stamp = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                            onExportBackup("lectures_backup_$stamp.json")
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "EXPORT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("DANGER ZONE", color = MaterialTheme.colorScheme.error)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .desktopHoverable()
                        .clickable(onClick = onRequestResetNotes)
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Reset All Lecture Notes",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Permanently delete saved notes and attachments",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .desktopHoverable()
                        .clickable(onClick = onRequestResetTasks)
                        .padding(vertical = 8.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AlarmOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Reset Academic Tasks & Alarms",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Cancel all scheduled alarms and clear coursework",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Reset",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
