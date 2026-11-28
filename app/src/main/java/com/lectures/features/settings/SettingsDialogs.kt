package com.lectures.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.lectures.features.rooms.UniversityDirectory
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.rememberLecturesWindow
import java.util.Locale

@Composable
internal fun CustomLeadMinutesDialog(
    input: String,
    onInputChange: (String) -> Unit,
    onSave: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    val parsedMinutes = input.toIntOrNull()
    val isValidMinutes = parsedMinutes != null && parsedMinutes in 1..180
    val showValidationError = input.isNotEmpty() && !isValidMinutes
    val saveAction: () -> Unit = {
        if (parsedMinutes != null && parsedMinutes in 1..180) {
            onSave(parsedMinutes)
            onDismiss()
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 360.dp, max = 480.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        icon = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Custom Lecture Reminder", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter how many minutes before class start you want to be reminded (1–180 minutes, e.g. 20, 30, 45):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { raw ->
                        if (raw.all { it.isDigit() } && raw.length <= 3) {
                            onInputChange(raw)
                        }
                    },
                    placeholder = { Text("30") },
                    singleLine = true,
                    isError = showValidationError,
                    supportingText = {
                        Text(
                            text = if (showValidationError) {
                                "Enter a value between 1 and 180 minutes."
                            } else {
                                "Allowed range: 1 to 180 minutes"
                            },
                            modifier = Modifier.testTag("dialog_custom_lead_helper")
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { saveAction() }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_custom_lead_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = saveAction,
                enabled = isValidMinutes,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor(enabled = isValidMinutes)
                    .testTag("dialog_custom_lead_save_btn")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun ResetNotesConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 360.dp, max = 480.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Reset All Notes?", fontWeight = FontWeight.Bold) },
        text = { Text("This will permanently delete all your lecture notes and attached class alarm intervals. This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_reset_notes_confirm_btn")
            ) {
                Text("Delete Notes")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun ResetTasksConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 360.dp, max = 480.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 420.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        icon = { Icon(Icons.Outlined.AlarmOff, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text("Reset Academic Tasks?", fontWeight = FontWeight.Bold) },
        text = { Text("This will remove all saved assignments and cancel any scheduled reminder notifications.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_reset_tasks_confirm_btn")
            ) {
                Text("Clear Tasks & Alarms")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun VersionDetailsDialog(
    uiState: SettingsUiState,
    onDismiss: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    val meta = uiState.metadata
    val isFinal = uiState.isFinal || uiState.timetableVersion.contains("Final", ignoreCase = true)
    val stageLabel = if (isFinal) "Official Final Release" else "Active Draft ${meta?.draftNumber ?: ""}".trim()
    val badgeBg = if (isFinal) ext.successBg else ext.warningBg
    val badgeColor = if (isFinal) ext.successText else ext.warningText

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 380.dp, max = 520.dp)
                } else {
                    Modifier.padding(horizontal = 16.dp).widthIn(max = 440.dp)
                }
            )
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(badgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    uiState.timetableVersion,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                SettingsStatusPill(
                    text = stageLabel,
                    containerColor = badgeBg,
                    contentColor = badgeColor
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = meta?.releaseNotes ?: if (isFinal) {
                        "Authoritative teaching schedule approved and ratified by MUST Academic Registrar."
                    } else {
                        "Draft schedule subject to departmental lecture and lab reviews."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(color = ext.borderSubtle)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Academic Session", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${meta?.academicYear ?: "2026/2027"} · ${meta?.semester ?: "Semester 1"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Issuing Authority", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        meta?.source ?: "MUST Academic Registrar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Scheduled Sessions", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${meta?.totalSessions ?: 1304} Sessions · ${UniversityDirectory.ALL_ROOMS.size} Rooms",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Draft Lifecycle", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isFinal) "Final Ratified" else "Dynamic Review",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_version_close_btn")
            ) {
                Text("Close")
            }
        }
    )
}
