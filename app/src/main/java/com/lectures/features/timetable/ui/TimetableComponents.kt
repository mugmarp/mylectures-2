package com.lectures.features.timetable.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.TimeUtil
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.rememberLecturesWindow
import java.util.Calendar
import java.util.Locale

@Composable
fun TimetableTopAppBar(
    program: String,
    subtitle: String = TimeUtil.currentSemesterWeekLabel(),
    isPeeking: Boolean = false,
    hasUnreadNotifications: Boolean = false,
    onNotificationClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenVacantRooms: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val capsuleBg = if (isDark) DarkSurfaceCard else SurfaceContainerLowLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val iconTint = if (isDark) Color(0xFFCBD5E1) else TextSecondaryLight

    FloatingIslandTopBar(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Timetable",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        if (program.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = program,
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            val windowInfo = rememberLecturesWindow()

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenVacantRooms() }
                        .testTag("topbar_vacant_rooms_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(
                            horizontal = if (windowInfo.topBarCompactAction) 8.dp else 10.dp,
                            vertical = 6.dp
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MeetingRoom,
                            contentDescription = "Vacant Rooms",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        if (!windowInfo.topBarCompactAction) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Free Rooms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = capsuleBg,
                    border = BorderStroke(1.dp, borderSubtle),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .clickable { onProfileClick() }
                        .testTag("topbar_academic_info_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Academic Info",
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleHeaderSection(
    title: String = "Today's Schedule",
    dateText: String = "Tuesday, Sep 8",
    activeFilter: String = "All Sessions",
    onFilterClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val pillBg = if (isDark) DarkSurfaceCard else SurfaceContainerLowLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = dateText,
                fontSize = 13.sp,
                color = textMuted,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Surface(
            onClick = onFilterClicked,
            shape = RoundedCornerShape(14.dp),
            color = pillBg,
            border = BorderStroke(1.dp, borderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Filter",
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeFilter,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

data class SpecDayItem(
    val name: String,
    val isToday: Boolean,
    val isSelected: Boolean
)

@Composable
fun DayCapsuleStrip(
    dayItems: List<SpecDayItem>,
    onDaySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val windowInfo = rememberLecturesWindow()
    val isCompact = windowInfo.dayStripCompact

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(if (isCompact) 4.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        dayItems.forEach { item ->
            val isActive = item.isSelected
            val isToday = item.isToday

            val capsuleModifier = when {
                isActive -> {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = accentColor.copy(alpha = 0.35f),
                            spotColor = accentColor.copy(alpha = 0.45f)
                        )
                        .background(accentColor, RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(item.name) }
                }
                isToday -> {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .background(
                            if (isDark) accentColor.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(width = 1.5.dp, color = accentColor, shape = RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(item.name) }
                }
                else -> {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .background(if (isDark) DarkSurfaceCard else Color.White, RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = if (isDark) DarkBorderSubtle else BorderSubtleLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(item.name) }
                }
            }

            val dayLabel = if (isCompact) {
                when (item.name.lowercase(Locale.ROOT).take(3)) {
                    "thu" -> "Th"
                    "tue" -> "Tu"
                    "sun" -> "Su"
                    "sat" -> "Sa"
                    "wed" -> "W"
                    "mon" -> "M"
                    "fri" -> "F"
                    else -> item.name.take(1)
                }
            } else {
                item.name.take(3)
            }

            Box(
                modifier = capsuleModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabel,
                    fontSize = if (isCompact) 12.sp else 13.sp,
                    fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isActive -> Color.White
                        isToday -> accentColor
                        isDark -> Color.White
                        else -> TextSecondaryLight
                    },
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun DayCapsuleStrip(
    selectedDayIndex: Int,
    onDaySelected: (Int, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val todayIndex = run {
        val dow = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (dow == Calendar.SUNDAY) 6 else dow - 2
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEachIndexed { index, day ->
            val isActive = selectedDayIndex == index
            val isToday = index == todayIndex

            val capsuleModifier = when {
                isActive -> {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .shadow(
                            elevation = 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = accentColor.copy(alpha = 0.35f),
                            spotColor = accentColor.copy(alpha = 0.45f)
                        )
                        .background(accentColor, RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(index, day) }
                }
                isToday -> {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .background(
                            if (isDark) accentColor.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.12f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(width = 1.5.dp, color = accentColor, shape = RoundedCornerShape(12.dp))
                        .clickable { onDaySelected(index, day) }
                }
                else -> {
                    Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .background(if (isDark) DarkSurfaceCard else Color.White, RoundedCornerShape(12.dp))
                        .border(
                            width = 1.dp,
                            color = if (isDark) DarkBorderSubtle else BorderSubtleLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onDaySelected(index, day) }
                }
            }

            Box(
                modifier = capsuleModifier,
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 13.sp,
                    fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = when {
                        isActive -> Color.White
                        isToday -> accentColor
                        isDark -> Color.White
                        else -> TextSecondaryLight
                    }
                )
            }
        }
    }
}

@Composable
fun OfflineArchiveCard(
    onViewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = BorderStroke(1.dp, borderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accentColor.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Offline Archive",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Original imported schedule fallback",
                        fontSize = 12.sp,
                        color = textMuted
                    )
                }
            }

            OutlinedButton(
                onClick = onViewClick,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, borderSubtle),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text("View", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
