package com.lectures.features.timetable.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.domain.ActivityCategory
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.AdaptiveSheetOrDialog
import com.lectures.ui.components.DedicatedCalendarDatePickerDialog
import com.lectures.ui.components.DedicatedCoursePickerDialog
import com.lectures.ui.components.DedicatedTimePickerDialog
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onCtrlEnter
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberPrefersReducedMotion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class QuickAddMode(val title: String, val subtitle: String) {
    ACTIVITY("Timetable Activity", "Scheduled session on timetable"),
    TASK("Academic Task", "To-do, assignment & deadline")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddBottomSheet(
    initialDayOfWeek: String = TimeUtil.todayName(),
    initialMode: QuickAddMode = QuickAddMode.ACTIVITY,
    availableCourses: List<String> = emptyList(),
    courseDetails: List<Pair<String, String>> = emptyList(),
    defaultTaskReminderMinutes: Int = 120,
    onDismiss: () -> Unit,
    onSaveActivity: (CustomEvent) -> Unit,
    onSaveTask: (
        title: String,
        courseCode: String,
        dueDate: String,
        priority: String,
        reminderMinutes: Int?,
        notes: String
    ) -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val reducedMotion = rememberPrefersReducedMotion()

    var currentMode by remember { mutableStateOf(initialMode) }

    // Activity state
    var activityTitle by remember { mutableStateOf("") }
    var selectedActivityCategory by remember { mutableStateOf(ActivityCategory.STUDY) }
    var activityDay by remember {
        mutableStateOf(
            if (initialDayOfWeek.isNotBlank()) initialDayOfWeek else TimeUtil.todayName()
        )
    }
    var activityStartTime by remember { mutableStateOf("14:00") }
    var activityEndTime by remember { mutableStateOf("15:30") }
    var activityLocation by remember { mutableStateOf("") }
    var activityAlarmMinutes by remember { mutableStateOf<Int?>(30) }
    var isActivityCustomAlarm by remember { mutableStateOf(false) }
    var activityCustomMinutesText by remember { mutableStateOf("") }
    var activityNotes by remember { mutableStateOf("") }
    var activityHasError by remember { mutableStateOf(false) }

    // Task state
    var taskTitle by remember { mutableStateOf("") }
    var selectedTaskCategory by remember { mutableStateOf(TaskCategory.ASSIGNMENT) }
    var taskCourseCode by remember {
        mutableStateOf(availableCourses.firstOrNull() ?: "")
    }
    var taskDueDateOnly by remember {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        mutableStateOf(SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(cal.time))
    }
    var taskDueTime by remember { mutableStateOf("17:00") }
    val fullTaskDueDateTime = remember(taskDueDateOnly, taskDueTime) {
        if (taskDueTime.isNotBlank()) "$taskDueDateOnly $taskDueTime".trim() else taskDueDateOnly
    }
    var taskPriority by remember { mutableStateOf("Medium") }
    var taskReminderMinutes by remember(defaultTaskReminderMinutes) { mutableStateOf<Int?>(defaultTaskReminderMinutes) }
    var isTaskCustomReminder by remember { mutableStateOf(false) }
    var taskCustomMinutesText by remember { mutableStateOf("") }
    var taskNotes by remember { mutableStateOf("") }
    var taskHasError by remember { mutableStateOf(false) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showCoursePicker by remember { mutableStateOf(false) }
    var showDueCalendarPicker by remember { mutableStateOf(false) }
    var showDueTimePicker by remember { mutableStateOf(false) }

    val submitCurrentMode: () -> Unit = {
        when (currentMode) {
            QuickAddMode.ACTIVITY -> {
                if (activityTitle.isBlank()) {
                    activityHasError = true
                } else {
                    val newEvent = CustomEvent(
                        title = activityTitle.trim(),
                        dayOfWeek = activityDay,
                        startTime = activityStartTime.trim(),
                        endTime = activityEndTime.trim(),
                        location = activityLocation.trim(),
                        notes = activityNotes.trim(),
                        category = selectedActivityCategory.name,
                        colorTag = selectedActivityCategory.colorHex,
                        alarmMinutes = activityAlarmMinutes
                    )
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSaveActivity(newEvent)
                    onDismiss()
                }
            }
            QuickAddMode.TASK -> {
                if (taskTitle.isBlank()) {
                    taskHasError = true
                } else {
                    val effectiveCourse = taskCourseCode.ifBlank { "GEN100" }
                    val fullTitle = if (selectedTaskCategory != TaskCategory.GENERAL && !taskTitle.startsWith(selectedTaskCategory.displayName)) {
                        "[${selectedTaskCategory.displayName}] ${taskTitle.trim()}"
                    } else {
                        taskTitle.trim()
                    }
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    onSaveTask(
                        fullTitle,
                        effectiveCourse.trim(),
                        fullTaskDueDateTime.trim(),
                        taskPriority,
                        taskReminderMinutes,
                        taskNotes.trim()
                    )
                    onDismiss()
                }
            }
        }
    }

    AdaptiveSheetOrDialog(
        onDismissRequest = onDismiss,
        sheetTestTag = "quick_add_bottom_sheet",
        containerColor = MaterialTheme.colorScheme.surface,
        desktopMaxWidth = 640.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .onCtrlEnter(onSubmit = submitCurrentMode)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Quick Add Entry",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add an activity or task directly into your schedule",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            onDismiss()
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                            .testTag("close_quick_add_sheet")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Mode Distinction Selector Tabs
                QuickAddModeSelectorTabs(
                    currentMode = currentMode,
                    onModeSelected = { currentMode = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedContent(
                    targetState = currentMode,
                    transitionSpec = {
                        MotionSpec.directionalSlideTransform(
                            forward = targetState == QuickAddMode.TASK,
                            reducedMotion = reducedMotion
                        )
                    },
                    label = "quick_add_form_switch"
                ) { mode ->
                    when (mode) {
                        QuickAddMode.ACTIVITY -> {
                            QuickAddActivityForm(
                                activityTitle = activityTitle,
                                onActivityTitleChange = {
                                    activityTitle = it
                                    if (it.isNotBlank()) activityHasError = false
                                },
                                onSuggestionClick = { sugg ->
                                    activityTitle = sugg
                                    activityHasError = false
                                },
                                activityHasError = activityHasError,
                                selectedCategory = selectedActivityCategory,
                                onCategoryChange = { selectedActivityCategory = it },
                                activityDay = activityDay,
                                onDayChange = { activityDay = it },
                                activityStartTime = activityStartTime,
                                onOpenStartTimePicker = { showStartTimePicker = true },
                                activityEndTime = activityEndTime,
                                onEndTimeChange = { activityEndTime = it },
                                onOpenEndTimePicker = { showEndTimePicker = true },
                                activityLocation = activityLocation,
                                onLocationChange = { activityLocation = it },
                                activityAlarmMinutes = activityAlarmMinutes,
                                isCustomAlarm = isActivityCustomAlarm,
                                customMinutesText = activityCustomMinutesText,
                                onPresetAlarmSelected = { minutes ->
                                    isActivityCustomAlarm = false
                                    activityAlarmMinutes = minutes
                                },
                                onToggleCustomAlarm = { isActivityCustomAlarm = !isActivityCustomAlarm },
                                onCustomMinutesChange = { filtered ->
                                    activityCustomMinutesText = filtered
                                    activityAlarmMinutes = filtered.toIntOrNull()
                                },
                                activityNotes = activityNotes,
                                onNotesChange = { activityNotes = it }
                            )
                        }
                        QuickAddMode.TASK -> {
                            QuickAddTaskForm(
                                taskTitle = taskTitle,
                                onTaskTitleChange = {
                                    taskTitle = it
                                    if (it.isNotBlank()) taskHasError = false
                                },
                                taskHasError = taskHasError,
                                selectedCategory = selectedTaskCategory,
                                onCategoryChange = { selectedTaskCategory = it },
                                taskPriority = taskPriority,
                                onPriorityChange = { taskPriority = it },
                                taskCourseCode = taskCourseCode,
                                onCourseCodeChange = { taskCourseCode = it },
                                availableCourses = availableCourses,
                                onOpenCoursePicker = { showCoursePicker = true },
                                taskDueDateOnly = taskDueDateOnly,
                                taskDueTime = taskDueTime,
                                fullTaskDueDateTime = fullTaskDueDateTime,
                                onOpenDueDatePicker = { showDueCalendarPicker = true },
                                onOpenDueTimePicker = { showDueTimePicker = true },
                                taskReminderMinutes = taskReminderMinutes,
                                isCustomReminder = isTaskCustomReminder,
                                customMinutesText = taskCustomMinutesText,
                                onPresetReminderSelected = { minutes ->
                                    isTaskCustomReminder = false
                                    taskReminderMinutes = minutes
                                },
                                onToggleCustomReminder = { isTaskCustomReminder = !isTaskCustomReminder },
                                onCustomMinutesChange = { filtered ->
                                    taskCustomMinutesText = filtered
                                    taskReminderMinutes = filtered.toIntOrNull()
                                },
                                taskNotes = taskNotes,
                                onNotesChange = { taskNotes = it }
                            )
                        }
                    }
                }
            }

            // Pinned / Sticky Bottom Action Bar - ALWAYS VISIBLE, ZERO SCROLLING REQUIRED!
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    AnimatedContent(
                        targetState = currentMode,
                        transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                        label = "quick_add_cta_switch"
                    ) { mode ->
                        when (mode) {
                            QuickAddMode.ACTIVITY -> {
                                Button(
                                    onClick = submitCurrentMode,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .desktopHandCursor()
                                        .testTag("save_activity_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = selectedActivityCategory.color,
                                        contentColor = contrastOnColor(selectedActivityCategory.color)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.EventNote, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add Activity",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            QuickAddMode.TASK -> {
                                Button(
                                    onClick = submitCurrentMode,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .desktopHandCursor()
                                        .testTag("save_task_button"),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = contrastOnColor(MaterialTheme.colorScheme.primary)
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Outlined.Assignment, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Add Task",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = activityStartTime,
            title = "Set Activity Start Time",
            onTimeSelected = { activityStartTime = it },
            onDismiss = { showStartTimePicker = false }
        )
    }

    if (showEndTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = activityEndTime,
            title = "Set Activity End Time",
            onTimeSelected = { activityEndTime = it },
            onDismiss = { showEndTimePicker = false }
        )
    }

    if (showCoursePicker) {
        val resolvedCourseList = remember(availableCourses, courseDetails) {
            if (courseDetails.isNotEmpty()) courseDetails
            else availableCourses.map { it to "" }
        }
        DedicatedCoursePickerDialog(
            selectedCourse = taskCourseCode,
            availableCourses = resolvedCourseList,
            onCourseSelected = { code, _ -> taskCourseCode = code },
            onDismiss = { showCoursePicker = false }
        )
    }

    if (showDueCalendarPicker) {
        DedicatedCalendarDatePickerDialog(
            initialDate = taskDueDateOnly,
            title = "Select Task Due Date",
            onDateSelected = { picked ->
                taskDueDateOnly = picked
                showDueCalendarPicker = false
            },
            onDismiss = { showDueCalendarPicker = false }
        )
    }

    if (showDueTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = taskDueTime.ifBlank { "17:00" },
            title = "Select Task Due Time",
            onTimeSelected = { picked ->
                taskDueTime = picked
                showDueTimePicker = false
            },
            onDismiss = { showDueTimePicker = false }
        )
    }
}

@Composable
private fun QuickAddModeSelectorTabs(
    currentMode: QuickAddMode,
    onModeSelected: (QuickAddMode) -> Unit
) {
    val reducedMotion = rememberPrefersReducedMotion()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val isActivity = currentMode == QuickAddMode.ACTIVITY
        val activityBg by animateColorAsState(
            targetValue = if (isActivity) MaterialTheme.colorScheme.surface else Color.Transparent,
            animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
            label = "qa_tab_activity_bg"
        )
        val activityTint by animateColorAsState(
            targetValue = if (isActivity) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
            label = "qa_tab_activity_tint"
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .desktopHandCursor()
                .background(activityBg)
                .clickable { onModeSelected(QuickAddMode.ACTIVITY) }
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .testTag("quick_add_tab_activity"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                    contentDescription = null,
                    tint = activityTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "User Activity",
                        fontWeight = if (isActivity) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isActivity) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Timetable Block",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val isTask = currentMode == QuickAddMode.TASK
        val taskBg by animateColorAsState(
            targetValue = if (isTask) MaterialTheme.colorScheme.surface else Color.Transparent,
            animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
            label = "qa_tab_task_bg"
        )
        val taskTint by animateColorAsState(
            targetValue = if (isTask) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
            label = "qa_tab_task_tint"
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(12.dp))
                .desktopHandCursor()
                .background(taskBg)
                .clickable { onModeSelected(QuickAddMode.TASK) }
                .padding(vertical = 10.dp, horizontal = 12.dp)
                .testTag("quick_add_tab_task"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Assignment,
                    contentDescription = null,
                    tint = taskTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "Academic Task",
                        fontWeight = if (isTask) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isTask) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Deadline & Alarm",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
