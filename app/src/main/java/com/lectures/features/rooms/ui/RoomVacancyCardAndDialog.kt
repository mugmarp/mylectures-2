package com.lectures.features.rooms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.lectures.features.rooms.RoomItem
import com.lectures.features.rooms.RoomType
import com.lectures.features.rooms.RoomVacancyStatus
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.ui.BorderSubtleLight
import com.lectures.features.timetable.ui.DarkBorderSubtle
import com.lectures.features.timetable.ui.DarkSurfaceBase
import com.lectures.features.timetable.ui.DarkSurfaceCard
import com.lectures.features.timetable.ui.StatusGreenLive
import com.lectures.features.timetable.ui.TextMutedLight
import com.lectures.features.timetable.ui.TextPrimaryLight
import com.lectures.ui.components.FloorIndicatorBadge

fun formatRoomDisplayCode(room: RoomItem): String {
    val raw = room.code
    return when {
        raw.contains("Kihumuro Library Room 1", ignoreCase = true) -> "Study Room 1"
        raw.contains("Kihumuro Library Room 2", ignoreCase = true) -> "Study Room 2"
        raw.contains("Kihumuro Library Room 3", ignoreCase = true) -> "Study Room 3"
        raw.contains("Kihumuro Library Room 4", ignoreCase = true) -> "Study Room 4"
        raw.contains("FCI Library 1", ignoreCase = true) -> "Library Room 1"
        raw.contains("FCI Library 2", ignoreCase = true) -> "Library Room 2"
        else -> raw
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RoomVacancyCard(
    status: RoomVacancyStatus,
    isDark: Boolean,
    showCampus: Boolean = false,
    selectedBuilding: String? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val room = status.room
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    val statusColor = when {
        !room.isStudyFriendly -> Color(0xFF8B5CF6)
        status.isOccupied -> Color(0xFFEF4444)
        room.isPermanentlyVacant -> Color(0xFF0EA5E9)
        status.gapMinutes >= 60 || status.freeUntil == "Rest of day" -> Color(0xFF16A34A)
        else -> Color(0xFFF59E0B)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderSubtle),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val displayCode = formatRoomDisplayCode(room)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = displayCode,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FloorIndicatorBadge(
                            level = room.level,
                            compact = true,
                            isDark = isDark
                        )
                    }

                    val isLibrary = room.type == RoomType.LIBRARY || room.buildingName.contains("Library", ignoreCase = true)
                    val showTypeBadge = !isLibrary && room.type != RoomType.CLASSROOM
                    if (showTypeBadge) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDark) Color(0xFF334155).copy(alpha = 0.5f) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = room.type.displayName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (selectedBuilding == null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (showCampus) "${room.buildingName} · ${room.campus}" else room.buildingName,
                        fontSize = 13.sp,
                        color = textMuted,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(if (selectedBuilding == null) 7.dp else 5.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = status.formattedAvailability,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                if (!status.isOccupied && status.nextSession != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val cohort = status.nextSession.programmeGroup.ifBlank { "Students" }
                    val timeStr = status.nextSession.startTime
                    Text(
                        text = "Next: $cohort (starts at $timeStr)",
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (status.isOccupied && status.currentSession != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    val cohort = status.currentSession.programmeGroup.ifBlank { "Students" }
                    val endStr = status.currentSession.endTime
                    Text(
                        text = "Current: $cohort (ends at $endStr)",
                        fontSize = 11.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "View Day Schedule",
                tint = textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun RoomScheduleDetailDialog(
    status: RoomVacancyStatus,
    dayOfWeek: String,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val room = status.room
    val sessions = status.allDaySessions
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 580.dp)
                .padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatRoomDisplayCode(room),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            FloorIndicatorBadge(
                                level = room.level,
                                compact = true,
                                isDark = isDark
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "${room.buildingName} · $dayOfWeek",
                            fontSize = 12.5.sp,
                            color = textMuted
                        )
                        val accentColor = MaterialTheme.colorScheme.primary
                        if (room.usedByFaculties.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Used by: ${room.usedByFaculties.joinToString(", ")}",
                                fontSize = 11.sp,
                                color = accentColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onDismiss) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    val cleanNotes = remember(room.notes) {
                        room.notes
                            .replace(Regex("(?i)NOTE:\\s*duplicate listing in source data\\.?"), "")
                            .replace(Regex("(?i)NOTE:\\s*exists in FCI directory.*"), "")
                            .trim()
                    }
                    val showCustomNotes = cleanNotes.isNotBlank() &&
                        !cleanNotes.contains("No scheduled sessions", ignoreCase = true) &&
                        !cleanNotes.contains("scheduled session(s)", ignoreCase = true)

                    if (showCustomNotes) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF8B5CF6).copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cleanNotes,
                                fontSize = 12.sp,
                                color = Color(0xFF8B5CF6),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Text(
                        text = "All Scheduled Sessions (${sessions.size})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (sessions.isEmpty()) {
                        val isLibraryVenue = room.type == RoomType.LIBRARY || room.buildingName.contains("Library", ignoreCase = true)
                        val isPermanentlyVacant = room.isPermanentlyVacant || room.sessionsThisSemester == 0

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) DarkSurfaceBase else Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(StatusGreenLive.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLibraryVenue) Icons.Outlined.LocalLibrary else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = StatusGreenLive,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = when {
                                        isLibraryVenue -> "Open for Self-Study & Reading"
                                        isPermanentlyVacant -> "Unallocated Classroom · Free All Day"
                                        else -> "No Scheduled Lectures on $dayOfWeek"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Text(
                                    text = when {
                                        isLibraryVenue -> "This is a dedicated academic library / study facility. No lectures are timetable-allocated here, so it is freely accessible for individual study, reading, and research throughout $dayOfWeek."
                                        isPermanentlyVacant -> "This venue has zero scheduled teaching sessions in the 2026/2027 Semester I timetable. You can freely use it for quiet revision, coursework, or group discussions."
                                        else -> "No teaching sessions are allocated to this room on $dayOfWeek. It is open and available for student study, revision, or coursework."
                                    },
                                    fontSize = 12.sp,
                                    color = textMuted,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 17.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = StatusGreenLive.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (isLibraryVenue) "✓ Quiet Study Friendly" else "✓ Full Day Free",
                                            color = StatusGreenLive,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = room.level.displayName,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sessions.forEach { session: TimetableEntry ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else BorderSubtleLight),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.width(68.dp)
                                        ) {
                                            Text(
                                                text = session.startTime,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = session.endTime,
                                                fontSize = 11.sp,
                                                color = textMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))
                                        VerticalDivider(
                                            modifier = Modifier.height(34.dp),
                                            color = textMuted.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.courseCode,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = textPrimary
                                            )
                                            Text(
                                                text = session.courseTitle,
                                                fontSize = 12.sp,
                                                color = textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (session.programmeGroup.isNotBlank()) {
                                                Text(
                                                    text = "Group: ${session.programmeGroup}",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
