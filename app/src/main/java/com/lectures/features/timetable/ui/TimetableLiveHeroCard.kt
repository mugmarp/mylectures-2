package com.lectures.features.timetable.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.LivePulseBadge
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.rememberPrefersReducedMotion

@Composable
fun HappeningNowHeroCard(
    session: SpecClassSession,
    progress: Float = session.progress,
    elapsedText: String = session.elapsedText,
    remainingText: String = session.remainingText,
    onQuickNote: () -> Unit,
    onAlarmDismiss: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val appTheme = LocalAppTheme.current
    val isDark = appTheme.isDark
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .then(
                if (isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = darkAccentGlow.copy(alpha = 0.48f),
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
            .clip(HeroCardShape)
            .desktopHandCursor()
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LivePulseBadge(
                    text = "HAPPENING NOW",
                    dotColor = Color(0xFF4ADE80),
                    containerColor = Color.White.copy(alpha = 0.2f),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    dotSize = 7.dp,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                )

                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
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
                        text = remainingText,
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
                        .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
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
                    text = "• ${session.type.label} · ${session.startTime} – ${session.endTime}",
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

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .background(Color(0xFF4ADE80), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = elapsedText,
                            fontSize = 11.sp,
                            color = Color.White.copy(0.9f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = remainingText,
                        fontSize = 11.sp,
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val reducedMotion = rememberPrefersReducedMotion()
                val animatedProgress by animateFloatAsState(
                    targetValue = progress.coerceIn(0f, 1f),
                    animationSpec = MotionSpec.stateTween(450, reducedMotion),
                    label = "hero_live_progress"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(ProgressTrackShape)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = animatedProgress)
                            .fillMaxHeight()
                            .clip(ProgressTrackShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(ProgressGradientStart, ProgressGradientEnd)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = session.startTime,
                        fontSize = 10.sp,
                        color = Color.White.copy(0.6f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}% completed",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(0.85f)
                    )
                    Text(
                        text = session.endTime,
                        fontSize = 10.sp,
                        color = Color.White.copy(0.6f)
                    )
                }
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
                    onClick = onQuickNote,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .desktopHandCursor(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.primary else Color.White,
                        contentColor = if (isDark) MaterialTheme.colorScheme.onPrimary else heroStart
                    ),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Quick Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onAlarmDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .desktopHandCursor(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = if (isDark) 0.14f else 0.2f),
                        contentColor = Color.White
                    ),
                    shape = ActionButtonShape,
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Outlined.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Alarm / Dismiss", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
