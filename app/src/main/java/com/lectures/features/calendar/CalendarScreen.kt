package com.lectures.features.calendar

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.features.tasks.TaskCard
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.ui.ActivityCard
import com.lectures.features.timetable.ui.LectureCard
import com.lectures.ui.Chrome
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow

internal sealed class CalendarTimelineItem(val startTime: String) {
    data class Lecture(val entry: TimetableEntry) : CalendarTimelineItem(entry.startTime)
    data class Activity(val event: CustomEvent) : CalendarTimelineItem(event.startTime)
}

@Composable
fun CalendarScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToTimetable: () -> Unit = {},
    onPeekProgramme: (String) -> Unit = {},
    onSetOfficialProgramme: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    if (repository == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val taskAlarmScheduler = remember(context) { TaskAlarmScheduler(context) }
    val viewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.provideFactory(repository, classAlarmScheduler, taskAlarmScheduler)
    )

    val uiState by viewModel.uiState.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearUserMessage()
        }
    }

    var showAddActivitySheet by rememberSaveable { mutableStateOf(false) }
    var editingActivityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    var deleteEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleteTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailActivityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailLectureKey by rememberSaveable { mutableStateOf<String?>(null) }

    val activityToEdit = remember(editingActivityId, uiState.customEvents) {
        editingActivityId?.let { id -> uiState.customEvents.firstOrNull { it.id == id } }
    }
    val eventToDelete = remember(deleteEventId, uiState.customEvents) {
        deleteEventId?.let { id -> uiState.customEvents.firstOrNull { it.id == id } }
    }
    val taskToDelete = remember(deleteTaskId, uiState.assignments) {
        deleteTaskId?.let { id -> uiState.assignments.firstOrNull { it.id == id } }
    }
    val selectedActivityForDetail = remember(detailActivityId, uiState.customEvents) {
        detailActivityId?.let { id -> uiState.customEvents.firstOrNull { it.id == id } }
    }
    val selectedLectureForDetail = remember(detailLectureKey, uiState.entries) {
        detailLectureKey?.let { key -> uiState.entries.firstOrNull { it.naturalKey == key } }
    }

    val isDark = LocalAppTheme.current.isDark

    val selectedDayLectures = remember(uiState.entries, uiState.selectedDayOfWeek, uiState.academicContext.isInstructionalDay) {
        if (uiState.academicContext.isInstructionalDay) {
            uiState.entries
                .filter { it.dayOfWeek.equals(uiState.selectedDayOfWeek, ignoreCase = true) }
                .sortedBy { it.startTime }
        } else {
            emptyList()
        }
    }

    val selectedDayActivities = remember(uiState.customEvents, uiState.selectedDayOfWeek) {
        uiState.customEvents
            .filter { it.dayOfWeek.equals(uiState.selectedDayOfWeek, ignoreCase = true) }
            .sortedBy { it.startTime }
    }

    val selectedDayDeadlines = remember(uiState.assignments, uiState.selectedDay, uiState.currentMonth, uiState.currentYear) {
        uiState.assignments.filter { assignment ->
            !assignment.completed && viewModel.isTaskDueOnDay(assignment, uiState.selectedDay)
        }
    }

    val workload = remember(
        uiState.selectedDay,
        uiState.selectedDayOfWeek,
        selectedDayLectures,
        selectedDayActivities,
        selectedDayDeadlines
    ) {
        viewModel.getDayWorkload(
            uiState.selectedDay,
            uiState.selectedDayOfWeek,
            selectedDayLectures,
            selectedDayActivities,
            selectedDayDeadlines
        )
    }

    val timelineItems = remember(
        selectedDayLectures,
        selectedDayActivities,
        uiState.selectedFilter
    ) {
        val list = mutableListOf<CalendarTimelineItem>()
        when (uiState.selectedFilter) {
            CalendarFilter.ALL -> {
                list.addAll(selectedDayLectures.map { CalendarTimelineItem.Lecture(it) })
                list.addAll(selectedDayActivities.map { CalendarTimelineItem.Activity(it) })
            }
            CalendarFilter.LECTURES -> {
                list.addAll(selectedDayLectures.map { CalendarTimelineItem.Lecture(it) })
            }
            CalendarFilter.ACTIVITIES -> {
                list.addAll(selectedDayActivities.map { CalendarTimelineItem.Activity(it) })
            }
            CalendarFilter.TASKS -> Unit
        }
        list.sortedBy { it.startTime }
    }

    val windowInfo = rememberLecturesWindow()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = Chrome.listBottomPadding(navBarBottom, windowInfo.useNavigationRail)
    val listState = rememberLazyListState()

    val hasOverlayOpen = showAddActivitySheet || editingActivityId != null || showProfileSheet ||
        detailActivityId != null || detailLectureKey != null || eventToDelete != null || taskToDelete != null

    Scaffold(
        modifier = Modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && !hasOverlayOpen) {
                val cmdOrCtrl = event.isCtrlPressed || event.isMetaPressed
                when {
                    cmdOrCtrl && event.key == Key.N -> {
                        showAddActivitySheet = true
                        true
                    }
                    event.key == Key.DirectionLeft && uiState.activeTabMode == CalendarTabMode.MONTH_PLANNER -> {
                        viewModel.previousDay()
                        true
                    }
                    event.key == Key.DirectionRight && uiState.activeTabMode == CalendarTabMode.MONTH_PLANNER -> {
                        viewModel.nextDay()
                        true
                    }
                    else -> false
                }
            } else false
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (uiState.activeTabMode == CalendarTabMode.MONTH_PLANNER) {
                if (windowInfo.collapseFab) {
                    FloatingActionButton(
                        onClick = { showAddActivitySheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                            .desktopHandCursor()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Schedule Activity")
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showAddActivitySheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = "Schedule Activity") },
                        text = { Text("Schedule Activity", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                            .desktopHandCursor()
                    )
                }
            }
        },
        topBar = {
            FloatingIslandTopBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.desktopHandCursor()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.Event,
                                    contentDescription = null,
                                    tint = contrastOnColor(MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                        Column {
                            Text(
                                "Academic Calendar",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
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
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = {
                                viewModel.setTabMode(CalendarTabMode.MONTH_PLANNER)
                                viewModel.selectToday()
                            },
                            label = { Text("Today", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Today,
                                    contentDescription = "Jump to Today",
                                    modifier = Modifier.size(15.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.desktopHandCursor()
                        )

                        IconButton(
                            onClick = { showProfileSheet = true },
                            modifier = Modifier
                                .desktopHandCursor()
                                .testTag("topbar_academic_info_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = "Academic Info",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        val ext = LocalExtendedColors.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            if (windowInfo.isDualPane && uiState.activeTabMode == CalendarTabMode.MONTH_PLANNER) {
                // Desktop / Expanded-tablet 2-pane split planner: Month Calendar LEFT + Day Agenda RIGHT
                Column(
                    modifier = Modifier
                        .widthIn(max = windowInfo.maxReadingWidth)
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding() + 8.dp)
                ) {
                    CalendarModeSwitchRow(
                        activeTabMode = uiState.activeTabMode,
                        onSelectTabMode = { viewModel.setTabMode(it) },
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // LEFT PANE: Month Grid + Color Legend
                        Column(
                            modifier = Modifier
                                .weight(1.02f)
                                .widthIn(max = 540.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = listBottomPadding)
                                .testTag("calendar_month_pane"),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, ext.borderSubtle)
                            ) {
                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                    CalendarMonthGridCard(
                                        uiState = uiState,
                                        viewModel = viewModel
                                    )
                                }
                            }

                            CalendarLegendCard(
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // RIGHT PANE: Selected Day Horizon + Filters + Deadlines & Timeline
                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                                .testTag("calendar_agenda_pane")
                        ) {
                            SelectedDayHorizonCard(
                                uiState = uiState,
                                workload = workload,
                                viewModel = viewModel,
                                horizontalPadding = 0.dp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            @OptIn(ExperimentalLayoutApi::class)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CalendarFilter.entries.forEach { filter ->
                                    val count = when (filter) {
                                        CalendarFilter.ALL -> selectedDayLectures.size + selectedDayActivities.size + selectedDayDeadlines.size
                                        CalendarFilter.LECTURES -> selectedDayLectures.size
                                        CalendarFilter.ACTIVITIES -> selectedDayActivities.size
                                        CalendarFilter.TASKS -> selectedDayDeadlines.size
                                    }
                                    FilterChip(
                                        selected = uiState.selectedFilter == filter,
                                        onClick = { viewModel.setFilter(filter) },
                                        label = { Text("${filter.label} ($count)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.desktopHandCursor()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(bottom = listBottomPadding + 88.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (selectedDayDeadlines.isNotEmpty() && (uiState.selectedFilter == CalendarFilter.ALL || uiState.selectedFilter == CalendarFilter.TASKS)) {
                                    item(key = "desktop_deadlines_header") {
                                        DeadlinesSectionHeader()
                                    }
                                    items(selectedDayDeadlines, key = { "deadline_${it.id}" }) { task ->
                                        Box(modifier = Modifier.animateItem()) {
                                            TaskCard(
                                                task = task,
                                                isDark = isDark,
                                                onToggleComplete = { viewModel.toggleTaskComplete(task) },
                                                onEdit = { },
                                                onDelete = { deleteTaskId = task.id }
                                            )
                                        }
                                    }
                                }

                                if (!uiState.academicContext.isInstructionalDay && (uiState.selectedFilter == CalendarFilter.ALL || uiState.selectedFilter == CalendarFilter.LECTURES)) {
                                    item(key = "desktop_non_instructional_notice") {
                                        NonInstructionalNoticeCard(
                                            phaseName = uiState.academicContext.phase.displayName
                                        )
                                    }
                                }

                                if (timelineItems.isEmpty() && (selectedDayDeadlines.isEmpty() || uiState.selectedFilter != CalendarFilter.ALL)) {
                                    item(key = "desktop_empty_day") {
                                        EmptyDayScheduleCard(selectedDayOfWeek = uiState.selectedDayOfWeek)
                                    }
                                } else {
                                    items(timelineItems, key = { item ->
                                        when (item) {
                                            is CalendarTimelineItem.Lecture -> "lec_${item.entry.naturalKey}"
                                            is CalendarTimelineItem.Activity -> "act_${item.event.id}"
                                        }
                                    }) { item ->
                                        Box(modifier = Modifier.animateItem()) {
                                            when (item) {
                                                is CalendarTimelineItem.Lecture -> {
                                                    LectureCard(
                                                        entry = item.entry,
                                                        onClick = { detailLectureKey = item.entry.naturalKey }
                                                    )
                                                }
                                                is CalendarTimelineItem.Activity -> {
                                                    ActivityCard(
                                                        event = item.event,
                                                        onClick = { detailActivityId = item.event.id },
                                                        onEdit = { editingActivityId = item.event.id },
                                                        onDelete = { deleteEventId = item.event.id }
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
            } else if (windowInfo.isTablet && uiState.activeTabMode == CalendarTabMode.SEMESTER_MILESTONES) {
                // Multi-column Adaptive Milestones Grid on Medium / Expanded / Desktop screens
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier
                        .widthIn(max = windowInfo.maxReadingWidth)
                        .fillMaxSize()
                        .testTag("calendar_milestones_grid"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = listBottomPadding
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            CalendarModeSwitchRow(
                                activeTabMode = uiState.activeTabMode,
                                onSelectTabMode = { viewModel.setTabMode(it) },
                                modifier = Modifier
                                    .widthIn(max = 520.dp)
                                    .padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "2026/2027 Academic Schedule",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Key university dates, test checkpoints, revision weeks, and official holidays.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(uiState.milestones, key = { it.id }) { milestone ->
                        val isLiveNow = remember(milestone) { milestone.isLiveNow() }
                        val isSelectedDate = milestone.contains(
                            uiState.currentYear,
                            uiState.currentMonth,
                            uiState.selectedDay
                        )
                        AcademicMilestoneCard(
                            milestone = milestone,
                            isLiveNow = isLiveNow,
                            isSelectedDate = isSelectedDate,
                            outerPadding = PaddingValues(0.dp),
                            modifier = Modifier.animateItem(),
                            onClick = {
                                viewModel.navigateToDate(
                                    milestone.startYear,
                                    milestone.startMonth,
                                    milestone.startDay
                                )
                                viewModel.setTabMode(CalendarTabMode.MONTH_PLANNER)
                            }
                        )
                    }
                }
            } else {
                // Compact Phones & Medium Portrait Tablets (< 840dp)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .widthIn(max = if (windowInfo.isTablet) 760.dp else windowInfo.maxReadingWidth)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(
                        top = innerPadding.calculateTopPadding() + 8.dp,
                        bottom = listBottomPadding
                    )
                ) {
                    item {
                        CalendarModeSwitchRow(
                            activeTabMode = uiState.activeTabMode,
                            onSelectTabMode = { viewModel.setTabMode(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (uiState.activeTabMode == CalendarTabMode.SEMESTER_MILESTONES) {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                Text(
                                    text = "2026/2027 Academic Schedule",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Key university dates, test checkpoints, revision weeks, and official holidays.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        items(uiState.milestones, key = { it.id }) { milestone ->
                            val isLiveNow = remember(milestone) { milestone.isLiveNow() }
                            val isSelectedDate = milestone.contains(
                                uiState.currentYear,
                                uiState.currentMonth,
                                uiState.selectedDay
                            )
                            AcademicMilestoneCard(
                                milestone = milestone,
                                isLiveNow = isLiveNow,
                                isSelectedDate = isSelectedDate,
                                modifier = Modifier.animateItem(),
                                onClick = {
                                    viewModel.navigateToDate(
                                        milestone.startYear,
                                        milestone.startMonth,
                                        milestone.startDay
                                    )
                                    viewModel.setTabMode(CalendarTabMode.MONTH_PLANNER)
                                }
                            )
                        }
                    } else {
                        item {
                            CalendarMonthSection(
                                uiState = uiState,
                                workload = workload,
                                viewModel = viewModel
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(CalendarFilter.entries.toTypedArray()) { filter ->
                                    val count = when (filter) {
                                        CalendarFilter.ALL -> selectedDayLectures.size + selectedDayActivities.size + selectedDayDeadlines.size
                                        CalendarFilter.LECTURES -> selectedDayLectures.size
                                        CalendarFilter.ACTIVITIES -> selectedDayActivities.size
                                        CalendarFilter.TASKS -> selectedDayDeadlines.size
                                    }
                                    FilterChip(
                                        selected = uiState.selectedFilter == filter,
                                        onClick = { viewModel.setFilter(filter) },
                                        label = { Text("${filter.label} ($count)") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        if (selectedDayDeadlines.isNotEmpty() && (uiState.selectedFilter == CalendarFilter.ALL || uiState.selectedFilter == CalendarFilter.TASKS)) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                                    DeadlinesSectionHeader()
                                }
                            }

                            items(selectedDayDeadlines, key = { "deadline_${it.id}" }) { task ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).animateItem()) {
                                    TaskCard(
                                        task = task,
                                        isDark = isDark,
                                        onToggleComplete = { viewModel.toggleTaskComplete(task) },
                                        onEdit = { },
                                        onDelete = { deleteTaskId = task.id }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(8.dp)) }
                        }

                        if (!uiState.academicContext.isInstructionalDay && (uiState.selectedFilter == CalendarFilter.ALL || uiState.selectedFilter == CalendarFilter.LECTURES)) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                                    NonInstructionalNoticeCard(phaseName = uiState.academicContext.phase.displayName)
                                }
                            }
                        }

                        if (timelineItems.isEmpty() && (selectedDayDeadlines.isEmpty() || uiState.selectedFilter != CalendarFilter.ALL)) {
                            item {
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                                    EmptyDayScheduleCard(selectedDayOfWeek = uiState.selectedDayOfWeek)
                                }
                            }
                        } else {
                            items(timelineItems, key = { item ->
                                when (item) {
                                    is CalendarTimelineItem.Lecture -> "lec_${item.entry.naturalKey}"
                                    is CalendarTimelineItem.Activity -> "act_${item.event.id}"
                                }
                            }) { item ->
                                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp).animateItem()) {
                                    when (item) {
                                        is CalendarTimelineItem.Lecture -> {
                                            LectureCard(
                                                entry = item.entry,
                                                onClick = { detailLectureKey = item.entry.naturalKey }
                                            )
                                        }
                                        is CalendarTimelineItem.Activity -> {
                                            ActivityCard(
                                                event = item.event,
                                                onClick = { detailActivityId = item.event.id },
                                                onEdit = { editingActivityId = item.event.id },
                                                onDelete = { deleteEventId = item.event.id }
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

    CalendarActionDialogs(
        selectedActivityForDetail = selectedActivityForDetail,
        activityToEdit = activityToEdit,
        selectedLectureForDetail = selectedLectureForDetail,
        showAddActivitySheet = showAddActivitySheet,
        showProfileSheet = showProfileSheet,
        eventToDelete = eventToDelete,
        taskToDelete = taskToDelete,
        currentProgramme = uiState.currentProgramme,
        enrolledProgramme = repository.getInitialProgramme() ?: "",
        selectedDayOfWeek = uiState.selectedDayOfWeek,
        onDismissActivityDetail = { detailActivityId = null },
        onEditActivity = { editingActivityId = it },
        onRequestDeleteActivity = { deleteEventId = it },
        onDismissEditActivity = { editingActivityId = null },
        onSaveActivity = { viewModel.addCustomEvent(it) },
        onSaveLectureNote = { entry, text, minutes ->
            viewModel.saveNote(entry, text, minutes)
            detailLectureKey = null
        },
        onDismissLectureDetail = { detailLectureKey = null },
        onDismissAddActivity = { showAddActivitySheet = false },
        onDismissProfileSheet = { showProfileSheet = false },
        onConfirmDeleteActivity = {
            viewModel.deleteCustomEvent(it)
            deleteEventId = null
        },
        onDismissDeleteActivity = { deleteEventId = null },
        onConfirmDeleteTask = {
            viewModel.deleteTask(it)
            deleteTaskId = null
        },
        onDismissDeleteTask = { deleteTaskId = null },
        onPeekProgramme = onPeekProgramme,
        onSetOfficialProgramme = onSetOfficialProgramme,
        onReconfigureAcademicProfile = onReconfigureAcademicProfile,
        onSettingsClick = onSettingsClick,
        onNavigateToTimetable = onNavigateToTimetable
    )
}

@Composable
private fun CalendarModeSwitchRow(
    activeTabMode: CalendarTabMode,
    onSelectTabMode: (CalendarTabMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FilterChip(
            selected = activeTabMode == CalendarTabMode.MONTH_PLANNER,
            onClick = { onSelectTabMode(CalendarTabMode.MONTH_PLANNER) },
            label = { Text("Month Planner", fontWeight = FontWeight.SemiBold) },
            leadingIcon = {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .desktopHandCursor()
        )

        FilterChip(
            selected = activeTabMode == CalendarTabMode.SEMESTER_MILESTONES,
            onClick = { onSelectTabMode(CalendarTabMode.SEMESTER_MILESTONES) },
            label = { Text("Semester Milestones", fontWeight = FontWeight.SemiBold) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primary,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .desktopHandCursor()
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarLegendCard(modifier: Modifier = Modifier) {
    val ext = LocalExtendedColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ext.borderSubtle)
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LegendDotLabel(color = Color(0xFFEF4444), label = "Deadlines")
            LegendDotLabel(color = MaterialTheme.colorScheme.primary, label = "Classes")
            LegendDotLabel(color = Color(0xFF10B981), label = "Activities")
            LegendDotLabel(color = Color(0xFF8B5CF6), label = "Milestones")
        }
    }
}

@Composable
private fun LegendDotLabel(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DeadlinesSectionHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = Color(0xFFEF4444),
            shape = CircleShape,
            modifier = Modifier.size(8.dp)
        ) {}
        Text(
            text = "Deadlines Due Today",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFEF4444)
        )
    }
}

@Composable
private fun NonInstructionalNoticeCard(phaseName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "No Regular Lectures Scheduled",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$phaseName. University instructional timetable is paused during this period.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyDayScheduleCard(selectedDayOfWeek: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Outlined.Event,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No commitments scheduled for $selectedDayOfWeek",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap 'Schedule Activity' below to plan a revision session or lab prep",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

