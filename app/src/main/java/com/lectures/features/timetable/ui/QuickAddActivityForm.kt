package com.lectures.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.domain.ActivityCategory
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.rememberLecturesWindow
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickAddActivityForm(
    activityTitle: String,
    onActivityTitleChange: (String) -> Unit,
    onSuggestionClick: (String) -> Unit,
    activityHasError: Boolean,
    selectedCategory: ActivityCategory,
    onCategoryChange: (ActivityCategory) -> Unit,
    activityDay: String,
    onDayChange: (String) -> Unit,
    activityStartTime: String,
    onOpenStartTimePicker: () -> Unit,
    activityEndTime: String,
    onEndTimeChange: (String) -> Unit,
    onOpenEndTimePicker: () -> Unit,
    activityLocation: String,
    onLocationChange: (String) -> Unit,
    activityAlarmMinutes: Int?,
    isCustomAlarm: Boolean,
    customMinutesText: String,
    onPresetAlarmSelected: (Int?) -> Unit,
    onToggleCustomAlarm: () -> Unit,
    onCustomMinutesChange: (String) -> Unit,
    activityNotes: String,
    onNotesChange: (String) -> Unit
) {
    val daysOfWeek = TimeUtil.DAYS
    val windowInfo = rememberLecturesWindow()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Category Selection Header
    Text(
        text = "Activity Category",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    val categoryCardContent: @Composable (ActivityCategory) -> Unit = { cat ->
        val isSelected = cat == selectedCategory
        val categoryIcon = when (cat) {
            ActivityCategory.STUDY -> Icons.AutoMirrored.Outlined.MenuBook
            ActivityCategory.LAB -> Icons.Outlined.Science
            ActivityCategory.SPORTS -> Icons.Outlined.FitnessCenter
            ActivityCategory.CLUB -> Icons.Outlined.Groups
            ActivityCategory.PERSONAL -> Icons.Outlined.Person
            ActivityCategory.EXAM -> Icons.Outlined.Timer
        }
        Card(
            onClick = { onCategoryChange(cat) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) cat.color.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            border = if (isSelected) BorderStroke(1.5.dp, cat.color) else null,
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .desktopHandCursor()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = categoryIcon,
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

    if (windowInfo.isTablet) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("activity_category_row")
        ) {
            ActivityCategory.entries.forEach { cat -> categoryCardContent(cat) }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("activity_category_row")
        ) {
            items(ActivityCategory.entries) { cat -> categoryCardContent(cat) }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Title input
    OutlinedTextField(
        value = activityTitle,
        onValueChange = onActivityTitleChange,
        label = { Text("Activity Title *") },
        placeholder = { Text("e.g. Group Revision, Gym, Physics Lab") },
        isError = activityHasError,
        supportingText = if (activityHasError) {
            { Text("Title is required", color = MaterialTheme.colorScheme.error) }
        } else null,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_title_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    // Title quick suggestions
    val suggestions = listOf("Group Study", "Physics Lab", "Library Work", "Discussion", "Exam Revision")
    if (windowInfo.isTablet) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            suggestions.forEach { sugg ->
                SuggestionChip(
                    onClick = { onSuggestionClick(sugg) },
                    label = { Text(sugg, fontSize = 11.sp) },
                    modifier = Modifier.desktopHandCursor()
                )
            }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            items(suggestions) { sugg ->
                SuggestionChip(
                    onClick = { onSuggestionClick(sugg) },
                    label = { Text(sugg, fontSize = 11.sp) },
                    modifier = Modifier.desktopHandCursor()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Day of Week
    Text(
        text = "Day of Week",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    if (windowInfo.isTablet) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("activity_day_row")
        ) {
            daysOfWeek.forEach { day ->
                val isSelected = day.equals(activityDay, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onDayChange(day) },
                    label = { Text(day.take(3), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }
        }
    } else {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(end = 16.dp),
            modifier = Modifier.testTag("activity_day_row")
        ) {
            items(daysOfWeek) { day ->
                val isSelected = day.equals(activityDay, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = { onDayChange(day) },
                    label = { Text(day.take(3), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Time Window: Start & End Time (Tap to open circular time picker)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onOpenStartTimePicker,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .desktopHandCursor()
                .testTag("activity_start_time_input")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Start Time",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activityStartTime.ifBlank { "14:00" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Surface(
            onClick = onOpenEndTimePicker,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .desktopHandCursor()
                .testTag("activity_end_time_input")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "End Time",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = activityEndTime.ifBlank { "15:30" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    // Duration presets
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf("+1 hr" to 60, "+1.5 hrs" to 90, "+2 hrs" to 120).forEach { (label, durationMinutes) ->
            AssistChip(
                onClick = {
                    val startMin = TimeUtil.toMinutes(activityStartTime)
                    val endMin = (startMin + durationMinutes).coerceAtMost(23 * 60 + 59)
                    val h = endMin / 60
                    val m = endMin % 60
                    onEndTimeChange(String.format(Locale.US, "%02d:%02d", h, m))
                },
                label = { Text(label, fontSize = 11.sp) },
                modifier = Modifier.desktopHandCursor()
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Location
    OutlinedTextField(
        value = activityLocation,
        onValueChange = onLocationChange,
        label = { Text("Location (Optional)") },
        placeholder = { Text("e.g. Main Library, Room 204, Sports Field") },
        leadingIcon = {
            Icon(Icons.Outlined.MeetingRoom, contentDescription = "Venue", modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
            if (activityLocation.isNotBlank()) {
                RoomFloorBadge(roomName = activityLocation, compact = true)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_location_input"),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Reminder Alarm
    Text(
        text = "Alarm Notification",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(8.dp))

    val alarmOptions = listOf(
        "None" to null,
        "10m" to 10,
        "15m" to 15,
        "30m" to 30,
        "1h" to 60
    )
    if (windowInfo.isTablet) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            alarmOptions.forEach { (label, minutes) ->
                val isSelected = !isCustomAlarm && activityAlarmMinutes == minutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetAlarmSelected(minutes) },
                    label = { Text(label, fontSize = 12.sp) },
                    leadingIcon = if (isSelected && minutes != null) {
                        { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }
            FilterChip(
                selected = isCustomAlarm,
                onClick = onToggleCustomAlarm,
                label = { Text("Custom", fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.desktopHandCursor()
            )
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 16.dp)
        ) {
            items(alarmOptions) { (label, minutes) ->
                val isSelected = !isCustomAlarm && activityAlarmMinutes == minutes
                FilterChip(
                    selected = isSelected,
                    onClick = { onPresetAlarmSelected(minutes) },
                    label = { Text(label, fontSize = 12.sp) },
                    leadingIcon = if (isSelected && minutes != null) {
                        { Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }

            item {
                FilterChip(
                    selected = isCustomAlarm,
                    onClick = onToggleCustomAlarm,
                    label = { Text("Custom", fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }
        }
    }

    if (isCustomAlarm) {
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = customMinutesText,
            onValueChange = { input ->
                onCustomMinutesChange(input.filter { it.isDigit() }.take(3))
            },
            label = { Text("Custom lead minutes") },
            placeholder = { Text("e.g. 20, 45, 90") },
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
            shape = RoundedCornerShape(10.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Notes
    OutlinedTextField(
        value = activityNotes,
        onValueChange = onNotesChange,
        label = { Text("Notes (Optional)") },
        placeholder = { Text("Any prep notes or materials needed...") },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("activity_notes_input"),
        shape = RoundedCornerShape(12.dp),
        maxLines = 3
    )

    Spacer(modifier = Modifier.height(16.dp))
}
