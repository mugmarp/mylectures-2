package com.lectures.features.timetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.RoomFloorBadge

@Composable
fun TimelineClassCard(
    session: SpecClassSession,
    onToggleAlarm: () -> Unit,
    onAttachedNoteClick: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = session.type.tagColor
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextSecondaryLight
    val textTertiary = if (isDark) Color(0xFF64748B) else TextTertiaryLight
    val noteBg = if (isDark) DarkSurfaceContainer else SurfaceContainerLowLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(elevation = 2.dp, shape = TimelineCardShape)
            .background(cardBg, TimelineCardShape)
            .border(
                width = if (session.hasConflict) 2.dp else 1.dp,
                color = if (session.hasConflict) Color(0xFFDC2626) else borderSubtle,
                shape = TimelineCardShape
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(4.dp)
                .matchParentSize()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                .background(categoryColor)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${session.startTime} – ${session.endTime}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(categoryColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = session.type.label,
                            color = categoryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                IconButton(onClick = onToggleAlarm) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Set Reminder",
                        tint = if (session.isAlarmSet) accentColor else textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (session.hasConflict) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFFDC2626).copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Schedule conflict detected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFDC2626)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = session.courseName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            val isAssoc = session.type == SpecSessionType.ASSOCIATION ||
                session.courseCode.contains("MUCOSA", ignoreCase = true) ||
                session.courseName.contains("MUCOSA", ignoreCase = true)

            if (isAssoc) {
                if (!session.courseCode.equals(session.courseName, ignoreCase = true) && session.courseCode.isNotBlank()) {
                    Text(
                        text = session.courseCode,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            } else {
                val subtitleText = if (session.department.isNotBlank()) {
                    "${session.courseCode} · ${session.department}"
                } else {
                    session.courseCode
                }
                Text(
                    text = subtitleText,
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val hasVenue = !session.venue.isNullOrBlank()
            val hasLecturer = !session.lecturer.isNullOrBlank()
            val hasStartsIn = session.startsInText != null

            if (hasVenue || hasLecturer || hasStartsIn) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (hasVenue) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = session.venue!!,
                                fontSize = 12.sp,
                                color = textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            RoomFloorBadge(
                                roomName = session.venue,
                                floor = session.floor,
                                compact = true,
                                isDark = isDark
                            )
                        }
                    }
                    if (hasVenue && hasLecturer) {
                        Text("•", color = textTertiary)
                    }
                    if (hasLecturer) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Lecturer",
                                tint = textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = session.lecturer!!,
                                fontSize = 12.sp,
                                color = textSecondary
                            )
                        }
                    }
                    if ((hasVenue || hasLecturer) && hasStartsIn) {
                        Text("•", color = textTertiary)
                    }
                    if (hasStartsIn) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .background(StatusGreenLive, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = session.startsInText!!,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StatusGreenLive
                            )
                        }
                    }
                }
            }

            session.attachedNotePreview?.takeIf { it.isNotBlank() }?.let { notePreview ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(noteBg, RoundedCornerShape(10.dp))
                        .clickable(onClick = onAttachedNoteClick)
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "1 attached note: \"$notePreview\"",
                            fontSize = 11.sp,
                            color = textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = null,
                        tint = textTertiary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
