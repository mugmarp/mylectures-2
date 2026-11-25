package com.lectures.features.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassAlarmPickerDialog(
    entry: TimetableEntry,
    currentMinutes: Int?,
    onConfirm: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = LocalAppTheme.current.isDark
    val ext = com.lectures.ui.LocalExtendedColors.current
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = ext.surfaceCard
    val textColor = ext.textPrimary
    val textSub = ext.textMuted

    val standardPresets = listOf(5, 10, 15, 30, 45, 60)
    val isCustomInitial = currentMinutes != null && currentMinutes !in standardPresets

    var selectedMinutes by remember { mutableStateOf<Int?>(currentMinutes ?: 30) }
    var isCustomSelected by remember { mutableStateOf(isCustomInitial) }
    var customText by remember { mutableStateOf(if (isCustomInitial) currentMinutes.toString() else "") }

    val startMinutes = remember(entry.startTime) { TimeUtil.toMinutes(entry.startTime) }
    val effectiveMinutes = if (isCustomSelected) customText.toIntOrNull() else selectedMinutes
    val canConfirm = effectiveMinutes != null && effectiveMinutes > 0
    val confirmAlarm: () -> Unit = {
        val finalMinutes = if (isCustomSelected) customText.toIntOrNull() else selectedMinutes
        if (finalMinutes != null && finalMinutes > 0) {
            if (!com.lectures.core.util.NotificationPermissionHelper.areNotificationsEnabled(context)) {
                android.widget.Toast.makeText(
                    context,
                    "Alarm saved! Enable notifications in Settings if you'd like audible alerts.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            onConfirm(finalMinutes)
        }
    }

    val triggerTimeDisplay = remember(effectiveMinutes, startMinutes) {
        if (effectiveMinutes == null || effectiveMinutes <= 0 || startMinutes <= 0) null
        else {
            val triggerMin = (startMinutes - effectiveMinutes + 1440) % 1440
            String.format(java.util.Locale.US, "%02d:%02d", triggerMin / 60, triggerMin % 60)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 400.dp, max = 520.dp)
                } else {
                    Modifier
                }
            )
            .heightIn(max = (windowInfo.heightDp - 48).coerceAtLeast(360).dp)
            .onEscapeKey(onDismiss),
        containerColor = surfaceColor,
        shape = RoundedCornerShape(20.dp),
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Class Alarm: ${entry.courseCode}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.dayOfWeek} · ${entry.startTime} – ${entry.endTime}",
                    fontSize = 13.sp,
                    color = textSub
                )
                val roomText = entry.room?.trim()
                if (!roomText.isNullOrEmpty() && roomText.uppercase(java.util.Locale.ROOT) !in listOf("TBA", "TBD", "NONE", "N/A")) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "📍 $roomText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textSub
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RoomFloorBadge(roomName = roomText, floor = entry.floor, compact = true, isDark = isDark)
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Alert Lead Time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Presets Grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardPresets.forEach { mins ->
                        val isSelected = !isCustomSelected && selectedMinutes == mins
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .desktopHandCursor()
                                .clickable {
                                    isCustomSelected = false
                                    selectedMinutes = mins
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m before",
                                color = if (isSelected) contrastOnColor(primaryColor) else textColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }

                    // Custom Option Chip
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isCustomSelected) primaryColor else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                1.dp,
                                if (isCustomSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .desktopHandCursor()
                            .clickable {
                                isCustomSelected = true
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Custom",
                            color = if (isCustomSelected) contrastOnColor(primaryColor) else textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isCustomSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }

                // Custom Input Field
                if (isCustomSelected) {
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = customText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }.take(3)
                            customText = filtered
                            selectedMinutes = filtered.toIntOrNull()
                        },
                        label = { Text("Custom minutes before class") },
                        placeholder = { Text("e.g. 20, 25, 40, 90") },
                        leadingIcon = {
                            Icon(Icons.Outlined.Timer, contentDescription = null, tint = primaryColor)
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { confirmAlarm() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                if (triggerTimeDisplay != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = primaryColor.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Alarm, contentDescription = null, tint = primaryColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alarm will sound at $triggerTimeDisplay (${effectiveMinutes}m before)",
                                fontSize = 12.sp,
                                color = primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = confirmAlarm,
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = contrastOnColor(primaryColor)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor(enabled = canConfirm)
            ) {
                Text("Set Alarm")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentMinutes != null) {
                    TextButton(
                        onClick = { onConfirm(null) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ext.dangerIcon),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.AlarmOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Turn Off")
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
