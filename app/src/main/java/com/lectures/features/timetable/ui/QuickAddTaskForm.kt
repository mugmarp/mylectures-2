package com.lectures.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.features.timetable.domain.TaskCategory

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickAddTaskForm(
    taskTitle: String,
    onTaskTitleChange: (String) -> Unit,
    taskHasError: Boolean,
    selectedCategory: TaskCategory,
    onCategoryChange: (TaskCategory) -> Unit,
    taskPriority: String,
    onPriorityChange: (String) -> Unit,
    taskCourseCode: String,
    onCourseCodeChange: (String) -> Unit,
    availableCourses: List<String>,
    onOpenCoursePicker: () -> Unit,
    taskDueDateOnly: String,
    taskDueTime: String,
    fullTaskDueDateTime: String,
    onOpenDueDatePicker: () -> Unit,
    onOpenDueTimePicker: () -> Unit,
    taskReminderMinutes: Int?,
    isCustomReminder: Boolean,
    customMinutesText: String,
    onPresetReminderSelected: (Int?) -> Unit,
    onToggleCustomReminder: () -> Unit,
    onCustomMinutesChange: (String) -> Unit,
    taskNotes: String,
    onNotesChange: (String) -> Unit
) {
    // Task Category Header
    Text(
        text = "Task Category",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.testTag("task_category_row")
    ) {
        items(TaskCategory.entries) { cat ->
            val isSelected = cat == selectedCategory
            Card(
                onClick = { onCategoryChange(cat) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) cat.color.copy(alpha = 0.15f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = cat.displayName,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) cat.color else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Priority level
    Text(
        text = "Priority Level",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            Triple("High", Color(0xFFE11D48), Color(0xFFFFE4E6)),
            Triple("Medium", Color(0xFFD97706), Color(0xFFFEF3C7)),
            Triple("Low", Color(0xFF2563EB), Color(0xFFEFF6FF))
        ).forEach { (prio, textColor, bgColor) ->
            val isSelected = taskPriority.equals(prio, ignoreCase = true)
            Card(
                onClick = { onPriorityChange(prio) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) bgColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (isSelected) BorderStroke(1.5.dp, textColor) else null,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = prio,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) textColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Task Title
    OutlinedTextField(
        value = taskTitle,
        onValueChange = onTaskTitleChange,
        label = { Text("Task Title *") },
        placeholder = { Text("e.g. Lab Report 3, Assignment 2, Paper Draft") },
        isError = taskHasError,
        supportingText = if (taskHasError) {
            { Text("Title is required", color = MaterialTheme.colorScheme.error) }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_title_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Course Code
    OutlinedTextField(
        value = taskCourseCode,
        onValueChange = onCourseCodeChange,
        label = { Text("Course Code *") },
        placeholder = { Text("e.g. Course Code") },
        leadingIcon = {
            Icon(Icons.Outlined.School, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            IconButton(onClick = onOpenCoursePicker) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Pick Course", modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_course_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )

    val courseSuggestions = availableCourses.distinct().take(5)
    if (courseSuggestions.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            item {
                AssistChip(
                    onClick = onOpenCoursePicker,
                    label = { Text("Select Course ▼", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
            items(courseSuggestions) { code ->
                SuggestionChip(
                    onClick = { onCourseCodeChange(code) },
                    label = { Text(code, fontSize = 11.sp) }
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Due Date Card
    Text(
        text = "Due Date *",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .weight(1.3f)
                .clickable(onClick = onOpenDueDatePicker)
                .testTag("task_due_date_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Pick Due Date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = taskDueDateOnly.ifBlank { "Select Date" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Due date",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onOpenDueTimePicker)
                .testTag("task_due_time_button")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Pick Due Time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = taskDueTime.ifBlank { "17:00" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        text = "Due time",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    val schedulePreview = remember(fullTaskDueDateTime, taskReminderMinutes) {
        TaskDateTimeParser.formatSchedulePreview(fullTaskDueDateTime, taskReminderMinutes)
    }
    if (schedulePreview.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = schedulePreview,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Alarm Reminder
    Text(
        text = "Alarm Reminder",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "None" to null,
            "30m" to 30,
            "1h" to 60,
            "2h" to 120,
            "6h" to 360,
            "12h" to 720,
            "1d" to 1440
        ).forEach { (label, minutes) ->
            val isSelected = !isCustomReminder && taskReminderMinutes == minutes
            FilterChip(
                selected = isSelected,
                onClick = { onPresetReminderSelected(minutes) },
                label = { Text(label, fontSize = 12.sp) },
                leadingIcon = if (isSelected && minutes != null) {
                    { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                } else null,
                shape = RoundedCornerShape(10.dp)
            )
        }

        FilterChip(
            selected = isCustomReminder,
            onClick = onToggleCustomReminder,
            label = { Text("Custom", fontSize = 12.sp) },
            shape = RoundedCornerShape(10.dp)
        )
    }

    if (isCustomReminder) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = customMinutesText,
            onValueChange = { input ->
                onCustomMinutesChange(input.filter { it.isDigit() }.take(4))
            },
            label = { Text("Custom lead minutes before deadline") },
            placeholder = { Text("e.g. 45, 90, 120, 2880") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Notes
    OutlinedTextField(
        value = taskNotes,
        onValueChange = onNotesChange,
        label = { Text("Task Notes (Optional)") },
        placeholder = { Text("Submission details, portal links, guidelines...") },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_notes_input"),
        shape = RoundedCornerShape(12.dp),
        maxLines = 3
    )

    Spacer(modifier = Modifier.height(16.dp))
}
