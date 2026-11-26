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
    val saveAction: () -> Unit = {
        val mins = input.toIntOrNull()
        if (mins != null && mins > 0) {
            onSave(mins)
        }
        onDismiss()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 360.dp, max = 480.dp) else Modifier)
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        icon = { Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Custom Lecture Reminder", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter how many minutes before class start you want to be reminded (e.g. 20, 30, 40):",
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
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { saveAction() }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = saveAction,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
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
internal fun DisclaimerDialog(onDismiss: () -> Unit) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 420.dp, max = 560.dp) else Modifier)
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        title = { Text("Institutional Non-Affiliation Disclaimer", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Independent Development",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is an independent academic timetable and study companion designed to help students organize classes, study notes, and coursework tasks.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. No University Affiliation or Endorsement",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is NOT an official application of, nor is it endorsed by, sponsored by, affiliated with, or in any way officially associated with Mbarara University of Science and Technology (MUST) or any of its constituent faculties, departments, or administrative offices.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Informational & Convenience Purpose",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "All course codes, faculty designations, module names, and timetable slots are referenced solely for descriptive and schedule organization purposes for students. Students are advised to consult official university online portals (timetable.must.ac.ug) and administrative systems for official announcements, venue changes, and examination schedules.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. Academic Schedule Accuracy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Timetable data is maintained offline and locally on your device. Students should verify critical announcements and examinations with their respective departments.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_disclaimer_confirm_btn")
            ) {
                Text("I Understand")
            }
        }
    )
}

@Composable
internal fun TermsDialog(onDismiss: () -> Unit) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 420.dp, max = 560.dp) else Modifier)
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        title = { Text("Disclaimer & Terms", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Permitted Use",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is provided for personal, non-commercial academic management and learning assistance. Users agree to use the application lawfully and ethically.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. 'As Is' Disclaimer of Warranty",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "The software is provided on an 'AS IS' basis without warranties of any kind. While every effort is made to maintain schedule accuracy, the developer is not responsible for missed classes, test clashes, device alarm failures, or timetable discrepancies.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Developer Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Developed by MUGENDAWALA MARK PAUL as an independent student tool for academic schedule tracking, vacant study hall finding, and offline coursework productivity.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. User Data & Notes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "All notes, summaries, checklists, and task entries are authored by the student and stored privately on the local device.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "5. Data Privacy & Local-First Storage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "All student coursework, personal lecture notes, revision tasks, and schedule preferences are stored locally on your device in secure SQLite database tables.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_terms_confirm_btn")
            ) {
                Text("I Understand")
            }
        }
    )
}

@Composable
internal fun PrivacyDialog(onDismiss: () -> Unit) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 420.dp, max = 560.dp) else Modifier)
            .onEscapeKey(onDismiss),
        containerColor = ext.surfaceCard,
        title = { Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "1. Local-First Student Data Storage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Lectures is designed local-first. Your timetable schedules, lecture notes, tasks, alarms, and personal checklists are stored securely on your device in SQLite database tables.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. Pseudonymous Service Diagnostics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "To ensure timetable schedule reliability, monitor cohort coverage, and diagnose device compatibility across MUST campuses, the app transmits pseudonymous technical telemetry (an anonymous installation UUID, device model, Android OS version, launch count, and enrolled academic programme) to our secure database. We do not collect, monetize, or sell names, email addresses, phone numbers, or lecture notes.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Permissions Usage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Notification and exact alarm permissions are requested solely to ring local alerts before scheduled lectures and task deadlines.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
                    .testTag("dialog_privacy_confirm_btn")
            ) {
                Text("Done")
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
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 360.dp, max = 480.dp) else Modifier)
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
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 360.dp, max = 480.dp) else Modifier)
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
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(if (windowInfo.preferDesktopDialogs) Modifier.widthIn(min = 380.dp, max = 520.dp) else Modifier)
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
