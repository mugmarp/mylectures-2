package com.lectures.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.AdaptiveSheetOrDialog
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onCtrlEnter

@Composable
fun LectureDetailSheet(
    entry: TimetableEntry,
    initialNote: String = "",
    initialReminder: Int? = null,
    onSaveNote: (String, Int?) -> Unit,
    onDismiss: () -> Unit
) {
    AdaptiveSheetOrDialog(
        onDismissRequest = onDismiss,
        sheetTestTag = "lecture_detail_sheet",
        containerColor = LocalExtendedColors.current.surfaceCard,
        desktopMaxWidth = 560.dp
    ) {
        LectureDetailContent(
            entry = entry,
            initialNote = initialNote,
            initialReminder = initialReminder,
            onSaveNote = { text, rem ->
                onSaveNote(text, rem)
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LectureDetailContent(
    entry: TimetableEntry,
    initialNote: String = "",
    initialReminder: Int? = null,
    onSaveNote: (String, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val ext = LocalExtendedColors.current
    val textColor = ext.textPrimary
    val textSub = ext.textMuted
    val inputBg = ext.surfaceBase
    val inputBorder = ext.borderSubtle

    var noteText by remember(entry.naturalKey, initialNote) { mutableStateOf(initialNote) }
    val standardReminderOptions = listOf(5, 10, 15, 30, 45, 60)
    val isCustomInitial = initialReminder != null && initialReminder !in standardReminderOptions
    var isCustomReminder by remember(entry.naturalKey, initialReminder) { mutableStateOf(isCustomInitial) }
    var customReminderText by remember(entry.naturalKey, initialReminder) {
        mutableStateOf(if (isCustomInitial) initialReminder.toString() else "")
    }
    var selectedReminder by remember(entry.naturalKey, initialReminder) {
        mutableStateOf<Int?>(initialReminder ?: 30)
    }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val effectiveMinutesToSave = if (isCustomReminder) customReminderText.toIntOrNull() else selectedReminder

    Column(
        modifier = modifier.onCtrlEnter {
            focusManager.clearFocus()
            keyboardController?.hide()
            onSaveNote(noteText, effectiveMinutesToSave)
        }
    ) {
            Text(
                text = entry.courseTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${entry.courseCode} • ${entry.dayOfWeek} ${entry.startTime}-${entry.endTime}",
                fontSize = 14.sp,
                color = textSub
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            // Session Badge
            val sessionLabel = resolveSessionTypeLabel(entry)
            val sessionStyling = sessionStyle(entry.sessionType, entry.room ?: "", entry.courseTitle)
            Box(
                modifier = Modifier
                    .background(
                        sessionStyling.badgeBg.copy(alpha = if (isDark) 0.35f else 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        sessionStyling.badgeBg.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = sessionLabel,
                    color = if (isDark) sessionStyling.badgeBg else sessionStyling.accentBar,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Details rows
            val roomVal = entry.room?.trim()
            if (!roomVal.isNullOrBlank() && roomVal.uppercase(java.util.Locale.ROOT) !in listOf("TBA", "TBD", "NONE", "N/A")) {
                DetailRow(
                    icon = Icons.Outlined.MeetingRoom,
                    label = "Venue / Room",
                    value = roomVal,
                    isDark = isDark,
                    trailingContent = {
                        RoomFloorBadge(roomName = roomVal, floor = entry.floor, compact = false, isDark = isDark)
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            val lecturerVal = entry.lecturer?.trim()
            if (!lecturerVal.isNullOrBlank() && !lecturerVal.equals("Staff", ignoreCase = true)) {
                DetailRow(icon = Icons.Outlined.Person, label = "Lecturer", value = lecturerVal, isDark = isDark)
                Spacer(modifier = Modifier.height(12.dp))
            }
            DetailRow(icon = Icons.Outlined.Schedule, label = "Time", value = "${entry.startTime} – ${entry.endTime}", isDark = isDark)
            
            val isAssociation = entry.courseCode.contains("MUCOSA", ignoreCase = true) ||
                entry.courseTitle.contains("MUCOSA", ignoreCase = true) ||
                entry.courseTitle.contains("ASSOCIATION", ignoreCase = true)
            
            val allAttendingCohorts = if (isAssociation) emptyList() else (listOf(entry.program_group) + entry.shared_with)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (allAttendingCohorts.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                DetailRow(
                    icon = Icons.Default.School,
                    label = "Attending Classes",
                    value = allAttendingCohorts.joinToString(", "),
                    isDark = isDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(start = 30.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allAttendingCohorts.forEach { cohort ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ext.surfaceElevated,
                            border = BorderStroke(1.dp, ext.borderSubtle)
                        ) {
                            Text(
                                text = cohort,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ext.textSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val accentColor = MaterialTheme.colorScheme.primary
            // Reminders
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Class Alarm Reminder", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Presets and Custom Chip
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                standardReminderOptions.forEach { minutes ->
                    val isSelected = !isCustomReminder && selectedReminder == minutes
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) accentColor else Color.Transparent)
                            .border(
                                1.dp,
                                if (isSelected) accentColor else ext.borderSubtle,
                                RoundedCornerShape(20.dp)
                            )
                            .desktopHandCursor()
                            .clickable {
                                isCustomReminder = false
                                selectedReminder = if (isSelected) null else minutes
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${minutes}m before",
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else ext.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Custom chip
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isCustomReminder) accentColor else Color.Transparent)
                        .border(
                            1.dp,
                            if (isCustomReminder) accentColor else ext.borderSubtle,
                            RoundedCornerShape(20.dp)
                        )
                        .desktopHandCursor()
                        .clickable {
                            isCustomReminder = !isCustomReminder
                            if (!isCustomReminder) {
                                selectedReminder = 30
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Custom",
                        color = if (isCustomReminder) MaterialTheme.colorScheme.onPrimary else ext.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isCustomReminder) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            if (isCustomReminder) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = customReminderText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() }.take(3)
                        customReminderText = filtered
                        selectedReminder = filtered.toIntOrNull()
                    },
                    label = { Text("Custom minutes before class") },
                    placeholder = { Text("e.g. 20, 25, 40, 90") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Personal notes
            Text("Personal Notes & Study Points", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("Add lecture takeaways, exam prep notes...", color = textSub) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = inputBorder,
                    focusedContainerColor = inputBg,
                    unfocusedContainerColor = inputBg,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                )
            )

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Linked to: ${entry.courseCode} (${entry.dayOfWeek})",
                fontSize = 11.sp,
                color = textSub
            )

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSaveNote(noteText, effectiveMinutesToSave)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .desktopHandCursor(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = if (effectiveMinutesToSave != null) "Save Note & Set ${effectiveMinutesToSave}m Alarm" else "Save Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    isDark: Boolean,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val ext = LocalExtendedColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = ext.textMuted,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            label,
            color = ext.textMuted,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                value,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = ext.textPrimary
            )
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}
