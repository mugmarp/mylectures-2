package com.lectures.features.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.ui.AddActivitySheet
import com.lectures.features.timetable.ui.LectureDetailSheet
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.RoomFloorBadge

@Composable
internal fun CalendarActionDialogs(
    selectedActivityForDetail: CustomEvent?,
    activityToEdit: CustomEvent?,
    selectedLectureForDetail: TimetableEntry?,
    showAddActivitySheet: Boolean,
    showProfileSheet: Boolean,
    eventToDelete: CustomEvent?,
    taskToDelete: Assignment?,
    currentProgramme: String,
    enrolledProgramme: String,
    selectedDayOfWeek: String,
    onDismissActivityDetail: () -> Unit,
    onEditActivity: (Long) -> Unit,
    onRequestDeleteActivity: (Long) -> Unit,
    onDismissEditActivity: () -> Unit,
    onSaveActivity: (CustomEvent) -> Unit,
    onSaveLectureNote: (TimetableEntry, String, Int?) -> Unit,
    onDismissLectureDetail: () -> Unit,
    onDismissAddActivity: () -> Unit,
    onDismissProfileSheet: () -> Unit,
    onConfirmDeleteActivity: (Long) -> Unit,
    onDismissDeleteActivity: () -> Unit,
    onConfirmDeleteTask: (Assignment) -> Unit,
    onDismissDeleteTask: () -> Unit,
    onPeekProgramme: (String) -> Unit,
    onSetOfficialProgramme: (String) -> Unit,
    onReconfigureAcademicProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    selectedActivityForDetail?.let { event ->
        AlertDialog(
            onDismissRequest = onDismissActivityDetail,
            title = {
                Text(
                    text = event.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Category: ${event.category}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Time: ${event.dayOfWeek} · ${event.startTime} - ${event.endTime}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (event.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Venue: ${event.location}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            RoomFloorBadge(roomName = event.location, compact = true)
                        }
                    }
                    if (event.notes.isNotBlank()) {
                        Text(
                            text = "Notes: ${event.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        val toEditId = event.id
                        onDismissActivityDetail()
                        onEditActivity(toEditId)
                    }) {
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onDismissActivityDetail) {
                        Text("Close")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val toDelId = event.id
                        onDismissActivityDetail()
                        onRequestDeleteActivity(toDelId)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Activity")
                }
            }
        )
    }

    if (activityToEdit != null) {
        AddActivitySheet(
            eventToEdit = activityToEdit,
            onDismiss = onDismissEditActivity,
            onSave = { updatedEvent ->
                onSaveActivity(updatedEvent)
                onDismissEditActivity()
            }
        )
    }

    if (selectedLectureForDetail != null) {
        LectureDetailSheet(
            entry = selectedLectureForDetail,
            onSaveNote = { noteText, reminderMinutes ->
                onSaveLectureNote(selectedLectureForDetail, noteText, reminderMinutes)
            },
            onDismiss = onDismissLectureDetail
        )
    }

    if (showAddActivitySheet) {
        AddActivitySheet(
            initialDayOfWeek = selectedDayOfWeek,
            onDismiss = onDismissAddActivity,
            onSave = onSaveActivity
        )
    }

    if (showProfileSheet) {
        AcademicProfileSheet(
            programme = currentProgramme,
            enrolledProgramme = enrolledProgramme,
            onDismiss = onDismissProfileSheet,
            onPeekProgramme = { peekGroup ->
                onDismissProfileSheet()
                onPeekProgramme(peekGroup)
                onNavigateToTimetable()
            },
            onSetOfficialProgramme = { newGroup ->
                onDismissProfileSheet()
                onSetOfficialProgramme(newGroup)
                onNavigateToTimetable()
            },
            onReconfigureProfile = {
                onDismissProfileSheet()
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                onDismissProfileSheet()
                onSettingsClick()
            },
            onNavigateToTimetable = {
                onDismissProfileSheet()
                onNavigateToTimetable()
            }
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = onDismissDeleteActivity,
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete \"${event.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteActivity(event.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteActivity) {
                    Text("Cancel")
                }
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = onDismissDeleteTask,
            title = { Text("Delete Deadline") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteTask(task) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteTask) {
                    Text("Cancel")
                }
            }
        )
    }
}
