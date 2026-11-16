package com.lectures.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.components.RoomFloorBadge
import java.util.Locale

private val INVALID_ROOM_MARKERS = listOf("TBA", "TBD", "NONE", "N/A")

@Composable
internal fun DiscardNoteConfirmDialog(
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Discard unsaved changes?",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "You have unsaved changes to this note. Leaving now will cause edits to be lost.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Discard", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Editing")
            }
        }
    )
}

@Composable
internal fun NoteCoursePickerDialog(
    selectedCourse: String,
    availableCourses: List<CourseOption>,
    onSelectCourse: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customCourseCode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Course", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Choose from your enrolled courses or enter a custom course code:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                availableCourses.forEach { course ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCourse(course.code)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedCourse.equals(course.code, ignoreCase = true)) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try { Color(android.graphics.Color.parseColor(course.color)) } catch (_: Exception) { Color(0xFF2563EB) }
                                    )
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(course.code, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(course.title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Custom Course Code:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customCourseCode,
                        onValueChange = { customCourseCode = it.uppercase(Locale.ROOT) },
                        placeholder = { Text("e.g. CSC3100") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (customCourseCode.isNotBlank()) {
                                onSelectCourse(customCourseCode.trim())
                                onDismiss()
                            }
                        },
                        enabled = customCourseCode.isNotBlank()
                    ) {
                        Text("Set")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun NoteTagPickerDialog(
    currentTag: String,
    onSelectTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val standardTags = remember {
        listOf(
            "Lecture" to "General classroom notes & discussions",
            "Lab Prep" to "Practical assignments, code logs & experiments",
            "Study Guide" to "Comprehensive summaries & textbook synthesis",
            "Assignment" to "Coursework breakdown, research & references",
            "Revision" to "High-yield exam prep, formulas & confidence tracker",
            "Exam Prep" to "Targeted past papers & test solutions",
            "Quick Note" to "Lightweight thoughts & immediate reminders"
        )
    }
    var customTagInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Note Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                standardTags.forEach { (tagName, desc) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectTag(tagName)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentTag == tagName) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("🏷️ $tagName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Custom Tag:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customTagInput,
                        onValueChange = { customTagInput = it },
                        placeholder = { Text("e.g. Fieldwork") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (customTagInput.isNotBlank()) {
                                onSelectTag(customTagInput.trim())
                                onDismiss()
                            }
                        },
                        enabled = customTagInput.isNotBlank()
                    ) {
                        Text("Set")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun NoteClassPickerDialog(
    attachedClassText: String,
    selectedCourse: String,
    timetableClasses: List<TimetableEntry>,
    onDetachClass: () -> Unit,
    onAttachClass: (attachedText: String, attachedLoc: String, lecturerName: String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attach Timetable Class", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (attachedClassText.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            onDetachClass()
                            onDismiss()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Detach Current Class")
                    }
                }

                val matchingClasses = timetableClasses.filter {
                    it.courseCode.equals(selectedCourse, ignoreCase = true)
                }.ifEmpty { timetableClasses }

                if (matchingClasses.isEmpty()) {
                    Text("No specific timetable classes found. You can still write notes without an attached class.")
                } else {
                    matchingClasses.forEach { entry ->
                        val validRoom = entry.room?.trim()?.takeIf {
                            it.isNotBlank() && it.uppercase(Locale.ROOT) !in INVALID_ROOM_MARKERS
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val classText = "${entry.courseCode}: ${entry.courseTitle} · ${entry.day}, ${entry.startTime} – ${entry.endTime}"
                                    val locText = if (validRoom != null) "📍 $validRoom" else ""
                                    onAttachClass(classText, locText, entry.lecturer?.trim().orEmpty())
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("${entry.courseCode} · ${entry.courseTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                val roomPart = if (validRoom != null) " · $validRoom" else ""
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${entry.day} ${entry.startTime} - ${entry.endTime}$roomPart", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (validRoom != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        RoomFloorBadge(roomName = entry.room ?: "", compact = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
internal fun NoteTemplateConfirmDialog(
    targetTemplate: NoteTemplate,
    onConfirmReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Apply \"${targetTemplate.title}\"?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Your current note already contains text. Applying a template will replace the existing note body.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirmReplace,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Replace Content")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Current")
            }
        }
    )
}

@Composable
internal fun CustomPreClassAlertDialog(
    initialMinutesText: String,
    onApply: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var customReminderText by remember(initialMinutesText) { mutableStateOf(initialMinutesText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Pre-Class Alert", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Minutes before class begins:", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customReminderText,
                    onValueChange = { customReminderText = it.filter { ch -> ch.isDigit() }.take(4) },
                    label = { Text("Minutes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = customReminderText.toIntOrNull()?.coerceIn(1, 1440)
                if (parsed != null) onApply(parsed)
                onDismiss()
            }) {
                Text("Apply", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
