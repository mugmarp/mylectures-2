package com.lectures.features.timetable.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.lectures.TimetableApplication
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.core.util.SessionProgress
import com.lectures.core.util.TimeUtil
import com.lectures.core.util.rememberLiveClockPulse
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableConflictDetector
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.Chrome
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class GlobalNextUp(
    val entry: TimetableEntry,
    val isLive: Boolean,
    val progress: SessionProgress?,
    val minutesUntil: Int,
    val dayLabel: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    programme: String,
    enrolledProgramme: String = "",
    isPeeking: Boolean = false,
    entries: List<TimetableEntry>,
    customEvents: List<CustomEvent> = emptyList(),
    assignments: List<Assignment> = emptyList(),
    notes: Map<String, LectureNote> = emptyMap(),
    selectedDay: String,
    isLoading: Boolean = false,
    onDaySelected: (String) -> Unit,
    onSaveNote: (TimetableEntry, String, Int?) -> Unit = { _, _, _ -> },
    onAddActivity: (CustomEvent) -> Unit = {},
    onDeleteActivity: (Long) -> Unit = {},
    onAddTask: (title: String, courseCode: String, dueDate: String, priority: String, reminderMinutes: Int?, notes: String) -> Unit = { _, _, _, _, _, _ -> },
    onToggleTaskComplete: (Assignment) -> Unit = {},
    onDeleteTask: (Long) -> Unit = {},
    onPeekProgramme: (String) -> Unit = {},
    onResetToEnrolled: () -> Unit = {},
    onSetOfficialProgramme: (String) -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToNotes: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onOpenVacantRooms: () -> Unit = {}
) {
    val context = LocalContext.current
    val isDark = LocalAppTheme.current.isDark

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = {
            NotificationPermissionHelper.setPromptedNotificationPermission(context, true)
        }
    )
    LaunchedEffect(Unit) {
        val app = context.applicationContext as? TimetableApplication
        app?.analyticsManager?.trackFeature("timetable_view")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            val alreadyPrompted = NotificationPermissionHelper.hasPromptedNotificationPermission(context)
            val isExistingUser = (app?.repository?.checkIsOnboardingCompleted() == true) ||
                ETagStore(context).isOnboardingCompleted()

            if (!granted && !alreadyPrompted && !isExistingUser) {
                NotificationPermissionHelper.setPromptedNotificationPermission(context, true)
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var todayName by remember { mutableStateOf(TimeUtil.todayName()) }
    val daysList = remember { TimeUtil.DAYS }

    var detailLectureKey by rememberSaveable { mutableStateOf<String?>(null) }
    var alarmLectureKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showQuickAddSheet by rememberSaveable { mutableStateOf(false) }
    var showAcademicProfileSheet by rememberSaveable { mutableStateOf(false) }
    var detailActivityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingActivityId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedFilter by rememberSaveable { mutableStateOf("All") }

    val selectedEntryForDetail = remember(detailLectureKey, entries) {
        detailLectureKey?.let { key -> entries.firstOrNull { it.naturalKey == key } }
    }
    val selectedEntryForAlarm = remember(alarmLectureKey, entries) {
        alarmLectureKey?.let { key -> entries.firstOrNull { it.naturalKey == key } }
    }
    val selectedActivityForDetail = remember(detailActivityId, customEvents) {
        detailActivityId?.let { id -> customEvents.firstOrNull { it.id == id } }
    }
    val activityToEdit = remember(editingActivityId, customEvents) {
        editingActivityId?.let { id -> customEvents.firstOrNull { it.id == id } }
    }

    val dayInfoList = remember(todayName) {
        val now = Calendar.getInstance()
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = when (currentDayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        val mondayCal = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        }
        val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

        daysList.mapIndexed { index, dayName ->
            val dayCal = (mondayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, index)
            }
            Triple(dayName, dayCal.get(Calendar.DAY_OF_MONTH), dateFormat.format(dayCal.time))
        }
    }

    val clockPulse = rememberLiveClockPulse()
    val realTodayName = TimeUtil.todayName()
    if (!todayName.equals(realTodayName, ignoreCase = true)) {
        todayName = realTodayName
    }

    val globalNextUp = remember(entries, todayName, clockPulse) {
        findActualNextUp(entries, todayName)
    }

    val dayEntries = remember(entries, selectedDay, selectedFilter) {
        val onDay = entries.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        val filtered = when (selectedFilter) {
            "Lectures" -> onDay.filter {
                val label = resolveSessionTypeLabel(it)
                label == "Lecture" || label == "Clinical"
            }
            "Labs" -> onDay.filter {
                val label = resolveSessionTypeLabel(it)
                label == "Lab" || label == "Practical"
            }
            "Associations" -> onDay.filter {
                resolveSessionTypeLabel(it) == "Student Association"
            }
            else -> onDay
        }
        filtered.sortedBy { TimeUtil.toMinutes(it.startTime) }
    }

    val dayCustomEvents = remember(customEvents, selectedDay, selectedFilter) {
        val onDay = customEvents.filter { it.dayOfWeek.equals(selectedDay, ignoreCase = true) }
        val filtered = when (selectedFilter) {
            "Lectures", "Labs" -> emptyList()
            else -> onDay
        }
        filtered.sortedBy { TimeUtil.toMinutes(it.startTime) }
    }

    val isViewingToday = selectedDay.equals(todayName, ignoreCase = true)
    val selectedDayInfo = dayInfoList.firstOrNull { it.first.equals(selectedDay, ignoreCase = true) }
    val formattedSelectedDate = selectedDayInfo?.third ?: selectedDay

    val todayEntries = remember(entries, todayName) {
        entries.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }
            .sortedBy { TimeUtil.toMinutes(it.startTime) }
    }
    val todayDateFormatted = remember(dayInfoList, todayName) {
        dayInfoList.firstOrNull { it.first.equals(todayName, ignoreCase = true) }?.third
            ?: SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    }

    val allTodayClassesCompleted = remember(todayEntries, clockPulse) {
        if (todayEntries.isEmpty()) {
            false
        } else {
            val now = Calendar.getInstance()
            val curMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
            todayEntries.all { entry ->
                val endMin = TimeUtil.toMinutes(entry.endTime)
                endMin > 0 && endMin <= curMin
            }
        }
    }

    var dragTotal by remember { mutableFloatStateOf(0f) }
    val windowInfo = rememberLecturesWindow()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = Chrome.listBottomPadding(navBarBottom, windowInfo.useNavigationRail)
    val listState = rememberLazyListState()

    if (isPeeking) {
        BackHandler(enabled = true) { onResetToEnrolled() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = if (isDark) DarkSurfaceBase else SurfaceBaseLight,
        modifier = Modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when {
                    event.isCtrlPressed && event.key == Key.N -> {
                        showQuickAddSheet = true
                        true
                    }
                    event.key == Key.Escape && (detailLectureKey != null || detailActivityId != null) -> {
                        detailLectureKey = null
                        detailActivityId = null
                        true
                    }
                    event.key == Key.DirectionLeft -> {
                        val idx = daysList.indexOfFirst { it.equals(selectedDay, ignoreCase = true) }
                        if (idx != -1) {
                            val prev = if (idx - 1 < 0) daysList.lastIndex else idx - 1
                            onDaySelected(daysList[prev])
                            true
                        } else false
                    }
                    event.key == Key.DirectionRight -> {
                        val idx = daysList.indexOfFirst { it.equals(selectedDay, ignoreCase = true) }
                        if (idx != -1) {
                            onDaySelected(daysList[(idx + 1) % daysList.size])
                            true
                        } else false
                    }
                    else -> false
                }
            } else false
        },
        topBar = {
            val semesterWeekLabel = remember { TimeUtil.currentSemesterWeekLabel() }
            TimetableTopAppBar(
                program = programme,
                subtitle = if (programme.isNotBlank()) semesterWeekLabel else "Select Programme",
                isPeeking = isPeeking,
                onProfileClick = { showAcademicProfileSheet = true },
                onSettingsClick = onSettingsClick,
                onOpenVacantRooms = onOpenVacantRooms
            )
        },
        floatingActionButton = {
            if (!windowInfo.isDualPane) {
                val fabInk = contrastOnColor(MaterialTheme.colorScheme.primary)
                if (windowInfo.collapseFab) {
                    FloatingActionButton(
                        onClick = { showQuickAddSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = fabInk,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                            .desktopHandCursor()
                            .testTag("timetable_fab")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Activity", modifier = Modifier.size(24.dp))
                    }
                } else {
                    ExtendedFloatingActionButton(
                        onClick = { showQuickAddSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = fabInk,
                        shape = RoundedCornerShape(16.dp),
                        icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        text = { Text(text = "Add Activity", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                            .desktopHandCursor()
                            .testTag("timetable_fab")
                    )
                }
            }
        }
    ) { innerPadding ->
        if (windowInfo.isDualPane) {
            TimetableDesktopWorkspace(
                programme = programme,
                enrolledProgramme = enrolledProgramme,
                isPeeking = isPeeking,
                entries = entries,
                customEvents = customEvents,
                notes = notes,
                selectedDay = selectedDay,
                todayName = todayName,
                daysList = daysList,
                dayInfoList = dayInfoList,
                dayEntries = dayEntries,
                dayCustomEvents = dayCustomEvents,
                globalNextUp = globalNextUp,
                selectedFilter = selectedFilter,
                isLoading = isLoading,
                selectedEntryForDetail = selectedEntryForDetail,
                selectedActivityForDetail = selectedActivityForDetail,
                topContentPadding = innerPadding.calculateTopPadding() + 8.dp,
                bottomContentPadding = listBottomPadding,
                onDaySelected = onDaySelected,
                onCycleFilter = {
                    selectedFilter = when (selectedFilter) {
                        "All" -> "Lectures"
                        "Lectures" -> "Labs"
                        "Labs" -> "Associations"
                        else -> "All"
                    }
                },
                onInspectEntry = { key ->
                    detailActivityId = null
                    detailLectureKey = key
                },
                onDismissInspectEntry = { detailLectureKey = null },
                onOpenAlarmPicker = { key -> alarmLectureKey = key },
                onSaveNote = onSaveNote,
                onInspectActivity = { id ->
                    detailLectureKey = null
                    detailActivityId = id
                },
                onDismissInspectActivity = { detailActivityId = null },
                onEditActivity = { id ->
                    detailActivityId = null
                    editingActivityId = id
                },
                onDeleteActivity = { id ->
                    detailActivityId = null
                    onDeleteActivity(id)
                },
                onOpenQuickAdd = { showQuickAddSheet = true },
                onResetToEnrolled = onResetToEnrolled,
                onSettingsClick = onSettingsClick
            )
        } else {
        Box(
            contentAlignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(selectedDay, daysList) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            val threshold = 70f
                            val currentIndex = daysList.indexOfFirst { it.equals(selectedDay, ignoreCase = true) }
                            if (currentIndex != -1) {
                                if (dragTotal < -threshold) {
                                    onDaySelected(daysList[(currentIndex + 1) % daysList.size])
                                } else if (dragTotal > threshold) {
                                    val prevIndex = if (currentIndex - 1 < 0) daysList.size - 1 else currentIndex - 1
                                    onDaySelected(daysList[prevIndex])
                                }
                            }
                            dragTotal = 0f
                        },
                        onDragCancel = { dragTotal = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragTotal += dragAmount
                        }
                    )
                }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = listBottomPadding
                )
            ) {
                if (isPeeking) {
                    item(key = "peek_mode_banner") {
                        TimetablePeekBanner(
                            programme = programme,
                            enrolledProgramme = enrolledProgramme,
                            isDark = isDark,
                            onResetToEnrolled = onResetToEnrolled
                        )
                    }
                }

                if (programme.isBlank()) {
                    item(key = "no_prog_prompt") {
                        NoProgrammePromptCard(isDark = isDark, onSelectClick = onSettingsClick)
                    }
                }

                item(key = "today_schedule_header") {
                    TodaysScheduleHeader(
                        todayDateFormatted = todayDateFormatted,
                        todaySessionCount = todayEntries.size,
                        isDark = isDark
                    )
                }

                val currentNextUp = globalNextUp
                if (allTodayClassesCompleted && currentNextUp != null && !currentNextUp.isLive) {
                    item(key = "today_completed_badge") {
                        TodayClassesCompletedBadge()
                    }
                }

                if (currentNextUp != null) {
                    item(key = "today_hero_card") {
                        val entry = currentNextUp.entry
                        val note = notes[entry.naturalKey]

                        if (currentNextUp.isLive) {
                            val liveProg = TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek)
                            val session = entry.toSpecClassSession(
                                currentProgramme = programme,
                                note = note,
                                isLive = true,
                                progress = if (liveProg.isOngoing) liveProg else currentNextUp.progress
                            )
                            HappeningNowHeroCard(
                                session = session,
                                progress = session.progress,
                                elapsedText = session.elapsedText,
                                remainingText = session.remainingText,
                                onQuickNote = { detailLectureKey = entry.naturalKey },
                                onAlarmDismiss = { alarmLectureKey = entry.naturalKey },
                                onClick = { detailLectureKey = entry.naturalKey },
                                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                            )
                        } else {
                            val countdownText = when {
                                currentNextUp.dayLabel.equals("Today", ignoreCase = true) -> {
                                    when {
                                        currentNextUp.minutesUntil in 1..59 -> "in ${currentNextUp.minutesUntil}m"
                                        currentNextUp.minutesUntil >= 60 -> "in ${currentNextUp.minutesUntil / 60}h ${currentNextUp.minutesUntil % 60}m"
                                        else -> "Starting soon"
                                    }
                                }
                                else -> "${currentNextUp.dayLabel} at ${entry.startTime}"
                            }
                            val session = entry.toSpecClassSession(
                                currentProgramme = programme,
                                note = note,
                                isLive = false,
                                startsInText = countdownText
                            )
                            NextUpHeroCard(
                                session = session,
                                onSetAlarm = { alarmLectureKey = entry.naturalKey },
                                onQuickNote = { detailLectureKey = entry.naturalKey },
                                onClick = { detailLectureKey = entry.naturalKey },
                                modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                            )
                        }
                    }
                } else if (todayEntries.isEmpty() && programme.isNotBlank() && !isLoading) {
                    item(key = "no_classes_today_card") {
                        NoClassesTodayCard(isDark = isDark)
                    }
                }

                item(key = "day_capsules") {
                    val specDays = remember(daysList, selectedDay, todayName) {
                        daysList.map { dayName ->
                            SpecDayItem(
                                name = dayName,
                                isToday = dayName.equals(todayName, ignoreCase = true),
                                isSelected = dayName.equals(selectedDay, ignoreCase = true)
                            )
                        }
                    }
                    DayCapsuleStrip(
                        dayItems = specDays,
                        onDaySelected = { onDaySelected(it) },
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                item(key = "schedule_header") {
                    val filterLabel = when (selectedFilter) {
                        "All" -> "All Sessions"
                        else -> selectedFilter
                    }
                    ScheduleHeaderSection(
                        title = "$selectedDay's Schedule",
                        dateText = formattedSelectedDate,
                        activeFilter = filterLabel,
                        onFilterClicked = {
                            selectedFilter = when (selectedFilter) {
                                "All" -> "Lectures"
                                "Lectures" -> "Labs"
                                "Labs" -> "Associations"
                                else -> "All"
                            }
                        },
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                if (isLoading) {
                    item(key = "loading_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                } else if (dayEntries.isEmpty() && dayCustomEvents.isEmpty()) {
                    item(key = "empty_state") {
                        EmptyDayScheduleCard(
                            selectedDay = selectedDay,
                            isDark = isDark,
                            onAddCustomActivityClick = { showQuickAddSheet = true }
                        )
                    }
                } else {
                    items(dayEntries, key = { it.naturalKey.ifBlank { "${it.courseCode}_${it.startTime}_${it.dayOfWeek}" } }) { entry ->
                        val entryNote = notes[entry.naturalKey]
                        val hasConflict = TimetableConflictDetector.hasConflict(entry, dayEntries)
                        val startsIn = if (isViewingToday) {
                            val now = Calendar.getInstance()
                            val curMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                            val startMin = TimeUtil.toMinutes(entry.startTime)
                            val endMin = TimeUtil.toMinutes(entry.endTime)
                            val diff = startMin - curMin
                            when {
                                endMin > startMin && curMin in startMin until endMin -> "Live now"
                                diff in 1..120 -> "in ${diff}m"
                                else -> null
                            }
                        } else null

                        val specSession = entry.toSpecClassSession(
                            currentProgramme = programme,
                            hasConflict = hasConflict,
                            note = entryNote,
                            progress = if (isViewingToday) TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek) else null,
                            startsInText = startsIn
                        )
                        TimelineClassCard(
                            session = specSession,
                            onToggleAlarm = { alarmLectureKey = entry.naturalKey },
                            onAttachedNoteClick = { detailLectureKey = entry.naturalKey },
                            onClick = { detailLectureKey = entry.naturalKey },
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .animateItem()
                        )
                    }

                    items(dayCustomEvents, key = { "custom_event_${it.id}" }) { event ->
                        PaddingBox(top = 4.dp, bottom = 6.dp, modifier = Modifier.animateItem()) {
                            ActivityCard(
                                event = event,
                                onClick = { detailActivityId = event.id },
                                onEdit = { editingActivityId = event.id },
                                onDelete = { onDeleteActivity(event.id) }
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (!windowInfo.isDualPane) {
        selectedActivityForDetail?.let { event ->
            TimetableActivityDetailDialog(
                event = event,
                onDismiss = { detailActivityId = null },
                onEdit = { id ->
                    detailActivityId = null
                    editingActivityId = id
                },
                onDelete = { id ->
                    detailActivityId = null
                    onDeleteActivity(id)
                }
            )
        }
    }

    if (activityToEdit != null) {
        AddActivitySheet(
            eventToEdit = activityToEdit,
            onDismiss = { editingActivityId = null },
            onSave = { updatedEvent ->
                onAddActivity(updatedEvent)
                editingActivityId = null
            }
        )
    }

    if (!windowInfo.isDualPane && selectedEntryForDetail != null) {
        val entry = selectedEntryForDetail
        LectureDetailSheet(
            entry = entry,
            initialNote = notes[entry.naturalKey]?.content ?: "",
            initialReminder = notes[entry.naturalKey]?.alarmMinutes,
            onSaveNote = { noteText, reminderMinutes ->
                onSaveNote(entry, noteText, reminderMinutes)
                detailLectureKey = null
            },
            onDismiss = { detailLectureKey = null }
        )
    }

    if (selectedEntryForAlarm != null) {
        val entry = selectedEntryForAlarm
        val currentRem = notes[entry.naturalKey]?.alarmMinutes
        ClassAlarmPickerDialog(
            entry = entry,
            currentMinutes = currentRem,
            onConfirm = { chosenMinutes ->
                val currentNoteContent = notes[entry.naturalKey]?.content ?: ""
                onSaveNote(entry, currentNoteContent, chosenMinutes)
                alarmLectureKey = null
            },
            onDismiss = { alarmLectureKey = null }
        )
    }

    if (showQuickAddSheet) {
        val appInstance = context.applicationContext as? TimetableApplication
        val defaultTaskLeadHours by (appInstance?.repository?.taskReminderLeadPref
            ?.collectAsState(initial = appInstance.repository.getTaskReminderLeadHours())
            ?: remember { mutableIntStateOf(2) })
        val courseDetailsList = remember(entries) {
            entries.map { it.courseCode to it.courseTitle }.distinctBy { it.first }
        }
        QuickAddBottomSheet(
            initialDayOfWeek = selectedDay,
            availableCourses = entries.map { it.courseCode }.distinct(),
            courseDetails = courseDetailsList,
            defaultTaskReminderMinutes = defaultTaskLeadHours * 60,
            onDismiss = { showQuickAddSheet = false },
            onSaveActivity = { event ->
                onAddActivity(event)
                showQuickAddSheet = false
            },
            onSaveTask = { title, course, due, prio, rem, nts ->
                onAddTask(title, course, due, prio, rem, nts)
                showQuickAddSheet = false
            }
        )
    }

    if (showAcademicProfileSheet) {
        AcademicProfileSheet(
            programme = programme,
            enrolledProgramme = enrolledProgramme,
            isPeeking = isPeeking,
            onDismiss = { showAcademicProfileSheet = false },
            onPeekProgramme = onPeekProgramme,
            onResetToEnrolled = onResetToEnrolled,
            onSetOfficialProgramme = onSetOfficialProgramme,
            onReconfigureProfile = {
                showAcademicProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showAcademicProfileSheet = false
                onSettingsClick()
            }
        )
    }
}
