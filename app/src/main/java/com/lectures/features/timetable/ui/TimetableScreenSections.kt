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
import com.lectures.features.timetable.domain.CustomEvent

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
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF291B00) else Color(0xFFFEF3C7),
        border = BorderStroke(1.dp, if (isDark) Color(0xFFB45309) else Color(0xFFF59E0B)),
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
                    .background(if (isDark) Color(0xFF78350F) else Color(0xFFFDE68A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = "Peek Mode",
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Previewing $programme",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                )
                Text(
                    text = "Viewing temporarily. Your alarms remain set for $enrolledProgramme.",
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onResetToEnrolled,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFFB45309) else Color(0xFFD97706),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
                    color = if (isDark) Color.White else TextPrimaryLight
                )
                Text(
                    text = "Select your programme to view your class timetable.",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSelectClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
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
                color = if (isDark) Color.White else TextPrimaryLight
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = todayDateFormatted,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF94A3B8) else TextMutedLight,
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
    PaddingBox(top = 4.dp, bottom = 2.dp) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF10B981).copy(alpha = 0.12f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "All classes for today are completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
internal fun NoClassesTodayCard(isDark: Boolean) {
    PaddingBox(top = 8.dp, bottom = 6.dp) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) DarkSurfaceCard else Color.White,
            border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
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
                        color = if (isDark) Color.White else TextPrimaryLight
                    )
                    Text(
                        text = "Take time for self-study or add personal activities.",
                        fontSize = 12.sp,
                        color = if (isDark) Color(0xFF94A3B8) else TextMutedLight
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
    PaddingBox(top = 24.dp, bottom = 24.dp) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isDark) DarkSurfaceCard else Color.White,
            border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
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
                    color = if (isDark) Color.White else TextPrimaryLight
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Take time to study, revise, or add a custom study session.",
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFF94A3B8) else TextMutedLight,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onAddCustomActivityClick,
                    shape = ActionButtonShape,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    AlertDialog(
        onDismissRequest = onDismiss,
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
                    text = "Category: ${event.category}",
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
                TextButton(onClick = { onEdit(event.id) }) {
                    Text("Edit", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDelete(event.id) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete Activity")
            }
        }
    )
}
