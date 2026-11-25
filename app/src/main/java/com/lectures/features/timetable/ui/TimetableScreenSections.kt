package com.lectures.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.lectures.features.timetable.domain.ActivityCategory
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow

@Composable
internal fun PaddingBox(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = top, bottom = bottom)
    ) {
        content()
    }
}

@Composable
internal fun TimetablePeekBanner(
    programme: String,
    enrolledProgramme: String,
    isDark: Boolean,
    onResetToEnrolled: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ext.warningBg,
        border = BorderStroke(1.dp, ext.warningBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(ext.warningIcon.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Peek Mode",
                    tint = ext.warningIcon,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Previewing $programme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.warningText
                )
                Text(
                    text = "Viewing temporarily. Your alarms remain set for $enrolledProgramme.",
                    fontSize = 11.sp,
                    color = ext.warningText.copy(alpha = 0.85f),
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onResetToEnrolled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ext.warningIcon,
                    contentColor = contrastOnColor(ext.warningIcon)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Exit Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun NoProgrammePromptCard(
    isDark: Boolean,
    onSelectClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No Programme Selected",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = ext.textPrimary
                )
                Text(
                    text = "Select your programme to view your class timetable.",
                    fontSize = 12.sp,
                    color = ext.textMuted
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSelectClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = contrastOnColor(MaterialTheme.colorScheme.primary)
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Select", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun TodaysScheduleHeader(
    todayDateFormatted: String,
    todaySessionCount: Int,
    isDark: Boolean
) {
    val ext = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Today's Schedule",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = ext.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = todayDateFormatted,
                fontSize = 13.sp,
                color = ext.textMuted,
                fontWeight = FontWeight.Medium
            )
        }

        if (todaySessionCount > 0) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Text(
                    text = "$todaySessionCount ${if (todaySessionCount == 1) "session" else "sessions"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun TodayClassesCompletedBadge() {
    val ext = LocalExtendedColors.current
    PaddingBox(top = 4.dp, bottom = 2.dp) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = ext.successBg,
            border = BorderStroke(1.dp, ext.successBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = ext.successIcon,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All classes for today are completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ext.successText
                )
            }
        }
    }
}

@Composable
internal fun NoClassesTodayCard(isDark: Boolean) {
    val ext = LocalExtendedColors.current
    PaddingBox(top = 8.dp, bottom = 6.dp) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ext.surfaceCard,
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "No classes scheduled for today",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = ext.textPrimary
                    )
                    Text(
                        text = "Take time for self-study or add personal activities.",
                        fontSize = 12.sp,
                        color = ext.textMuted
                    )
                }
            }
        }
    }
}

@Composable
internal fun EmptyDayScheduleCard(
    selectedDay: String,
    isDark: Boolean,
    onAddCustomActivityClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    PaddingBox(top = 24.dp, bottom = 24.dp) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ext.surfaceCard,
            border = BorderStroke(1.dp, ext.borderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "No classes scheduled for $selectedDay",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = ext.textPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Take time to study, revise, or add a custom study session.",
                    fontSize = 12.sp,
                    color = ext.textMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAddCustomActivityClick,
                    shape = ActionButtonShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = contrastOnColor(MaterialTheme.colorScheme.primary)
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Custom Activity", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
internal fun TimetableActivityDetailDialog(
    event: CustomEvent,
    onDismiss: () -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
        modifier = Modifier
            .then(
                if (windowInfo.preferDesktopDialogs) {
                    Modifier.widthIn(min = 360.dp, max = 500.dp)
                } else {
                    Modifier
                }
            )
            .onEscapeKey(onDismiss),
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
                    Text(
                        text = "Venue: ${event.location}",
                        style = MaterialTheme.typography.bodyMedium
                    )
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
                    onClick = { onEdit(event.id) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text("Edit", fontWeight = FontWeight.Bold)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDelete(event.id) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .desktopHandCursor()
            ) {
                Text("Delete Activity")
            }
        }
    )
}
