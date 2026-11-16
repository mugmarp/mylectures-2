package com.lectures.features.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.features.timetable.ui.DarkBorderSubtle
import com.lectures.features.timetable.ui.DarkSurfaceBase
import com.lectures.features.timetable.ui.DarkSurfaceCard
import com.lectures.features.timetable.ui.TextMutedLight
import com.lectures.features.timetable.ui.TextPrimaryLight
import com.lectures.ui.components.DedicatedCalendarDatePickerDialog
import com.lectures.ui.components.DedicatedTimePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    isDark: Boolean,
    initialTask: Assignment? = null,
    availableCourses: List<Pair<String, String>> = emptyList(),
    defaultReminderMinutes: Int = 120,
    onDismiss: () -> Unit,
    onConfirm: (title: String, course: String, dueDate: String, priority: String, reminder: Int?, notes: String, category: TaskCategory) -> Unit
) {
    val initialCatAndTitle = remember(initialTask) {
        if (initialTask != null) resolveTaskCategoryAndTitle(initialTask.title)
        else TaskCategory.ASSIGNMENT to ""
    }

    var title by remember(initialTask) { mutableStateOf(initialCatAndTitle.second) }
    var category by remember(initialTask) { mutableStateOf(initialCatAndTitle.first) }
    var course by remember(initialTask) { mutableStateOf(initialTask?.courseCode ?: "") }
    var dueTime by remember(initialTask) {
        val extractedTime = TaskDateTimeParser.extractTime(initialTask?.dueDate)
        mutableStateOf(extractedTime ?: "17:00")
    }
    var dueDateOnly by remember(initialTask) {
        val raw = initialTask?.dueDate
        val extractedTime = TaskDateTimeParser.extractTime(raw)
        val cleanDate = if (extractedTime != null && raw != null) {
            raw.replace(extractedTime, "").trim()
        } else {
            raw ?: ""
        }
        val fallbackTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val defaultFormattedDate = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(fallbackTomorrow.time)
        mutableStateOf(if (cleanDate.isBlank() || cleanDate == "Soon") defaultFormattedDate else cleanDate)
    }
    val fullDueDateTime = remember(dueDateOnly, dueTime) {
        if (dueTime.isNotBlank()) "$dueDateOnly $dueTime".trim() else dueDateOnly
    }
    val taskSaveKey = initialTask?.id ?: -1L
    var priority by rememberSaveable(taskSaveKey) { mutableStateOf(initialTask?.priority ?: "Medium") }
    var reminderMinutes by rememberSaveable(taskSaveKey) { mutableStateOf<Int?>(initialTask?.reminderMinutes ?: defaultReminderMinutes) }
    var notes by rememberSaveable(taskSaveKey) { mutableStateOf(initialTask?.notes ?: "") }

    var courseDropdownExpanded by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var showDueCalendarPicker by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var showDueTimePicker by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var titleTouched by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var courseTouched by rememberSaveable(taskSaveKey) { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    val schedulePreview = remember(fullDueDateTime, reminderMinutes) {
        TaskDateTimeParser.formatSchedulePreview(fullDueDateTime, reminderMinutes)
    }

    val handleDismiss = {
        courseDropdownExpanded = false
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        modifier = Modifier.imePadding(),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (initialTask != null) Icons.Outlined.Edit else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (initialTask != null) "Edit & Reschedule Task" else "Add Academic Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleTouched = true
                    },
                    label = { Text("Task title *") },
                    placeholder = { Text("e.g. Lab report submission") },
                    isError = titleTouched && title.isBlank(),
                    supportingText = if (titleTouched && title.isBlank()) {
                        { Text("Title is required", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(TaskCategory.entries) { cat ->
                        val isSelected = category == cat
                        Surface(
                            onClick = { category = cat },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) cat.color else (if (isDark) DarkSurfaceBase else cat.lightContainerColor.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, if (isSelected) cat.color else cat.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    cat.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else cat.color,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    cat.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF1E293B))
                                )
                            }
                        }
                    }
                }

                if (availableCourses.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = courseDropdownExpanded,
                        onExpandedChange = { courseDropdownExpanded = !courseDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val matchingCourse = availableCourses.firstOrNull { it.first.equals(course, ignoreCase = true) }
                        val displayCourse = if (course.isNotBlank()) {
                            if (matchingCourse != null && matchingCourse.second.isNotBlank()) {
                                "${matchingCourse.first} · ${matchingCourse.second}"
                            } else {
                                course
                            }
                        } else ""

                        OutlinedTextField(
                            value = displayCourse,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Course unit *") },
                            placeholder = { Text("Select course from program") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseDropdownExpanded)
                            },
                            colors = OutlinedTextFieldDefaults.colors(),
                            isError = courseTouched && course.isBlank(),
                            supportingText = if (courseTouched && course.isBlank()) {
                                { Text("Course unit is required", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                            } else null,
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = courseDropdownExpanded,
                            onDismissRequest = { courseDropdownExpanded = false }
                        ) {
                            availableCourses.forEach { (cCode, cTitle) ->
                                val isSelected = course.equals(cCode, ignoreCase = true)
                                DropdownMenuItem(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) (if (isDark) primaryColor.copy(alpha = 0.22f) else primaryColor.copy(alpha = 0.12f))
                                            else Color.Transparent
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) (if (isDark) primaryColor.copy(alpha = 0.5f) else primaryColor.copy(alpha = 0.35f)) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    text = {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = cCode,
                                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                color = if (isSelected) primaryColor else (if (isDark) Color.White else Color(0xFF0F172A)),
                                                fontSize = 13.sp
                                            )
                                            if (cTitle.isNotBlank()) {
                                                Text(
                                                    text = cTitle,
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) primaryColor.copy(alpha = 0.85f) else textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        course = cCode
                                        courseTouched = false
                                        courseDropdownExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = course,
                        onValueChange = {
                            course = it
                            courseTouched = true
                        },
                        label = { Text("Course code *") },
                        placeholder = { Text("e.g. BCS2101") },
                        isError = courseTouched && course.isBlank(),
                        supportingText = if (courseTouched && course.isBlank()) {
                            { Text("Course code is required", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                        } else null,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Text(
                    text = "Deadline Schedule *",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceCard else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1.3f)
                            .clickable { showDueCalendarPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Pick Due Date",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = dueDateOnly.ifBlank { "Select Date" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Due date",
                                    fontSize = 10.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) DarkSurfaceCard else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showDueTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Pick Due Time",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = dueTime.ifBlank { "17:00" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF0F172A),
                                    maxLines = 1
                                )
                                Text(
                                    text = "Due time",
                                    fontSize = 10.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Priority:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                    listOf("High", "Medium", "Low").forEach { p ->
                        val isSelected = priority == p
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) primaryColor else (if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9))
                                )
                                .clickable { priority = p }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                p,
                                color = if (isSelected) Color.White else textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Alert: ", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                    val reminderOptions = listOf(
                        0 to "On due time",
                        60 to "1h before",
                        120 to "2h before",
                        360 to "6h before",
                        720 to "12h before",
                        1440 to "1d before",
                        2880 to "2d before",
                        null to "None"
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(reminderOptions) { (mins, label) ->
                            val isSelected = reminderMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) primaryColor else (if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9))
                                    )
                                    .clickable { reminderMinutes = mins }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (isSelected) Color.White else textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Instructions (Optional)") },
                    placeholder = { Text("e.g. MueLE submission, rubric...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF064E3B).copy(alpha = 0.4f) else Color(0xFFF0FDF4),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            if (isDark) Color(0xFF065F46) else Color(0xFFBBF7D0),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        schedulePreview,
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF166534),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    courseDropdownExpanded = false
                    titleTouched = true
                    courseTouched = true
                    if (title.isNotBlank() && course.isNotBlank()) {
                        onConfirm(
                            title.trim(),
                            course.trim(),
                            fullDueDateTime.ifBlank { "Soon" },
                            priority,
                            reminderMinutes,
                            notes.trim(),
                            category
                        )
                    }
                },
                enabled = title.isNotBlank() && course.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                Text(if (initialTask != null) "Save & Reschedule" else "Schedule & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = handleDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showDueCalendarPicker) {
        DedicatedCalendarDatePickerDialog(
            initialDate = dueDateOnly,
            title = "Select Task Due Date",
            onDateSelected = { picked ->
                dueDateOnly = picked
                showDueCalendarPicker = false
            },
            onDismiss = { showDueCalendarPicker = false }
        )
    }

    if (showDueTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = dueTime.ifBlank { "17:00" },
            title = "Select Due Time",
            onTimeSelected = { picked ->
                dueTime = picked
                showDueTimePicker = false
            },
            onDismiss = { showDueTimePicker = false }
        )
    }
}
