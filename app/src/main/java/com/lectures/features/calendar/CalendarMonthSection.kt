package com.lectures.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.rememberLecturesWindow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
internal fun CalendarMonthSection(
    uiState: CalendarUiState,
    workload: DayWorkload,
    viewModel: CalendarViewModel
) {
    CalendarMonthGridCard(uiState = uiState, viewModel = viewModel)
    Spacer(modifier = Modifier.height(14.dp))
    SelectedDayHorizonCard(uiState = uiState, workload = workload, viewModel = viewModel)
}

@Composable
internal fun CalendarMonthGridCard(
    uiState: CalendarUiState,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    val windowInfo = rememberLecturesWindow()
    val useCompactDesktopCells = windowInfo.isTablet

    Column(modifier = modifier.fillMaxWidth()) {
        // Month Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.previousMonth() },
                modifier = Modifier.desktopHandCursor().testTag("calendar_prev_month_btn")
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.yearMonthText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = uiState.academicContext.phase.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { viewModel.nextMonth() },
                modifier = Modifier.desktopHandCursor().testTag("calendar_next_month_btn")
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Days of week header
        val dayNames = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            dayNames.forEach { name ->
                Text(
                    text = name,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val taskDaysWithDue = remember(uiState.assignments, uiState.currentMonth, uiState.currentYear) {
            val days = mutableSetOf<Int>()
            for (day in 1..uiState.daysInMonth) {
                if (uiState.assignments.any { !it.completed && viewModel.isTaskDueOnDay(it, day) }) {
                    days.add(day)
                }
            }
            days
        }

        val classDaysWithSchedule = remember(uiState.entries) {
            uiState.entries.map { it.dayOfWeek.lowercase(Locale.ROOT) }.toSet()
        }

        val activityDaysWithSchedule = remember(uiState.customEvents) {
            uiState.customEvents.map { it.dayOfWeek.lowercase(Locale.ROOT) }.toSet()
        }

        val cellHeightDp = when {
            windowInfo.isWideWorkspace -> 54.dp
            useCompactDesktopCells -> 50.dp
            else -> 0.dp
        }

        // Calendar Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            var currentDay = 1
            for (row in 0..5) {
                if (currentDay > uiState.daysInMonth) break
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        if (cellIndex < uiState.firstDayOfWeekOffset || currentDay > uiState.daysInMonth) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .let { if (useCompactDesktopCells) it.height(cellHeightDp) else it.aspectRatio(1f) }
                            )
                        } else {
                            val dayNum = currentDay
                            val isSelected = dayNum == uiState.selectedDay
                            val isToday = viewModel.isToday(dayNum)
                            val cellDayName = viewModel.getDayOfWeekNameForDay(dayNum)
                            val cellDayLower = cellDayName.lowercase(Locale.ROOT)

                            val cellAcademic = uiState.monthAcademicContexts[dayNum]
                                ?: AcademicCalendarEngine.getAcademicContextForDate(
                                    uiState.currentYear,
                                    uiState.currentMonth,
                                    dayNum
                                )

                            val hasClassesThisDay = cellAcademic.isInstructionalDay && (cellDayLower in classDaysWithSchedule)
                            val hasActivitiesThisDay = cellDayLower in activityDaysWithSchedule
                            val hasDeadlinesThisDay = dayNum in taskDaysWithDue
                            val hasMilestoneThisDay = cellAcademic.primaryMilestone != null

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .let { if (useCompactDesktopCells) it.height(cellHeightDp) else it.aspectRatio(1f) }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    )
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(10.dp)
                                            )
                                        } else Modifier
                                    )
                                    .desktopHoverable(
                                        enabled = !isSelected,
                                        hoverContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                    )
                                    .clickable { viewModel.selectDay(dayNum) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (hasClassesThisDay || hasActivitiesThisDay || hasDeadlinesThisDay || hasMilestoneThisDay) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val selectedDotColor = MaterialTheme.colorScheme.onPrimary
                                            if (hasDeadlinesThisDay) {
                                                CalendarDot(color = if (isSelected) selectedDotColor else Color(0xFFEF4444))
                                            }
                                            if (hasClassesThisDay) {
                                                CalendarDot(color = if (isSelected) selectedDotColor else MaterialTheme.colorScheme.primary)
                                            }
                                            if (hasActivitiesThisDay) {
                                                CalendarDot(color = if (isSelected) selectedDotColor else Color(0xFF10B981))
                                            }
                                            if (hasMilestoneThisDay && !hasClassesThisDay && !hasDeadlinesThisDay) {
                                                CalendarDot(color = if (isSelected) selectedDotColor else Color(0xFF8B5CF6))
                                            }
                                        }
                                    }
                                }
                            }
                            currentDay++
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SelectedDayHorizonCard(
    uiState: CalendarUiState,
    workload: DayWorkload,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp
) {
    // Selected Day Academic Horizon Card
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val monthName = remember(uiState.currentMonth) {
                        SimpleDateFormat("MMM", Locale.getDefault()).format(
                            Calendar.getInstance().apply { set(Calendar.MONTH, uiState.currentMonth) }.time
                        )
                    }
                    Text(
                        text = "${uiState.selectedDayOfWeek}, $monthName ${uiState.selectedDay}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${uiState.academicContext.semesterLabel} • ${uiState.academicContext.weekLabel}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.previousDay() }) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.nextDay() }) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Next Day",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            uiState.academicContext.primaryMilestone?.let { milestone ->
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (milestone.isHoliday) {
                        Color(0xFFEF4444).copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = if (milestone.isHoliday) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = milestone.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (milestone.isHoliday) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = milestone.description,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkloadPill(
                    label = "Classes",
                    value = if (workload.lectureCount > 0) "${workload.lectureCount} (${workload.totalHoursText})" else "None",
                    icon = Icons.Outlined.School,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                WorkloadPill(
                    label = "Deadlines",
                    value = if (workload.deadlineCount > 0) "${workload.deadlineCount} Due" else "None",
                    icon = Icons.Outlined.TaskAlt,
                    color = if (workload.deadlineCount > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                WorkloadPill(
                    label = "Activities",
                    value = if (workload.activityCount > 0) "${workload.activityCount}" else "0",
                    icon = Icons.Outlined.Schedule,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun WorkloadPill(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarDot(color: Color) {
    Box(
        modifier = Modifier
            .size(4.dp)
            .background(color, CircleShape)
    )
}
