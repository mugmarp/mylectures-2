package com.lectures.features.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.lectures.features.tasks.AddTaskDialog
import com.lectures.features.timetable.domain.ActivityCategory
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.ui.AddActivitySheet
import com.lectures.features.timetable.ui.LectureDetailSheet
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.rememberLecturesWindow

@Composable
internal fun CalendarActionDialogs(
    selectedActivityForDetail: CustomEvent?,
    activityToEdit: CustomEvent?,
    taskToEdit: Assignment?,
    selectedLectureForDetail: TimetableEntry?,
    selectedLectureNote: LectureNote? = null,
    showAddActivitySheet: Boolean,
    showProfileSheet: Boolean,
    eventToDelete: CustomEvent?,
    taskToDelete: Assignment?,
    currentProgramme: String,
    enrolledProgramme: String,
    isPeeking: Boolean,
    selectedDayOfWeek: String,
    isDark: Boolean,
    availableCoursePairs: List<Pair<String, String>>,
    defaultReminderMinutes: Int,
    onDismissActivityDetail: () -> Unit,
    onEditActivity: (Long) -> Unit,
    onRequestDeleteActivity: (Long) -> Unit,
    onDismissEditActivity: () -> Unit,
    onSaveActivity: (CustomEvent) -> Unit,
    onDismissEditTask: () -> Unit,
    onSaveTask: (Assignment) -> Unit,
    onSaveLectureNote: (TimetableEntry, String, Int?) -> Unit,
    onDismissLectureDetail: () -> Unit,
    onDismissAddActivity: () -> Unit,
    onDismissProfileSheet: () -> Unit,
    onConfirmDeleteActivity: (Long) -> Unit,
    onDismissDeleteActivity: () -> Unit,
    onConfirmDeleteTask: (Assignment) -> Unit,
    onDismissDeleteTask: () -> Unit,
    onPeekProgramme: (String) -> Unit,
    onResetToEnrolled: () -> Unit,
    onSetOfficialProgramme: (String) -> Unit,
    onReconfigureAcademicProfile: () -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToTimetable: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val useWideDialogs = windowInfo.preferDesktopDialogs

    selectedActivityForDetail?.let { event ->
        AlertDialog(
            onDismissRequest = onDismissActivityDetail,
            properties = DialogProperties(usePlatformDefaultWidth = !useWideDialogs),
            modifier = Modifier
                .then(if (useWideDialogs) Modifier.widthIn(min = 360.dp, max = 480.dp) else Modifier)
                .onEscapeKey(onDismissActivityDetail),
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
                        text = "Category: ${ActivityCategory.fromName(event.category).displayName}",
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
                    TextButton(
                        onClick = {
                            val toEditId = event.id
                            onDismissActivityDetail()
                            onEditActivity(toEditId)
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                    ) {
                        Text("Edit", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onDismissActivityDetail,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                    ) {
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
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

    if (taskToEdit != null) {
        AddTaskDialog(
            isDark = isDark,
            initialTask = taskToEdit,
            availableCourses = availableCoursePairs,
            defaultReminderMinutes = defaultReminderMinutes,
            onDismiss = onDismissEditTask,
            onConfirm = { title, course, dueDate, priority, reminderMinutes, notes, category ->
                val fullTitle = if (category != TaskCategory.GENERAL) {
                    "[${category.displayName}] ${title.trim()}"
                } else {
                    title.trim()
                }
                onSaveTask(
                    taskToEdit.copy(
                        title = fullTitle,
                        courseCode = course,
                        dueDate = dueDate,
                        priority = priority,
                        reminderMinutes = reminderMinutes,
                        notes = notes
                    )
                )
                onDismissEditTask()
            }
        )
    }

    if (selectedLectureForDetail != null) {
        LectureDetailSheet(
            entry = selectedLectureForDetail,
            initialNote = selectedLectureNote?.content ?: "",
            initialReminder = selectedLectureNote?.alarmMinutes,
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
            isPeeking = isPeeking,
            onDismiss = onDismissProfileSheet,
            onPeekProgramme = { peekGroup ->
                onDismissProfileSheet()
                onPeekProgramme(peekGroup)
                onNavigateToTimetable()
            },
            onResetToEnrolled = {
                onDismissProfileSheet()
                onResetToEnrolled()
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
            properties = DialogProperties(usePlatformDefaultWidth = !useWideDialogs),
            modifier = Modifier
                .then(if (useWideDialogs) Modifier.widthIn(min = 340.dp, max = 460.dp) else Modifier)
                .onEscapeKey(onDismissDeleteActivity),
            title = { Text("Delete Activity") },
            text = { Text("Are you sure you want to delete \"${event.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteActivity(event.id) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteActivity,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = onDismissDeleteTask,
            properties = DialogProperties(usePlatformDefaultWidth = !useWideDialogs),
            modifier = Modifier
                .then(if (useWideDialogs) Modifier.widthIn(min = 340.dp, max = 460.dp) else Modifier)
                .onEscapeKey(onDismissDeleteTask),
            title = { Text("Delete Deadline") },
            text = { Text("Are you sure you want to delete \"${task.title}\"?") },
            confirmButton = {
                TextButton(
                    onClick = { onConfirmDeleteTask(task) },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissDeleteTask,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
