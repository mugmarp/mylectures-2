package com.lectures.features.rooms.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.features.rooms.BuildingLevel
import com.lectures.features.timetable.ui.DarkSurfaceCard
import com.lectures.features.timetable.ui.StatusGreenLive
import com.lectures.features.timetable.ui.TextMutedLight
import com.lectures.features.timetable.ui.TextPrimaryLight
import com.lectures.ui.components.AdaptiveSheetOrDialog
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.contrastOnColor

@Composable
internal fun VacantRoomsFilterSheet(
    queryDay: String,
    queryTime: String,
    isLiveNow: Boolean,
    minGapMinutes: Int,
    selectedLevel: BuildingLevel?,
    studyFriendlyOnly: Boolean,
    hideLibrary: Boolean,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onResetAll: () -> Unit,
    onSelectLiveNow: () -> Unit,
    onOpenDayPicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onPresetTimeSelected: (String) -> Unit,
    onMinGapSelected: (Int) -> Unit,
    onLevelSelected: (BuildingLevel?) -> Unit,
    onToggleStudyFriendly: () -> Unit,
    onToggleHideLibrary: () -> Unit
) {
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White

    AdaptiveSheetOrDialog(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        desktopMaxWidth = 580.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            VacantRoomsFilterOptionsContent(
                queryDay = queryDay,
                queryTime = queryTime,
                isLiveNow = isLiveNow,
                minGapMinutes = minGapMinutes,
                selectedLevel = selectedLevel,
                studyFriendlyOnly = studyFriendlyOnly,
                hideLibrary = hideLibrary,
                isDark = isDark,
                showApplyButton = true,
                onDismiss = onDismiss,
                onResetAll = onResetAll,
                onSelectLiveNow = onSelectLiveNow,
                onOpenDayPicker = onOpenDayPicker,
                onOpenTimePicker = onOpenTimePicker,
                onPresetTimeSelected = onPresetTimeSelected,
                onMinGapSelected = onMinGapSelected,
                onLevelSelected = onLevelSelected,
                onToggleStudyFriendly = onToggleStudyFriendly,
                onToggleHideLibrary = onToggleHideLibrary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VacantRoomsFilterOptionsContent(
    queryDay: String,
    queryTime: String,
    isLiveNow: Boolean,
    minGapMinutes: Int,
    selectedLevel: BuildingLevel?,
    studyFriendlyOnly: Boolean,
    hideLibrary: Boolean,
    isDark: Boolean,
    showApplyButton: Boolean,
    onDismiss: () -> Unit,
    onResetAll: () -> Unit,
    onSelectLiveNow: () -> Unit,
    onOpenDayPicker: () -> Unit,
    onOpenTimePicker: () -> Unit,
    onPresetTimeSelected: (String) -> Unit,
    onMinGapSelected: (Int) -> Unit,
    onLevelSelected: (BuildingLevel?) -> Unit,
    onToggleStudyFriendly: () -> Unit,
    onToggleHideLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Filter & Schedule Options",
                fontSize = if (showApplyButton) 18.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            TextButton(
                onClick = onResetAll,
                modifier = Modifier.desktopHandCursor()
            ) {
                Text("Reset All", color = accentColor, fontSize = 12.5.sp)
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schedule Day & Time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                FilterChip(
                    selected = isLiveNow,
                    onClick = onSelectLiveNow,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isLiveNow) StatusGreenLive else textMuted, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Live Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDayPicker,
                    modifier = Modifier
                        .weight(1f)
                        .desktopHandCursor(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(queryDay, fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenTimePicker,
                    modifier = Modifier
                        .weight(1f)
                        .desktopHandCursor(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(queryTime, fontSize = 12.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val presets = listOf(
                "08:00" to "Morning (08:00)",
                "11:00" to "Midday (11:00)",
                "14:00" to "Afternoon (14:00)",
                "17:00" to "Evening (17:00)"
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                presets.forEach { (presetTime, presetLabel) ->
                    val isSelected = !isLiveNow && queryTime == presetTime
                    FilterChip(
                        selected = isSelected,
                        onClick = { onPresetTimeSelected(presetTime) },
                        label = { Text(presetLabel, fontSize = 11.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.desktopHandCursor()
                    )
                }
            }
        }

        Column {
            Text(
                text = "Minimum Study Gap",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            val gapOptions = listOf(
                "Any free (0m)" to 0,
                "15 min" to 15,
                "30 min" to 30,
                "45 min" to 45,
                "1 hour" to 60
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                gapOptions.forEach { (label, value) ->
                    FilterChip(
                        selected = minGapMinutes == value,
                        onClick = { onMinGapSelected(value) },
                        label = { Text(label, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.desktopHandCursor()
                    )
                }
            }
        }

        Column {
            Text(
                text = "Floor / Level",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = selectedLevel == null,
                    onClick = { onLevelSelected(null) },
                    label = { Text("All Floors", fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.desktopHandCursor()
                )
                BuildingLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedLevel == level,
                        onClick = { onLevelSelected(level) },
                        label = { Text(level.shortName, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.desktopHandCursor()
                    )
                }
            }
        }

        Column {
            Text(
                text = "Room Preferences",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FilterChip(
                    selected = !studyFriendlyOnly,
                    onClick = onToggleStudyFriendly,
                    label = { Text(if (studyFriendlyOnly) "Classrooms Only" else "Include Offices", fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.desktopHandCursor()
                )
                FilterChip(
                    selected = hideLibrary,
                    onClick = onToggleHideLibrary,
                    label = { Text("Hide Library", fontSize = 11.5.sp) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.desktopHandCursor()
                )
            }
        }

        if (showApplyButton) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .desktopHandCursor(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = contrastOnColor(accentColor)
                )
            ) {
                Text("Apply & View Rooms", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

