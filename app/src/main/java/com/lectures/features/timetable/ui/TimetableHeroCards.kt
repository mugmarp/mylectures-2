package com.lectures.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.AccentColors
import com.lectures.ui.AccentDarkColors
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.RoomFloorBadge

@Composable
fun NextUpHeroCard(
    session: SpecClassSession,
    onSetAlarm: () -> Unit,
    onQuickNote: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppTheme.current
    val isDark = appTheme.isDark
    val ext = LocalExtendedColors.current
    val safeAccentIdx = appTheme.accentIndex.coerceIn(0, AccentColors.lastIndex)
    val heroStart = if (isDark) {
        when (safeAccentIdx) {
            0 -> Color(0xFF15223B)
            1 -> Color(0xFF0A2E25)
            2 -> Color(0xFF22163B)
            3 -> Color(0xFF2E1B08)
            else -> Color(0xFF0D283A)
        }
    } else {
        AccentColors[safeAccentIdx]
    }
    val heroEnd = if (isDark) {
        when (safeAccentIdx) {
            0 -> Color(0xFF1B1938)
            1 -> Color(0xFF0B221D)
            2 -> Color(0xFF1A1430)
            3 -> Color(0xFF22160A)
            else -> Color(0xFF0C1E2E)
        }
    } else {
        when (safeAccentIdx) {
            0 -> Color(0xFF4F46E5)
            1 -> Color(0xFF047857)
            2 -> Color(0xFF5B21B6)
            3 -> Color(0xFFB45309)
            else -> Color(0xFF0369A1)
        }
    }
    val darkAccentGlow = AccentDarkColors[safeAccentIdx]
    val cardTextPrimary = if (isDark) ext.textPrimary else Color.White

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .then(
                if (isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = darkAccentGlow.copy(alpha = 0.42f),
                        shape = HeroCardShape
                    )
                } else {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = HeroCardShape,
                        ambientColor = heroStart.copy(alpha = 0.4f)
                    )
                }
            )
            .background(
                brush = Brush.verticalGradient(listOf(heroStart, heroEnd)),
                shape = HeroCardShape
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(StatusGreenLive, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEXT UP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = session.startsInText ?: if (session.startTime.isNotBlank()) "Starts ${session.startTime}" else "Upcoming",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = session.courseCode,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "• ${session.type.label}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.courseName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            session.topicDescription?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            val hasVenue = !session.venue.isNullOrBlank()
            val hasLecturer = !session.lecturer.isNullOrBlank()

            if (hasVenue || hasLecturer) {
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasVenue) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MeetingRoom,
                                    contentDescription = "Venue",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Venue", fontSize = 10.sp, color = Color.White.copy(0.7f))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = session.venue!!,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    RoomFloorBadge(
                                        roomName = session.venue,
                                        floor = session.floor,
                                        compact = true,
                                        useContrastColor = true
                                    )
                                }
                            }
                        }
                    }

                    if (hasLecturer) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Lecturer", fontSize = 10.sp, color = Color.White.copy(0.7f))
                                Text(
                                    text = session.lecturer!!,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSetAlarm,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            isDark && session.isAlarmSet -> ext.successBg
                            isDark -> MaterialTheme.colorScheme.primary
                            session.isAlarmSet -> Color(0xFFDCFCE7)
                            else -> Color.White
                        },
                        contentColor = when {
                            isDark && session.isAlarmSet -> ext.successText
                            isDark -> MaterialTheme.colorScheme.onPrimary
                            session.isAlarmSet -> Color(0xFF15803D)
                            else -> heroStart
                        }
                    ),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    val leadMins = session.alarmMinutes ?: 30
                    Icon(
                        imageVector = if (session.isAlarmSet) Icons.Outlined.CheckCircle else Icons.Outlined.Alarm,
                        contentDescription = if (session.isAlarmSet) "Alarm active ($leadMins min before)" else "Set ${leadMins}m Alarm",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (session.isAlarmSet) "Alarm Set (${leadMins}m)" else "Set ${leadMins}m Alarm",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onQuickNote,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.2f),
                        contentColor = cardTextPrimary
                    ),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EditNote,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Quick Note",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun DockedUpNextBar(
    session: SpecClassSession,
    startsInText: String = "In 35m",
    onSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val ext = LocalExtendedColors.current
    val accentColor = MaterialTheme.colorScheme.primary
    val containerColor = if (isDark) DarkSurfaceCard else SurfaceContainerLowLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val textPrimary = if (isDark) ext.textPrimary else TextPrimaryLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onSessionClick),
        shape = UpNextBarShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FastForward,
                        contentDescription = "Up Next",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "UP NEXT • ${session.startTime}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${session.courseCode}",
                            fontSize = 11.sp,
                            color = if (isDark) ext.textMuted else TextMutedLight
                        )
                    }
                    Text(
                        text = session.courseName,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(accentColor.copy(alpha = 0.08f), PillBadgeShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = startsInText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }
    }
}
