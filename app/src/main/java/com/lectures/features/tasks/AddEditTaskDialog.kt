package com.lectures.features.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.window.DialogProperties
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.DedicatedCalendarDatePickerDialog
import com.lectures.ui.components.DedicatedTimePickerDialog
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    isDark: Boolean,
    initialTask: Assignment? = null,
    availableCourses: List<Pair<String, String>> = emptyList(),
    defaultReminderMinutes: Int = 120,
    onDismiss: () -> Unit,
    onConfirm: (title: String, course: String, dueDate: String, priority: String, reminder: Int?, notes: String, category: TaskCategory) -> Unit
) {
    val taskSaveKey = initialTask?.id ?: -1L
    val initialCatAndTitle = remember(initialTask) {
        if (initialTask != null) resolveTaskCategoryAndTitle(initialTask.title)
        else TaskCategory.ASSIGNMENT to ""
    }

    var title by rememberSaveable(taskSaveKey) { mutableStateOf(initialCatAndTitle.second) }
    var categoryName by rememberSaveable(taskSaveKey) { mutableStateOf(initialCatAndTitle.first.name) }
    val category = TaskCategory.entries.firstOrNull { it.name == categoryName } ?: TaskCategory.ASSIGNMENT
    var course by rememberSaveable(taskSaveKey) { mutableStateOf(initialTask?.courseCode ?: "") }
    var dueTime by rememberSaveable(taskSaveKey) {
        val extractedTime = TaskDateTimeParser.extractTime(initialTask?.dueDate)
        mutableStateOf(extractedTime ?: "17:00")
    }
    var dueDateOnly by rememberSaveable(taskSaveKey) {
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
    var priority by rememberSaveable(taskSaveKey) { mutableStateOf(initialTask?.priority ?: "Medium") }
    var reminderMinutes by rememberSaveable(taskSaveKey) { mutableStateOf<Int?>(initialTask?.reminderMinutes ?: defaultReminderMinutes) }
    var notes by rememberSaveable(taskSaveKey) { mutableStateOf(initialTask?.notes ?: "") }

    var courseDropdownExpanded by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var showDueCalendarPicker by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var showDueTimePicker by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var titleTouched by rememberSaveable(taskSaveKey) { mutableStateOf(false) }
    var courseTouched by rememberSaveable(taskSaveKey) { mutableStateOf(false) }

    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val primaryColor = MaterialTheme.colorScheme.primary
    val textPrimary = ext.textPrimary
    val textSecondary = ext.textSecondary

    val schedulePreview = remember(fullDueDateTime, reminderMinutes) {
        TaskDateTimeParser.formatSchedulePreview(fullDueDateTime, reminderMinutes)
    }

    val windowInfo = rememberLecturesWindow()
    val handleDismiss = {
        courseDropdownExpanded = false
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = handleDismiss,
        containerColor = ext.surfaceCard,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .widthIn(min = 440.dp, max = 660.dp)
                        .fillMaxWidth()
                } else {
                    Modifier
                }
            )
            .heightIn(max = (windowInfo.heightDp - 48).coerceAtLeast(360).dp)
            .imePadding()
            .onEscapeKey(handleDismiss),
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

                val categoryChipContent: @Composable (TaskCategory) -> Unit = { cat ->
                    val isSelected = category == cat
                    val chipBg by animateColorAsState(
                        targetValue = if (isSelected) cat.color else (if (isDark) ext.surfaceBase else cat.lightContainerColor.copy(alpha = 0.5f)),
                        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                        label = "task_cat_bg"
                    )
                    val chipBorder by animateColorAsState(
                        targetValue = if (isSelected) cat.color else cat.color.copy(alpha = 0.35f),
                        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                        label = "task_cat_border"
                    )
                    Surface(
                        onClick = { categoryName = cat.name },
                        shape = RoundedCornerShape(10.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipBorder),
                        modifier = Modifier.desktopHandCursor()
                    ) {
                        val selectedInk = contrastOnColor(cat.color)
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                cat.icon,
                                contentDescription = null,
                                tint = if (isSelected) selectedInk else cat.color,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                cat.displayName,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) selectedInk else ext.textPrimary
                            )
                        }
                    }
                }

                if (windowInfo.isTablet) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TaskCategory.entries.forEach { cat -> categoryChipContent(cat) }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(TaskCategory.entries) { cat -> categoryChipContent(cat) }
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
                                                color = if (isSelected) primaryColor else ext.textPrimary,
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
                        color = ext.surfaceBase,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .weight(1.3f)
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor()
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
                                    color = ext.textPrimary,
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
                        color = ext.surfaceBase,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor()
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
                                    color = ext.textPrimary,
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
                    val selectedInk = contrastOnColor(primaryColor)
                    listOf("High", "Medium", "Low").forEach { p ->
                        val isSelected = priority == p
                        val chipBg by animateColorAsState(
                            targetValue = if (isSelected) primaryColor else ext.surfaceBase,
                            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                            label = "task_prio_bg"
                        )
                        val chipFg by animateColorAsState(
                            targetValue = if (isSelected) selectedInk else textSecondary,
                            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                            label = "task_prio_fg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(chipBg)
                                .desktopHandCursor()
                                .clickable { priority = p }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                p,
                                color = chipFg,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Alert:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
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
                    val selectedInk = contrastOnColor(primaryColor)
                    val alertChipContent: @Composable (Pair<Int?, String>) -> Unit = { (mins, label) ->
                        val isSelected = reminderMinutes == mins
                        val alertBg by animateColorAsState(
                            targetValue = if (isSelected) primaryColor else ext.surfaceBase,
                            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                            label = "task_alert_bg"
                        )
                        val alertFg by animateColorAsState(
                            targetValue = if (isSelected) selectedInk else textSecondary,
                            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                            label = "task_alert_fg"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(alertBg)
                                .desktopHandCursor()
                                .clickable { reminderMinutes = mins }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                label,
                                color = alertFg,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    if (windowInfo.isTablet) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            reminderOptions.forEach { opt -> alertChipContent(opt) }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(reminderOptions) { opt -> alertChipContent(opt) }
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
                        .background(ext.successBg, RoundedCornerShape(8.dp))
                        .border(1.dp, ext.successBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = ext.successIcon,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        schedulePreview,
                        fontSize = 11.sp,
                        color = ext.successText,
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = contrastOnColor(primaryColor)
                ),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor(enabled = title.isNotBlank() && course.isNotBlank())
            ) {
                Text(if (initialTask != null) "Save & Reschedule" else "Schedule & Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = handleDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
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
