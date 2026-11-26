package com.lectures.features.timetable.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.TimeUtil
import com.lectures.core.util.rememberLiveClockPulse
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.LivePulseBadge
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.rememberPrefersReducedMotion
import androidx.compose.foundation.border

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NextUpCard(entry: TimetableEntry, minutesUntil: Int, onClick: () -> Unit) {
    val style = sessionStyle(entry.sessionType, entry.room ?: "", entry.courseTitle)
    val clockPulse = rememberLiveClockPulse()
    val progress = remember(entry.startTime, entry.endTime, entry.dayOfWeek, clockPulse) {
        TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek)
    }

    val isLive = progress.isOngoing || minutesUntil == 0
    val isDark = LocalAppTheme.current.isDark
    val palette = rememberHeroCardPalette()
    val darkBorderGlow = palette.accentGlow
    val cardBrush = palette.horizontalBrush(isDark = isDark, isLive = isLive)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (isDark) {
                    Modifier.border(
                        width = 1.dp,
                        color = darkBorderGlow.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(24.dp)
                    )
                } else {
                    Modifier
                }
            )
            .desktopHandCursor()
            .clickable(onClick = onClick)
            .background(cardBrush)
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLive) {
                    LivePulseBadge(
                        text = "HAPPENING NOW",
                        dotColor = Color(0xFF4ADE80),
                        containerColor = Color(0xFF22C55E).copy(alpha = 0.25f),
                        contentColor = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        dotSize = 7.dp,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("NEXT UP", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                val timeBadge = when {
                    progress.isOngoing -> "${progress.remainingMinutes}m left"
                    minutesUntil == 0 -> "Happening now"
                    minutesUntil in 1..59 -> "in ${minutesUntil}m"
                    minutesUntil >= 60 -> "in ${minutesUntil / 60}h ${minutesUntil % 60}m"
                    else -> "on ${entry.dayOfWeek}"
                }
                Text(timeBadge, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.courseCode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(resolveSessionTypeLabel(entry), color = Color.White, fontSize = 12.sp)
                }

            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                entry.courseTitle,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 26.sp
            )
            
            Spacer(modifier = Modifier.height(14.dp))
            
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${entry.startTime} – ${entry.endTime}", color = Color.White, fontSize = 14.sp)
                }
                
                val roomText = entry.room?.trim()
                if (!roomText.isNullOrEmpty() && roomText.uppercase(java.util.Locale.ROOT) !in listOf("TBA", "TBD", "NONE", "N/A")) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.MeetingRoom, contentDescription = "Venue", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(roomText, color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        RoomFloorBadge(roomName = roomText, floor = entry.floor, compact = true, useContrastColor = true)
                    }
                }
            }

            // ONGOING PROGRESS BAR
            if (isLive) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    val percent = if (progress.totalMinutes > 0) (progress.progress * 100).toInt() else 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lecture Progress ($percent%)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (progress.remainingMinutes > 0) "${progress.remainingMinutes} min left" else "Ending soon",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val reducedMotion = rememberPrefersReducedMotion()
                    val animatedProgress by animateFloatAsState(
                        targetValue = if (progress.progress > 0f) progress.progress else 0.05f,
                        animationSpec = MotionSpec.stateTween(450, reducedMotion),
                        label = "nextup_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF4ADE80),
                        trackColor = Color.White.copy(alpha = 0.25f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${entry.startTime} (${progress.elapsedMinutes}m elapsed)",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${entry.endTime}",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            val lecturerText = entry.lecturer?.trim()?.takeIf {
                it.isNotBlank() && !it.equals("Staff", ignoreCase = true) && !it.equals("TBD", ignoreCase = true)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (lecturerText != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(lecturerText, color = Color.White, fontSize = 14.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            if (isDark) MaterialTheme.colorScheme.primary else Color.White,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = if (isDark) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
