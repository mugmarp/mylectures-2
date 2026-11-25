package com.lectures.features.timetable.ui

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.getValue
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
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
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
    val ext = LocalExtendedColors.current
    val accentColor = MaterialTheme.colorScheme.primary
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted
    val capsuleBg = ext.surfaceElevated
    val borderSubtle = ext.borderSubtle
    val iconTint = ext.textSecondary

    FloatingIslandTopBar(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
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
                        tint = com.lectures.ui.contrastOnColor(accentColor),
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
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!windowInfo.useNavigationRail) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .desktopHandCursor()
                            .clickable { onOpenVacantRooms() }
                            .testTag("topbar_vacant_rooms_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = accentColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (windowInfo.topBarCompactAction) 9.dp else 10.dp,
                                    vertical = 7.dp
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
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .desktopHandCursor()
                        .clickable { onProfileClick() }
                        .testTag("topbar_academic_info_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = capsuleBg,
                        border = BorderStroke(1.dp, borderSubtle),
                        modifier = Modifier.size(40.dp)
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
}

@Composable
fun ScheduleHeaderSection(
    title: String = "Today's Schedule",
    dateText: String = "Tuesday, Sep 8",
    activeFilter: String = "All Sessions",
    onFilterClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val accentColor = MaterialTheme.colorScheme.primary
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted
    val pillBg = ext.surfaceElevated
    val borderSubtle = ext.borderSubtle

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
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

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .desktopHandCursor()
                .clickable(onClick = onFilterClicked),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = pillBg,
                border = BorderStroke(1.dp, borderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
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

            val targetBg = when {
                isActive -> accentColor
                isToday -> if (isDark) accentColor.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.12f)
                else -> ext.surfaceCard
            }
            val targetBorder = when {
                isActive || isToday -> accentColor
                else -> ext.borderSubtle
            }
            val targetText = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                isToday -> accentColor
                else -> ext.textSecondary
            }

            val animBg by animateColorAsState(
                targetValue = targetBg,
                animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                label = "day_capsule_bg"
            )
            val animBorder by animateColorAsState(
                targetValue = targetBorder,
                animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                label = "day_capsule_border"
            )
            val animText by animateColorAsState(
                targetValue = targetText,
                animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                label = "day_capsule_text"
            )

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

            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .then(
                        if (isActive) {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = shape,
                                ambientColor = accentColor.copy(alpha = 0.35f),
                                spotColor = accentColor.copy(alpha = 0.45f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(shape)
                    .background(animBg, shape)
                    .border(
                        width = if (isToday && !isActive) 1.5.dp else 1.dp,
                        color = animBorder,
                        shape = shape
                    )
                    .desktopHoverable(enabled = !isActive)
                    .desktopHandCursor()
                    .clickable { onDaySelected(item.name) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayLabel,
                    fontSize = if (isCompact) 12.sp else 13.sp,
                    fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = animText,
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
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
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

            val targetBg = when {
                isActive -> accentColor
                isToday -> if (isDark) accentColor.copy(alpha = 0.22f) else accentColor.copy(alpha = 0.12f)
                else -> ext.surfaceCard
            }
            val targetBorder = when {
                isActive || isToday -> accentColor
                else -> ext.borderSubtle
            }
            val targetText = when {
                isActive -> MaterialTheme.colorScheme.onPrimary
                isToday -> accentColor
                else -> ext.textSecondary
            }

            val animBg by animateColorAsState(
                targetValue = targetBg,
                animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                label = "day_idx_bg"
            )
            val animBorder by animateColorAsState(
                targetValue = targetBorder,
                animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                label = "day_idx_border"
            )
            val animText by animateColorAsState(
                targetValue = targetText,
                animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                label = "day_idx_text"
            )

            val shape = RoundedCornerShape(12.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .then(
                        if (isActive) {
                            Modifier.shadow(
                                elevation = 4.dp,
                                shape = shape,
                                ambientColor = accentColor.copy(alpha = 0.35f),
                                spotColor = accentColor.copy(alpha = 0.45f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(shape)
                    .background(animBg, shape)
                    .border(
                        width = if (isToday && !isActive) 1.5.dp else 1.dp,
                        color = animBorder,
                        shape = shape
                    )
                    .desktopHoverable(enabled = !isActive)
                    .desktopHandCursor()
                    .clickable { onDaySelected(index, day) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = day,
                    fontSize = 13.sp,
                    fontWeight = if (isActive || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = animText
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
    val ext = LocalExtendedColors.current
    val accentColor = MaterialTheme.colorScheme.primary
    val cardBg = ext.surfaceCard
    val borderSubtle = ext.borderSubtle
    val textPrimary = ext.textPrimary
    val textMuted = ext.textMuted

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
