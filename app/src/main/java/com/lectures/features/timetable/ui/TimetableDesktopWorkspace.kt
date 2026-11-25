package com.lectures.features.timetable.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.domain.ActivityCategory
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableConflictDetector
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

private data class PositionedSessionBlock(
    val entry: TimetableEntry,
    val startMin: Int,
    val endMin: Int,
    val laneIndex: Int,
    val totalLanes: Int,
    val hasConflict: Boolean
)

@Composable
fun TimetableDesktopWorkspace(
    programme: String,
    enrolledProgramme: String,
    isPeeking: Boolean,
    entries: List<TimetableEntry>,
    customEvents: List<CustomEvent>,
    notes: Map<String, LectureNote>,
    selectedDay: String,
    todayName: String,
    daysList: List<String>,
    dayInfoList: List<Triple<String, Int, String>>,
    dayEntries: List<TimetableEntry>,
    dayCustomEvents: List<CustomEvent>,
    globalNextUp: GlobalNextUp?,
    selectedFilter: String,
    isLoading: Boolean,
    selectedEntryForDetail: TimetableEntry?,
    selectedActivityForDetail: CustomEvent?,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    onDaySelected: (String) -> Unit,
    onCycleFilter: () -> Unit,
    onInspectEntry: (String) -> Unit,
    onDismissInspectEntry: () -> Unit,
    onOpenAlarmPicker: (String) -> Unit,
    onSaveNote: (TimetableEntry, String, Int?) -> Unit,
    onInspectActivity: (Long) -> Unit,
    onDismissInspectActivity: () -> Unit,
    onEditActivity: (Long) -> Unit,
    onDeleteActivity: (Long) -> Unit,
    onOpenQuickAdd: () -> Unit,
    onResetToEnrolled: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val windowInfo = rememberLecturesWindow()
    val ext = LocalExtendedColors.current
    val isDark = LocalAppTheme.current.isDark
    val reducedMotion = rememberPrefersReducedMotion()
    val accent = MaterialTheme.colorScheme.primary

    var viewMode by rememberSaveable(windowInfo.isWideWorkspace) {
        mutableStateOf(if (windowInfo.isWideWorkspace) "Week" else "Day")
    }

    val filteredEntriesAllDays = remember(entries, selectedFilter) {
        when (selectedFilter) {
            "Lectures" -> entries.filter {
                val label = resolveSessionTypeLabel(it)
                label == "Lecture" || label == "Clinical"
            }
            "Labs" -> entries.filter {
                val label = resolveSessionTypeLabel(it)
                label == "Lab" || label == "Practical"
            }
            "Associations" -> entries.filter { resolveSessionTypeLabel(it) == "Student Association" }
            else -> entries
        }
    }

    val selectedDayFormatted = remember(dayInfoList, selectedDay) {
        dayInfoList.firstOrNull { it.first.equals(selectedDay, ignoreCase = true) }?.third ?: selectedDay
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topContentPadding, start = 18.dp, end = 18.dp)
    ) {
        if (isPeeking) {
            TimetablePeekBanner(
                programme = programme,
                enrolledProgramme = enrolledProgramme,
                isDark = isDark,
                onResetToEnrolled = onResetToEnrolled
            )
        }

        if (programme.isBlank()) {
            NoProgrammePromptCard(isDark = isDark, onSelectClick = onSettingsClick)
        }

        // Compact Desktop Action & Live Status Toolbar (single horizontal band so schedule fits above the fold)
        DesktopTimetableToolbar(
            globalNextUp = globalNextUp,
            notes = notes,
            selectedDay = selectedDay,
            todayName = todayName,
            daysList = daysList,
            viewMode = viewMode,
            selectedFilter = selectedFilter,
            onViewModeChange = { viewMode = it },
            onDaySelected = onDaySelected,
            onCycleFilter = onCycleFilter,
            onInspectNextUp = { globalNextUp?.entry?.naturalKey?.let(onInspectEntry) },
            onOpenAlarmNextUp = { globalNextUp?.entry?.naturalKey?.let(onOpenAlarmPicker) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // LEFT / MAIN SCHEDULE PANE
            AnimatedContent(
                targetState = if (isLoading) "Loading" else viewMode,
                transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                contentKey = { it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = "desktop_timetable_view_switch"
            ) { currentMode ->
                when (currentMode) {
                    "Loading" -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = accent)
                        }
                    }
                    "Week" -> {
                        WeeklyTimetableMatrix(
                            entries = filteredEntriesAllDays,
                            customEvents = if (selectedFilter == "Lectures" || selectedFilter == "Labs") emptyList() else customEvents,
                            notes = notes,
                            selectedDay = selectedDay,
                            todayName = todayName,
                            selectedEntryKey = selectedEntryForDetail?.naturalKey,
                            bottomPadding = bottomContentPadding,
                            onSelectDay = onDaySelected,
                            onSelectEntry = { entry ->
                                onDaySelected(entry.dayOfWeek)
                                onInspectEntry(entry.naturalKey)
                            },
                            onSelectActivity = { ev ->
                                onDaySelected(ev.dayOfWeek)
                                onInspectActivity(ev.id)
                            }
                        )
                    }
                    else -> {
                        DesktopDayTimelinePane(
                            programme = programme,
                            selectedDay = selectedDay,
                            todayName = todayName,
                            daysList = daysList,
                            formattedSelectedDate = selectedDayFormatted,
                            dayEntries = dayEntries,
                            dayCustomEvents = dayCustomEvents,
                            notes = notes,
                            selectedEntryKey = selectedEntryForDetail?.naturalKey,
                            bottomPadding = bottomContentPadding,
                            onDaySelected = onDaySelected,
                            onInspectEntry = onInspectEntry,
                            onOpenAlarmPicker = onOpenAlarmPicker,
                            onInspectActivity = onInspectActivity,
                            onEditActivity = onEditActivity,
                            onDeleteActivity = onDeleteActivity,
                            onOpenQuickAdd = onOpenQuickAdd
                        )
                    }
                }
            }

            // RIGHT DOCKED INSPECTOR PANE
            val inspectorWidth = if (windowInfo.isExtraLargeWidth) 380.dp else 336.dp
            val inspectorStateKey = when {
                selectedEntryForDetail != null -> "entry_${selectedEntryForDetail.naturalKey}"
                selectedActivityForDetail != null -> "activity_${selectedActivityForDetail.id}"
                else -> "overview"
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ext.surfaceCard,
                border = BorderStroke(1.dp, ext.borderSubtle),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .width(inspectorWidth)
                    .fillMaxHeight()
                    .padding(bottom = 14.dp)
                    .testTag("timetable_desktop_inspector")
            ) {
                AnimatedContent(
                    targetState = inspectorStateKey,
                    transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                    contentKey = { it },
                    modifier = Modifier.fillMaxSize(),
                    label = "desktop_timetable_inspector_switch"
                ) { _ ->
                    if (selectedEntryForDetail != null) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            InspectorHeader(
                                title = "Class Inspector",
                                subtitle = selectedEntryForDetail.courseCode,
                                onClose = onDismissInspectEntry
                            )
                            HorizontalDivider(color = ext.borderSubtle)
                            LectureDetailContent(
                                entry = selectedEntryForDetail,
                                initialNote = notes[selectedEntryForDetail.naturalKey]?.content ?: "",
                                initialReminder = notes[selectedEntryForDetail.naturalKey]?.alarmMinutes,
                                onSaveNote = { text, rem ->
                                    onSaveNote(selectedEntryForDetail, text, rem)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                                    .padding(18.dp)
                            )
                        }
                    } else if (selectedActivityForDetail != null) {
                        DockedActivityInspector(
                            event = selectedActivityForDetail,
                            onClose = onDismissInspectActivity,
                            onEdit = { onEditActivity(selectedActivityForDetail.id) },
                            onDelete = { onDeleteActivity(selectedActivityForDetail.id) }
                        )
                    } else {
                        TimetableOverviewInspector(
                            selectedDay = selectedDay,
                            formattedSelectedDate = selectedDayFormatted,
                            dayEntries = dayEntries,
                            dayCustomEvents = dayCustomEvents,
                            notes = notes,
                            globalNextUp = globalNextUp,
                            onInspectEntry = onInspectEntry,
                            onOpenQuickAdd = onOpenQuickAdd
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopTimetableToolbar(
    globalNextUp: GlobalNextUp?,
    notes: Map<String, LectureNote>,
    selectedDay: String,
    todayName: String,
    daysList: List<String>,
    viewMode: String,
    selectedFilter: String,
    onViewModeChange: (String) -> Unit,
    onDaySelected: (String) -> Unit,
    onCycleFilter: () -> Unit,
    onInspectNextUp: () -> Unit,
    onOpenAlarmNextUp: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val accent = MaterialTheme.colorScheme.primary
    val filterLabel = if (selectedFilter == "All") "All Sessions" else selectedFilter

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = ext.surfaceCard,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Live / Next-Up Pill Strip
            if (globalNextUp != null) {
                val e = globalNextUp.entry
                val hasAlarm = notes[e.naturalKey]?.alarmMinutes != null
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent.copy(alpha = 0.08f))
                        .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .desktopHoverable(hoverContainerColor = accent.copy(alpha = 0.14f))
                        .clickable { onInspectNextUp() }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (globalNextUp.isLive) StatusGreenLive else accent
                    ) {
                        Text(
                            text = if (globalNextUp.isLive) "LIVE" else "NEXT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = contrastOnColor(if (globalNextUp.isLive) StatusGreenLive else accent),
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${e.courseCode} · ${e.courseTitle}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = ext.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val roomPart = e.room?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                        val timing = if (globalNextUp.isLive) {
                            "${globalNextUp.progress?.remainingMinutes ?: 0}m left$roomPart"
                        } else {
                            "${globalNextUp.dayLabel} ${e.startTime}$roomPart"
                        }
                        Text(
                            text = timing,
                            fontSize = 11.5.sp,
                            color = ext.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onOpenAlarmNextUp,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(
                            imageVector = if (hasAlarm) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                            contentDescription = "Set alarm for next class",
                            tint = if (hasAlarm) accent else ext.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "Weekly & Daily Schedule",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ext.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Day Prev / Today / Next Stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val curIdx = daysList.indexOfFirst { it.equals(selectedDay, ignoreCase = true) }.coerceAtLeast(0)
                IconButton(
                    onClick = {
                        val prevIdx = if (curIdx - 1 < 0) daysList.lastIndex else curIdx - 1
                        onDaySelected(daysList[prevIdx])
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .desktopHandCursor()
                        .testTag("timetable_prev_day_btn")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Previous Day", tint = ext.textPrimary)
                }

                FilterChip(
                    selected = selectedDay.equals(todayName, ignoreCase = true),
                    onClick = { onDaySelected(todayName) },
                    label = { Text("Today", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.desktopHandCursor().testTag("timetable_today_btn")
                )

                IconButton(
                    onClick = { onDaySelected(daysList[(curIdx + 1) % daysList.size]) },
                    modifier = Modifier
                        .size(48.dp)
                        .desktopHandCursor()
                        .testTag("timetable_next_day_btn")
                ) {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Next Day", tint = ext.textPrimary)
                }
            }

            // Filter Cycle Pill
            Surface(
                onClick = onCycleFilter,
                shape = RoundedCornerShape(12.dp),
                color = ext.surfaceElevated,
                border = BorderStroke(1.dp, ext.borderSubtle),
                modifier = Modifier.desktopHandCursor()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = "Filter", tint = accent, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(filterLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = accent)
                }
            }

            // Week Grid / Day Focus Switch
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ext.surfaceElevated,
                border = BorderStroke(1.dp, ext.borderSubtle)
            ) {
                Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf("Week" to "Week Grid", "Day" to "Day List").forEach { (key, label) ->
                        val active = viewMode == key
                        val pillBg by animateColorAsState(
                            targetValue = if (active) accent else Color.Transparent,
                            animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                            label = "view_switch_bg"
                        )
                        val pillText by animateColorAsState(
                            targetValue = if (active) contrastOnColor(accent) else ext.textSecondary,
                            animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                            label = "view_switch_text"
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(pillBg)
                                .desktopHoverable(enabled = !active, hoverContainerColor = accent.copy(alpha = 0.08f))
                                .clickable { onViewModeChange(key) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("timetable_view_${key.lowercase(Locale.ROOT)}_btn")
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = pillText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyTimetableMatrix(
    entries: List<TimetableEntry>,
    customEvents: List<CustomEvent>,
    notes: Map<String, LectureNote>,
    selectedDay: String,
    todayName: String,
    selectedEntryKey: String?,
    bottomPadding: Dp,
    onSelectDay: (String) -> Unit,
    onSelectEntry: (TimetableEntry) -> Unit,
    onSelectActivity: (CustomEvent) -> Unit
) {
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val accent = MaterialTheme.colorScheme.primary
    val hasSundayItems = remember(entries, customEvents) {
        entries.any { it.dayOfWeek.equals("Sunday", ignoreCase = true) } ||
            customEvents.any { it.dayOfWeek.equals("Sunday", ignoreCase = true) }
    }
    val visibleDays = remember(hasSundayItems) {
        if (hasSundayItems) TimeUtil.DAYS else TimeUtil.DAYS.take(6)
    }

    val hourHeightDp = 58.dp
    val (startHour, endHour) = remember(entries, customEvents) {
        val allStarts = entries.map { TimeUtil.toMinutes(it.startTime) } + customEvents.map { TimeUtil.toMinutes(it.startTime) }
        val allEnds = entries.map { TimeUtil.toMinutes(it.endTime) } + customEvents.map { TimeUtil.toMinutes(it.endTime) }
        val minMin = allStarts.filter { it > 0 }.minOrNull() ?: (8 * 60)
        val maxMin = allEnds.filter { it > 0 }.maxOrNull() ?: (18 * 60)
        val s = (minMin / 60).coerceIn(6, 8)
        val e = ((maxMin + 59) / 60).coerceIn(17, 22)
        s to max(e, s + 8)
    }

    val positionedByDay = remember(entries, visibleDays) {
        visibleDays.associateWith { day ->
            val dayList = entries
                .filter { it.dayOfWeek.equals(day, ignoreCase = true) }
                .sortedBy { TimeUtil.toMinutes(it.startTime) }
            layoutOverlappingSessions(dayList)
        }
    }

    val verticalScroll = rememberScrollState()
    val density = LocalDensity.current
    LaunchedEffect(startHour, endHour) {
        val nowCal = Calendar.getInstance()
        val curHour = nowCal.get(Calendar.HOUR_OF_DAY)
        val targetHour = (curHour - 1).coerceIn(startHour, max(startHour, endHour - 4))
        val offsetPx = with(density) { (hourHeightDp * (targetHour - startHour)).toPx().toInt() }
        if (offsetPx > 0) {
            if (reducedMotion) verticalScroll.scrollTo(offsetPx)
            else verticalScroll.animateScrollTo(offsetPx)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ext.surfaceCard,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 14.dp)
            .testTag("timetable_weekly_grid")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Day Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ext.surfaceElevated.copy(alpha = 0.65f))
                    .padding(start = 56.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            ) {
                visibleDays.forEach { day ->
                    val isToday = day.equals(todayName, ignoreCase = true)
                    val isSelected = day.equals(selectedDay, ignoreCase = true)
                    val count = positionedByDay[day]?.size ?: 0
                    val headerBg by animateColorAsState(
                        targetValue = when {
                            isSelected -> accent
                            isToday -> accent.copy(alpha = 0.12f)
                            else -> Color.Transparent
                        },
                        animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                        label = "week_header_bg"
                    )
                    val headerTextColor by animateColorAsState(
                        targetValue = when {
                            isSelected -> contrastOnColor(accent)
                            isToday -> accent
                            else -> ext.textPrimary
                        },
                        animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                        label = "week_header_text"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(headerBg)
                            .desktopHoverable(enabled = !isSelected, hoverContainerColor = accent.copy(alpha = 0.08f))
                            .clickable { onSelectDay(day) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = day.take(3),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = headerTextColor
                            )
                            if (count > 0) {
                                Text(
                                    text = "($count)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isSelected) contrastOnColor(accent).copy(alpha = 0.85f) else ext.textMuted
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = ext.borderSubtle)

            // Scrollable Proportional Grid Body
            val totalHeight = hourHeightDp * (endHour - startHour)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll)
                    .padding(bottom = minOf(bottomPadding, 48.dp))
            ) {
                // Time Gutter
                Column(modifier = Modifier.width(56.dp)) {
                    for (h in startHour until endHour) {
                        Box(
                            modifier = Modifier
                                .height(hourHeightDp)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Text(
                                text = String.format(Locale.US, "%02d:00", h),
                                fontSize = 10.5.sp,
                                color = ext.textMuted,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Columns Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(totalHeight)
                        .padding(end = 8.dp)
                ) {
                    // Horizontal hour dividers
                    Column(modifier = Modifier.fillMaxSize()) {
                        for (h in startHour until endHour) {
                            HorizontalDivider(color = ext.borderSubtle.copy(alpha = 0.55f))
                            Spacer(modifier = Modifier.height(hourHeightDp - 1.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxSize()) {
                        visibleDays.forEach { day ->
                            val isToday = day.equals(todayName, ignoreCase = true)
                            val dayBlocks = positionedByDay[day].orEmpty()
                            val dayCustom = customEvents.filter { it.dayOfWeek.equals(day, ignoreCase = true) }

                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(if (isToday) accent.copy(alpha = 0.03f) else Color.Transparent)
                                    .border(0.5.dp, ext.borderSubtle.copy(alpha = 0.35f))
                            ) {
                                val colWidth = maxWidth

                                // Current time line on Today column
                                if (isToday) {
                                    val nowCal = Calendar.getInstance()
                                    val nowMin = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
                                    if (nowMin in (startHour * 60)..(endHour * 60)) {
                                        val offsetY = hourHeightDp * ((nowMin - startHour * 60) / 60f)
                                        Box(
                                            modifier = Modifier
                                                .offset(y = offsetY)
                                                .fillMaxWidth()
                                                .height(2.dp)
                                                .background(accent)
                                        )
                                    }
                                }

                                dayBlocks.forEach { block ->
                                    val clampedStart = max(block.startMin, startHour * 60)
                                    val clampedEnd = max(clampedStart + 30, min(block.endMin, endHour * 60))
                                    val topDp = hourHeightDp * ((clampedStart - startHour * 60) / 60f)
                                    val heightDp = max(36.dp, hourHeightDp * ((clampedEnd - clampedStart) / 60f) - 2.dp)
                                    val laneWidth = colWidth / block.totalLanes
                                    val leftDp = laneWidth * block.laneIndex
                                    val isSelected = block.entry.naturalKey == selectedEntryKey
                                    val style = sessionStyle(block.entry.sessionType, block.entry.room ?: "", block.entry.courseTitle)
                                    val hasAlarm = notes[block.entry.naturalKey]?.alarmMinutes != null
                                    val blockContainerColor by animateColorAsState(
                                        targetValue = if (isSelected) style.accentBar.copy(alpha = 0.22f) else style.badgeBg.copy(alpha = 0.13f),
                                        animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                                        label = "week_block_bg"
                                    )
                                    val blockBorderColor by animateColorAsState(
                                        targetValue = when {
                                            block.hasConflict -> MaterialTheme.colorScheme.error
                                            isSelected -> accent
                                            else -> style.accentBar.copy(alpha = 0.45f)
                                        },
                                        animationSpec = MotionSpec.stateTween(MotionSpec.STATE_COLOR_MS, reducedMotion),
                                        label = "week_block_border"
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = blockContainerColor,
                                        border = BorderStroke(
                                            width = if (isSelected || block.hasConflict) 2.dp else 1.dp,
                                            color = blockBorderColor
                                        ),
                                        modifier = Modifier
                                            .offset(x = leftDp + 2.dp, y = topDp + 1.dp)
                                            .width(laneWidth - 4.dp)
                                            .height(heightDp)
                                            .desktopHoverable(hoverContainerColor = style.accentBar.copy(alpha = 0.2f))
                                            .clickable { onSelectEntry(block.entry) }
                                            .testTag("week_block_${block.entry.courseCode}")
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = block.entry.courseCode,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = style.accentBar,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                if (hasAlarm) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.NotificationsActive,
                                                        contentDescription = null,
                                                        tint = accent,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = block.entry.courseTitle,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = ext.textPrimary,
                                                maxLines = if (heightDp >= 64.dp) 2 else 1,
                                                overflow = TextOverflow.Ellipsis,
                                                lineHeight = 13.sp
                                            )
                                            if (heightDp >= 54.dp && !block.entry.room.isNullOrBlank()) {
                                                Text(
                                                    text = block.entry.room.orEmpty(),
                                                    fontSize = 9.5.sp,
                                                    color = ext.textMuted,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                dayCustom.forEach { ev ->
                                    val sMin = TimeUtil.toMinutes(ev.startTime)
                                    val eMin = TimeUtil.toMinutes(ev.endTime)
                                    if (eMin > sMin && sMin > 0) {
                                        val clampedStart = max(sMin, startHour * 60)
                                        val clampedEnd = max(clampedStart + 30, min(eMin, endHour * 60))
                                        val topDp = hourHeightDp * ((clampedStart - startHour * 60) / 60f)
                                        val heightDp = max(34.dp, hourHeightDp * ((clampedEnd - clampedStart) / 60f) - 2.dp)
                                        val catColor = ActivityCategory.colorForHex(ev.colorTag.ifBlank { ActivityCategory.fromName(ev.category).colorHex })
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = catColor.copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, catColor.copy(alpha = 0.55f)),
                                            modifier = Modifier
                                                .offset(x = 2.dp, y = topDp + 1.dp)
                                                .width(colWidth - 4.dp)
                                                .height(heightDp)
                                                .desktopHoverable(hoverContainerColor = catColor.copy(alpha = 0.22f))
                                                .clickable { onSelectActivity(ev) }
                                        ) {
                                            Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
                                                Text(
                                                    text = ev.title,
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catColor,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${ev.startTime}–${ev.endTime}",
                                                    fontSize = 9.5.sp,
                                                    color = ext.textMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun max(a: Dp, b: Dp): Dp = if (a > b) a else b

private fun layoutOverlappingSessions(dayEntries: List<TimetableEntry>): List<PositionedSessionBlock> {
    if (dayEntries.isEmpty()) return emptyList()
    val raw = dayEntries.map { entry ->
        val s = TimeUtil.toMinutes(entry.startTime).let { if (it <= 0) 8 * 60 else it }
        val e = TimeUtil.toMinutes(entry.endTime).let { if (it <= s) s + 60 else it }
        Triple(entry, s, e)
    }

    // Group connected overlapping intervals
    val clusters = mutableListOf<MutableList<Triple<TimetableEntry, Int, Int>>>()
    var currentCluster = mutableListOf<Triple<TimetableEntry, Int, Int>>()
    var clusterEnd = -1
    for (item in raw) {
        if (currentCluster.isEmpty() || item.second < clusterEnd) {
            currentCluster.add(item)
            clusterEnd = max(clusterEnd, item.third)
        } else {
            clusters.add(currentCluster)
            currentCluster = mutableListOf(item)
            clusterEnd = item.third
        }
    }
    if (currentCluster.isNotEmpty()) clusters.add(currentCluster)

    val result = mutableListOf<PositionedSessionBlock>()
    for (cluster in clusters) {
        val laneEndTimes = mutableListOf<Int>()
        val assigned = cluster.map { item ->
            var lane = laneEndTimes.indexOfFirst { end -> end <= item.second }
            if (lane == -1) {
                lane = laneEndTimes.size
                laneEndTimes.add(item.third)
            } else {
                laneEndTimes[lane] = item.third
            }
            item to lane
        }
        val totalLanes = max(1, laneEndTimes.size)
        assigned.forEach { (item, lane) ->
            result.add(
                PositionedSessionBlock(
                    entry = item.first,
                    startMin = item.second,
                    endMin = item.third,
                    laneIndex = lane,
                    totalLanes = totalLanes,
                    hasConflict = totalLanes > 1 || TimetableConflictDetector.hasConflict(item.first, dayEntries)
                )
            )
        }
    }
    return result
}

@Composable
private fun DesktopDayTimelinePane(
    programme: String,
    selectedDay: String,
    todayName: String,
    daysList: List<String>,
    formattedSelectedDate: String,
    dayEntries: List<TimetableEntry>,
    dayCustomEvents: List<CustomEvent>,
    notes: Map<String, LectureNote>,
    selectedEntryKey: String?,
    bottomPadding: Dp,
    onDaySelected: (String) -> Unit,
    onInspectEntry: (String) -> Unit,
    onOpenAlarmPicker: (String) -> Unit,
    onInspectActivity: (Long) -> Unit,
    onEditActivity: (Long) -> Unit,
    onDeleteActivity: (Long) -> Unit,
    onOpenQuickAdd: () -> Unit
) {
    val isDark = LocalAppTheme.current.isDark
    val isViewingToday = selectedDay.equals(todayName, ignoreCase = true)
    val specDays = remember(daysList, selectedDay, todayName) {
        daysList.map { d -> SpecDayItem(d, d.equals(todayName, ignoreCase = true), d.equals(selectedDay, ignoreCase = true)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding)
    ) {
        item(key = "desktop_day_strip") {
            DayCapsuleStrip(
                dayItems = specDays,
                onDaySelected = onDaySelected,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        item(key = "desktop_day_title") {
            Text(
                text = "$selectedDay · $formattedSelectedDate",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = LocalExtendedColors.current.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }
        if (dayEntries.isEmpty() && dayCustomEvents.isEmpty()) {
            item(key = "desktop_day_empty") {
                EmptyDayScheduleCard(
                    selectedDay = selectedDay,
                    isDark = isDark,
                    onAddCustomActivityClick = onOpenQuickAdd
                )
            }
        } else {
            items(dayEntries, key = { it.naturalKey.ifBlank { "${it.courseCode}_${it.startTime}" } }) { entry ->
                val spec = entry.toSpecClassSession(
                    currentProgramme = programme,
                    hasConflict = TimetableConflictDetector.hasConflict(entry, dayEntries),
                    note = notes[entry.naturalKey],
                    progress = if (isViewingToday) TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek) else null
                )
                val isSelected = entry.naturalKey == selectedEntryKey
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .then(
                            if (isSelected) {
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                            } else Modifier
                        )
                ) {
                    TimelineClassCard(
                        session = spec,
                        onToggleAlarm = { onOpenAlarmPicker(entry.naturalKey) },
                        onAttachedNoteClick = { onInspectEntry(entry.naturalKey) },
                        onClick = { onInspectEntry(entry.naturalKey) }
                    )
                }
            }
            items(dayCustomEvents, key = { "desktop_custom_${it.id}" }) { ev ->
                PaddingBox(top = 4.dp, bottom = 6.dp) {
                    ActivityCard(
                        event = ev,
                        onClick = { onInspectActivity(ev.id) },
                        onEdit = { onEditActivity(ev.id) },
                        onDelete = { onDeleteActivity(ev.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InspectorHeader(
    title: String,
    subtitle: String,
    onClose: () -> Unit
) {
    val ext = LocalExtendedColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ext.textPrimary)
            Text(text = subtitle, fontSize = 12.sp, color = ext.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .desktopHandCursor()
                .testTag("inspector_close_btn")
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Close Inspector", tint = ext.textMuted)
        }
    }
}

@Composable
private fun DockedActivityInspector(
    event: CustomEvent,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val accent = MaterialTheme.colorScheme.primary
    Column(modifier = Modifier.fillMaxSize()) {
        InspectorHeader(title = "Activity Inspector", subtitle = event.category, onClose = onClose)
        HorizontalDivider(color = ext.borderSubtle)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(event.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = ext.textPrimary)
            Text("${event.dayOfWeek} · ${event.startTime} – ${event.endTime}", fontSize = 13.sp, color = accent, fontWeight = FontWeight.SemiBold)
            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.MeetingRoom, contentDescription = null, tint = ext.textMuted, modifier = Modifier.size(16.dp))
                    Text(event.location, fontSize = 13.sp, color = ext.textPrimary)
                    RoomFloorBadge(roomName = event.location, compact = true)
                }
            }
            if (event.notes.isNotBlank()) {
                Surface(shape = RoundedCornerShape(12.dp), color = ext.surfaceElevated, modifier = Modifier.fillMaxWidth()) {
                    Text(event.notes, fontSize = 13.sp, color = ext.textSecondary, modifier = Modifier.padding(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f).desktopHandCursor()) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.desktopHandCursor()
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TimetableOverviewInspector(
    selectedDay: String,
    formattedSelectedDate: String,
    dayEntries: List<TimetableEntry>,
    dayCustomEvents: List<CustomEvent>,
    notes: Map<String, LectureNote>,
    globalNextUp: GlobalNextUp?,
    onInspectEntry: (String) -> Unit,
    onOpenQuickAdd: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val accent = MaterialTheme.colorScheme.primary
    val conflicts = remember(dayEntries) {
        dayEntries.count { TimetableConflictDetector.hasConflict(it, dayEntries) }
    }
    val notesCount = remember(dayEntries, notes) {
        dayEntries.count { !notes[it.naturalKey]?.content.isNullOrBlank() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column {
            Text(text = "$selectedDay Overview", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ext.textPrimary)
            Text(text = formattedSelectedDate, fontSize = 12.sp, color = ext.textMuted)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            InspectorMetricPill(label = "Classes", value = "${dayEntries.size}", modifier = Modifier.weight(1f))
            InspectorMetricPill(label = "Notes", value = "$notesCount", modifier = Modifier.weight(1f))
            InspectorMetricPill(label = "Activities", value = "${dayCustomEvents.size}", modifier = Modifier.weight(1f))
        }

        if (conflicts > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ext.dangerBg,
                border = BorderStroke(1.dp, ext.dangerBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ext.dangerIcon, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$conflicts overlapping sessions on $selectedDay", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = ext.dangerText)
                }
            }
        }

        if (dayEntries.isNotEmpty()) {
            Text("Quick Inspect", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = ext.textSecondary)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dayEntries.forEach { entry ->
                    val style = sessionStyle(entry.sessionType, entry.room ?: "", entry.courseTitle)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ext.surfaceElevated,
                        border = BorderStroke(1.dp, ext.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .desktopHoverable()
                            .clickable { onInspectEntry(entry.naturalKey) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(modifier = Modifier.width(4.dp).height(28.dp).clip(CircleShape).background(style.accentBar))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.courseCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
                                Text(entry.courseTitle, fontSize = 12.sp, color = ext.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${entry.startTime}–${entry.endTime}${entry.room?.let { " · $it" } ?: ""}", fontSize = 11.sp, color = ext.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = ext.textMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        if (globalNextUp != null) {
            OutlinedButton(
                onClick = { onInspectEntry(globalNextUp.entry.naturalKey) },
                modifier = Modifier.fillMaxWidth().desktopHandCursor()
            ) {
                Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Inspect Next: ${globalNextUp.entry.courseCode}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Button(
            onClick = onOpenQuickAdd,
            modifier = Modifier.fillMaxWidth().desktopHandCursor(),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = contrastOnColor(accent))
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Activity or Task", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "Tip: Click any class block to attach notes or alarms. Use Left/Right arrows to switch days, Ctrl+N to quick add, Esc to close inspector.",
            fontSize = 11.sp,
            color = ext.textMuted,
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun InspectorMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.surfaceElevated,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = ext.textPrimary)
            Text(label, fontSize = 10.5.sp, color = ext.textMuted)
        }
    }
}
