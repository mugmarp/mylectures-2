package com.lectures.features.rooms.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lectures.features.rooms.RoomItem
import com.lectures.features.rooms.RoomType
import com.lectures.features.rooms.RoomVacancyStatus
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.FloorIndicatorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion

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
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val room = status.room
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val surfaceColor = ext.surfaceCard
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted
    val borderSubtle = ext.borderSubtle
    val primaryColor = MaterialTheme.colorScheme.primary

    val statusColor = when {
        !room.isStudyFriendly -> Color(0xFF8B5CF6)
        status.isOccupied -> ext.dangerIcon
        room.isPermanentlyVacant -> Color(0xFF0EA5E9)
        status.gapMinutes >= 60 || status.freeUntil == "Rest of day" -> ext.successIcon
        else -> ext.warningIcon
    }

    val cardBg by animateColorAsState(
        targetValue = if (isSelected) primaryColor.copy(alpha = 0.08f) else surfaceColor,
        animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
        label = "room_card_bg"
    )
    val cardBorderColor by animateColorAsState(
        targetValue = if (isSelected) primaryColor else borderSubtle,
        animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
        label = "room_card_border"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = cardBorderColor
        ),
        modifier = modifier
            .fillMaxWidth()
            .desktopHoverable(
                enabled = !isSelected,
                hoverContainerColor = primaryColor.copy(alpha = 0.06f)
            )
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
                                .background(ext.surfaceElevated, RoundedCornerShape(6.dp))
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
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val useWide = windowInfo.preferDesktopDialogs
    val dialogMaxH = minOf(640.dp, (windowInfo.heightDp - 48).coerceAtLeast(360).dp)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = !useWide)
    ) {
        var entered by remember { mutableStateOf(reducedMotion) }
        LaunchedEffect(Unit) { entered = true }
        val scale by animateFloatAsState(
            targetValue = if (entered) 1f else 0.95f,
            animationSpec = if (reducedMotion) snap() else MotionSpec.standardSpring(),
            label = "room_dialog_scale"
        )
        val alpha by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = if (reducedMotion) snap() else tween(MotionSpec.DIALOG_ENTER_MS),
            label = "room_dialog_alpha"
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .then(
                    if (useWide) Modifier.widthIn(min = 420.dp, max = 600.dp) else Modifier
                )
                .fillMaxWidth()
                .heightIn(max = dialogMaxH)
                .padding(horizontal = if (useWide) 24.dp else 4.dp)
                .onEscapeKey(onDismiss)
        ) {
            RoomScheduleInspectorContent(
                status = status,
                dayOfWeek = dayOfWeek,
                isDark = isDark,
                showBottomCloseButton = true,
                onDismiss = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }
    }
}

@Composable
internal fun EmptyRoomInspectorCard(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted
    val borderSubtle = ext.borderSubtle
    val surfaceColor = ext.surfaceCard

    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.MeetingRoom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Room Schedule Inspector",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Select any classroom or lab from the grid to inspect its full-day occupancy timeline, study friendliness, and allocated programmes.",
                fontSize = 12.5.sp,
                color = textMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
internal fun RoomScheduleInspectorContent(
    status: RoomVacancyStatus,
    dayOfWeek: String,
    isDark: Boolean,
    showBottomCloseButton: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val room = status.room
    val sessions = status.allDaySessions
    val ext = LocalExtendedColors.current
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted

    Column(modifier = modifier) {
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

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
                    .testTag("room_inspector_close_btn")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(ext.surfaceElevated, CircleShape),
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
                            color = if (isDark) ext.surfaceBase else ext.successBg,
                            border = BorderStroke(1.dp, if (isDark) ext.borderSubtle else ext.successBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(ext.successIcon.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLibraryVenue) Icons.Outlined.LocalLibrary else Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        tint = ext.successIcon,
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
                                        color = ext.successBg
                                    ) {
                                        Text(
                                            text = if (isLibraryVenue) "✓ Quiet Study Friendly" else "✓ Full Day Free",
                                            color = ext.successText,
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
                                    color = ext.surfaceBase,
                                    border = BorderStroke(1.dp, ext.borderSubtle),
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

        if (showBottomCloseButton) {
            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .desktopHandCursor(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = contrastOnColor(MaterialTheme.colorScheme.primary)
                )
            ) {
                Text("Close", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

