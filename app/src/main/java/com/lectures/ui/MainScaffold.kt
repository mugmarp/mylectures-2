package com.lectures.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.lectures.core.notification.InAppNotificationItem
import com.lectures.core.notification.InAppNotificationStore
import com.lectures.core.notification.NotificationItemType
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.TimetableApplication
import com.lectures.features.calendar.CalendarScreen
import com.lectures.features.notes.NotesScreen
import com.lectures.features.onboarding.*
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.tasks.TasksScreen
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.lectures.features.timetable.ui.TimetableRoute
import com.lectures.features.timetable.ui.WelcomeScreen
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.AdaptiveNavigationRailOrSidebar
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class MainScreenDestination {
    TABS,
    SETTINGS,
    VACANT_ROOMS,
    ACCOUNT
}

sealed class AppNavState {
    data object Welcome : AppNavState()
    data object FacultySelection : AppNavState()
    data class ProgrammeSelection(val faculty: Faculty) : AppNavState()
    data class ClassGroupSelection(val faculty: Faculty, val programme: Programme) : AppNavState()
    data object MainApp : AppNavState()
}

private val AppNavStateSaver: Saver<AppNavState, String> = Saver(
    save = { state ->
        when (state) {
            is AppNavState.Welcome -> "WELCOME"
            is AppNavState.FacultySelection -> "FACULTY"
            is AppNavState.ProgrammeSelection -> "PROG|${state.faculty.id}"
            is AppNavState.ClassGroupSelection -> "GROUP|${state.faculty.id}|${state.programme.code}"
            is AppNavState.MainApp -> "MAIN"
        }
    },
    restore = { encoded ->
        val parts = encoded.split("|")
        when (parts.firstOrNull()) {
            "WELCOME" -> AppNavState.Welcome
            "FACULTY" -> AppNavState.FacultySelection
            "PROG" -> {
                val faculty = FACULTIES.firstOrNull { it.id == parts.getOrNull(1) }
                if (faculty != null) AppNavState.ProgrammeSelection(faculty) else AppNavState.FacultySelection
            }
            "GROUP" -> {
                val faculty = FACULTIES.firstOrNull { it.id == parts.getOrNull(1) }
                val prog = faculty?.programmes?.firstOrNull { it.code == parts.getOrNull(2) }
                if (faculty != null && prog != null) {
                    AppNavState.ClassGroupSelection(faculty, prog)
                } else if (faculty != null) {
                    AppNavState.ProgrammeSelection(faculty)
                } else {
                    AppNavState.FacultySelection
                }
            }
            "MAIN" -> AppNavState.MainApp
            else -> AppNavState.Welcome
        }
    }
)

@Composable
fun MainScaffold(
    initialTab: Int = 0,
    notificationIntentFlow: SharedFlow<Pair<Int, Long?>>? = null
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

    val scope = rememberCoroutineScope()
    val initialProg = remember { repository.getInitialProgramme() }
    val savedProgramme by repository.programmePref.collectAsState(initial = initialProg)
    val initialOnboarding = remember { repository.isOnboardingCompleted() }
    val onboardingCompleted by repository.onboardingCompletedPref.collectAsState(initial = initialOnboarding)

    var navState by rememberSaveable(stateSaver = AppNavStateSaver) {
        mutableStateOf<AppNavState>(
            if (initialOnboarding) AppNavState.MainApp else AppNavState.Welcome
        )
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var isNotesEditing by rememberSaveable { mutableStateOf(false) }
    var pendingHighlightTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    // First app launch resolution via DataStore / Preferences
    LaunchedEffect(onboardingCompleted) {
        if (onboardingCompleted && (navState is AppNavState.Welcome || navState is AppNavState.FacultySelection)) {
            navState = AppNavState.MainApp
        }
    }

    when (val state = navState) {
        is AppNavState.Welcome -> {
            WelcomeScreen(
                onGetStarted = { navState = AppNavState.FacultySelection }
            )
        }
        is AppNavState.FacultySelection -> {
            BackHandler {
                navState = if (onboardingCompleted == true) AppNavState.MainApp else AppNavState.Welcome
            }
            FacultySelectionScreen(
                onSelectFaculty = { faculty -> navState = AppNavState.ProgrammeSelection(faculty) },
                onBack = {
                    navState = if (onboardingCompleted == true) AppNavState.MainApp else AppNavState.Welcome
                }
            )
        }
        is AppNavState.ProgrammeSelection -> {
            BackHandler {
                navState = AppNavState.FacultySelection
            }
            ProgrammeSelectionScreen(
                faculty = state.faculty,
                onSelectProgramme = { prog -> navState = AppNavState.ClassGroupSelection(state.faculty, prog) },
                onBack = { navState = AppNavState.FacultySelection }
            )
        }
        is AppNavState.ClassGroupSelection -> {
            BackHandler {
                navState = AppNavState.ProgrammeSelection(state.faculty)
            }
            val allEntries by repository.getAllEntries().collectAsState(initial = emptyList())
            val liveCohortCounts by repository.getCohortCounts(state.programme.code).collectAsState(initial = emptyMap())
            val calculatedCounts = remember(state.programme, allEntries, liveCohortCounts) {
                state.programme.allGroups.associateWith { g ->
                    com.lectures.features.timetable.domain.TimetableMatcher.getGroupCount(
                        group = g,
                        entries = allEntries,
                        cohortCounts = liveCohortCounts
                    )
                }
            }
            ClassGroupSelectionScreen(
                programme = state.programme,
                faculty = state.faculty,
                entryCounts = calculatedCounts,
                onConfirm = { chosenGroup ->
                    scope.launch {
                        repository.setProgrammePref(chosenGroup)
                        repository.setOnboardingCompleted(true)
                        com.lectures.core.alarm.AlarmSyncCoordinator.resyncAll(
                            context = context,
                            repository = repository,
                            force = true,
                            reason = "onboarding_group_selected"
                        )
                        navState = AppNavState.MainApp
                    }
                },
                onBack = { navState = AppNavState.ProgrammeSelection(state.faculty) }
            )
        }
        is AppNavState.MainApp -> {
            var currentDestination by rememberSaveable { mutableStateOf(MainScreenDestination.TABS) }

            // Handle notification deep-link intents even when user is inside Settings, Account, or Vacant Rooms.
            LaunchedEffect(notificationIntentFlow) {
                notificationIntentFlow?.collect { (tab, taskId) ->
                    currentDestination = MainScreenDestination.TABS
                    selectedTab = tab
                    pendingHighlightTaskId = taskId
                }
            }

            val inAppStore = remember(context) { InAppNotificationStore.getInstance(context) }
            var activeForegroundAlert by remember { mutableStateOf<InAppNotificationItem?>(null) }

            LaunchedEffect(Unit) {
                InAppNotificationStore.foregroundAlertFlow.collect { alert ->
                    activeForegroundAlert = alert
                }
            }

            LaunchedEffect(activeForegroundAlert) {
                if (activeForegroundAlert != null) {
                    kotlinx.coroutines.delay(5000)
                    activeForegroundAlert = null
                }
            }

            val haptic = LocalHapticFeedback.current
            val tabs = listOf("Timetable", "Calendar", "Notes", "Tasks")
            val filledIcons = listOf(
                Icons.Filled.CalendarToday,
                Icons.Filled.CalendarMonth,
                Icons.AutoMirrored.Filled.Notes,
                Icons.Filled.TaskAlt
            )
            val outlinedIcons = listOf(
                Icons.Outlined.CalendarToday,
                Icons.Outlined.CalendarMonth,
                Icons.AutoMirrored.Outlined.Notes,
                Icons.Outlined.TaskAlt
            )

            val isDark = com.lectures.ui.LocalAppTheme.current.isDark
            val navBg = if (isDark) com.lectures.features.timetable.ui.DarkSurfaceCard else Color.White
            val navBorder = if (isDark) com.lectures.features.timetable.ui.DarkBorderSubtle else com.lectures.features.timetable.ui.BorderSubtleLight
            val primaryBlue = MaterialTheme.colorScheme.primary
            val textMuted = if (isDark) Color(0xFF94A3B8) else com.lectures.features.timetable.ui.TextMutedLight

            val classAlarmScheduler = remember(context) { com.lectures.core.alarm.AlarmScheduler(context) }
            val taskAlarmScheduler = remember(context) { com.lectures.core.alarm.TaskAlarmScheduler(context) }
            val timetableViewModel: com.lectures.features.timetable.ui.TimetableViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                factory = com.lectures.features.timetable.ui.TimetableViewModel.provideFactory(
                    repository = repository,
                    alarmScheduler = taskAlarmScheduler,
                    classAlarmScheduler = classAlarmScheduler,
                    appContext = context.applicationContext
                )
            )

            val onPeekProgrammeAndGoToTimetable: (String) -> Unit = { group ->
                timetableViewModel.peekProgramme(group)
                currentDestination = MainScreenDestination.TABS
                selectedTab = 0
            }
            val onSetOfficialProgrammeAndGoToTimetable: (String) -> Unit = { group ->
                timetableViewModel.setOfficialProgramme(group)
                currentDestination = MainScreenDestination.TABS
                selectedTab = 0
            }

            val windowInfo = rememberLecturesWindow()
            var sidebarCollapsed by rememberSaveable { mutableStateOf(false) }
            var showSidebarProfileSheet by rememberSaveable { mutableStateOf(false) }

            // BackHandler per screen destination
            BackHandler(enabled = currentDestination != MainScreenDestination.TABS) {
                currentDestination = MainScreenDestination.TABS
            }
            BackHandler(enabled = currentDestination == MainScreenDestination.TABS && selectedTab != 0) {
                selectedTab = 0
            }

            if (showSidebarProfileSheet) {
                AcademicProfileSheet(
                    programme = savedProgramme ?: "",
                    enrolledProgramme = savedProgramme ?: "",
                    onDismiss = { showSidebarProfileSheet = false },
                    onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                    onResetToEnrolled = {
                        timetableViewModel.resetToEnrolledProgramme()
                        currentDestination = MainScreenDestination.TABS
                        selectedTab = 0
                    },
                    onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                    onReconfigureProfile = {
                        showSidebarProfileSheet = false
                        navState = AppNavState.FacultySelection
                    },
                    onOpenSettings = {
                        showSidebarProfileSheet = false
                        currentDestination = MainScreenDestination.SETTINGS
                    },
                    onNavigateToTimetable = {
                        showSidebarProfileSheet = false
                        currentDestination = MainScreenDestination.TABS
                        selectedTab = 0
                    }
                )
            }

            @Composable
            fun ForegroundAlertOverlay(modifier: Modifier = Modifier) {
                AnimatedVisibility(
                    visible = activeForegroundAlert != null,
                    enter = slideInVertically(
                        initialOffsetY = { -it },
                        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(),
                    exit = slideOutVertically(
                        targetOffsetY = { -it },
                        animationSpec = tween(180)
                    ) + fadeOut(),
                    modifier = modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(100f)
                ) {
                    activeForegroundAlert?.let { alert ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isDark) com.lectures.features.timetable.ui.DarkSurfaceCard else Color.White,
                            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.4f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .clickable {
                                    alert.navTab?.let {
                                        currentDestination = MainScreenDestination.TABS
                                        selectedTab = it
                                    }
                                    activeForegroundAlert = null
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(primaryBlue.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = primaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val ext = LocalExtendedColors.current
                                    Text(
                                        text = alert.title,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = alert.message,
                                        fontSize = 12.sp,
                                        color = ext.textMuted,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { activeForegroundAlert = null }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss",
                                        tint = textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            @Composable
            fun DestinationContent(modifier: Modifier = Modifier) {
                Box(modifier = modifier) {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            if (windowInfo.useNavigationRail) {
                                fadeIn(animationSpec = tween(160))
                                    .togetherWith(fadeOut(animationSpec = tween(110)))
                            } else if (targetState != MainScreenDestination.TABS) {
                                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                        fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } +
                                                fadeOut(animationSpec = tween(150))
                                    )
                            } else {
                                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { -it / 3 } +
                                        fadeIn(animationSpec = tween(220)))
                                    .togetherWith(
                                        slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { it } +
                                                fadeOut(animationSpec = tween(150))
                                    )
                            }
                        },
                        label = "SubScreenNavigation"
                    ) { destination ->
                        when (destination) {
                            MainScreenDestination.VACANT_ROOMS -> {
                                com.lectures.features.rooms.ui.VacantRoomsScreen(
                                    onBack = if (windowInfo.useNavigationRail) null else {
                                        { currentDestination = MainScreenDestination.TABS }
                                    },
                                    enrolledProgramme = savedProgramme
                                )
                            }
                            MainScreenDestination.ACCOUNT -> {
                                com.lectures.features.account.AccountScreen(
                                    onBack = if (windowInfo.useNavigationRail) null else {
                                        { currentDestination = MainScreenDestination.TABS }
                                    }
                                )
                            }
                            MainScreenDestination.SETTINGS -> {
                                SettingsScreen(
                                    currentProgramme = savedProgramme ?: "",
                                    onReconfigureAcademicProfile = {
                                        currentDestination = MainScreenDestination.TABS
                                        navState = AppNavState.FacultySelection
                                    },
                                    onOpenAccount = {
                                        currentDestination = MainScreenDestination.ACCOUNT
                                    },
                                    onResetOnboarding = {
                                        scope.launch {
                                            repository.setOnboardingCompleted(false)
                                            currentDestination = MainScreenDestination.TABS
                                            navState = AppNavState.Welcome
                                        }
                                    },
                                    onBack = if (windowInfo.useNavigationRail) null else {
                                        { currentDestination = MainScreenDestination.TABS }
                                    }
                                )
                            }
                            MainScreenDestination.TABS -> {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = {
                                            val direction = if (targetState > initialState) 1 else -1
                                            (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (direction * width * 0.14f).toInt() } +
                                                    fadeIn(animationSpec = tween(180)))
                                                .togetherWith(
                                                    slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (-direction * width * 0.14f).toInt() } +
                                                            fadeOut(animationSpec = tween(120))
                                                )
                                        },
                                        label = "MainTabsTransition"
                                    ) { tabIndex ->
                                        when (tabIndex) {
                                            0 -> com.lectures.core.util.ProvideLiveClockPulse {
                                                TimetableRoute(
                                                    onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                                    onNavigateToNotes = { selectedTab = 2 },
                                                    onNavigateToCalendar = { selectedTab = 1 },
                                                    onNavigateToTasks = { selectedTab = 3 },
                                                    onReconfigureAcademicProfile = {
                                                        currentDestination = MainScreenDestination.TABS
                                                        navState = AppNavState.FacultySelection
                                                    },
                                                    onOpenVacantRooms = { currentDestination = MainScreenDestination.VACANT_ROOMS },
                                                    sharedViewModel = timetableViewModel
                                                )
                                            }
                                            1 -> com.lectures.core.util.ProvideLiveClockPulse {
                                                CalendarScreen(
                                                    onBack = null,
                                                    onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                                    onReconfigureAcademicProfile = {
                                                        currentDestination = MainScreenDestination.TABS
                                                        navState = AppNavState.FacultySelection
                                                    },
                                                    onNavigateToTimetable = { selectedTab = 0 },
                                                    onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                                                    onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable
                                                )
                                            }
                                            2 -> NotesScreen(
                                                onBack = null,
                                                onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                                onReconfigureAcademicProfile = {
                                                    currentDestination = MainScreenDestination.TABS
                                                    navState = AppNavState.FacultySelection
                                                },
                                                onNavigateToTimetable = { selectedTab = 0 },
                                                onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                                                onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                                onEditingChanged = { isNotesEditing = it }
                                            )
                                            3 -> TasksScreen(
                                                onBack = null,
                                                onSettingsClick = { currentDestination = MainScreenDestination.SETTINGS },
                                                onReconfigureAcademicProfile = {
                                                    currentDestination = MainScreenDestination.TABS
                                                    navState = AppNavState.FacultySelection
                                                },
                                                onNavigateToTimetable = { selectedTab = 0 },
                                                onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                                                onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                                highlightTaskId = pendingHighlightTaskId
                                            )
                                        }
                                    }

                                    // Mobile Floating Pill Dock: Rendered exclusively on phones when rail is inactive
                                    if (!windowInfo.useNavigationRail && !isNotesEditing && !windowInfo.isImeVisible) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .navigationBarsPadding()
                                                .padding(start = Chrome.DockSideInset, end = Chrome.DockSideInset, bottom = Chrome.DockBottomGap),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(24.dp),
                                                color = navBg.copy(alpha = 0.96f),
                                                tonalElevation = 6.dp,
                                                shadowElevation = 8.dp,
                                                border = BorderStroke(1.dp, navBorder),
                                                modifier = Modifier
                                                    .widthIn(max = 440.dp)
                                                    .fillMaxWidth()
                                                    .height(Chrome.DockHeight)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    tabs.forEachIndexed { index, tab ->
                                                        val isSelected = selectedTab == index
                                                        val icon = if (isSelected) filledIcons[index] else outlinedIcons[index]

                                                        val pillColor by animateColorAsState(
                                                            targetValue = if (isSelected) primaryBlue.copy(alpha = 0.14f) else Color.Transparent,
                                                            animationSpec = tween(180),
                                                            label = "pill_color"
                                                        )
                                                        val iconScale by animateFloatAsState(
                                                            targetValue = if (isSelected) 1.08f else 1.0f,
                                                            animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMedium),
                                                            label = "icon_scale"
                                                        )

                                                        Box(
                                                            modifier = Modifier
                                                                .weight(1f)
                                                                .fillMaxHeight()
                                                                .clip(RoundedCornerShape(20.dp))
                                                                .clickable {
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                    selectedTab = index
                                                                }
                                                                .testTag("nav_${tab.lowercase(java.util.Locale.ROOT)}"),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Column(
                                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                                verticalArrangement = Arrangement.Center
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(14.dp))
                                                                        .background(pillColor)
                                                                        .padding(horizontal = if (windowInfo.dockIconOnly) 16.dp else 10.dp, vertical = if (windowInfo.dockIconOnly) 6.dp else 2.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Icon(
                                                                        imageVector = icon,
                                                                        contentDescription = tab,
                                                                        tint = if (isSelected) primaryBlue else textMuted,
                                                                        modifier = Modifier
                                                                            .size(if (windowInfo.dockIconOnly) 24.dp else 21.dp)
                                                                            .graphicsLayer {
                                                                                scaleX = iconScale
                                                                                scaleY = iconScale
                                                                            }
                                                                    )
                                                                }
                                                                if (!windowInfo.dockIconOnly) {
                                                                    Spacer(modifier = Modifier.height(1.dp))
                                                                    Text(
                                                                        text = tab,
                                                                        fontSize = 10.sp,
                                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                        color = if (isSelected) primaryBlue else textMuted,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
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

                    ForegroundAlertOverlay(modifier = Modifier.align(Alignment.TopCenter))
                }
            }

            val desktopShortcutModifier = Modifier.onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (keyEvent.isCtrlPressed) {
                    when (keyEvent.key) {
                        Key.One -> {
                            currentDestination = MainScreenDestination.TABS
                            selectedTab = 0
                            true
                        }
                        Key.Two -> {
                            currentDestination = MainScreenDestination.TABS
                            selectedTab = 1
                            true
                        }
                        Key.Three -> {
                            currentDestination = MainScreenDestination.TABS
                            selectedTab = 2
                            true
                        }
                        Key.Four -> {
                            currentDestination = MainScreenDestination.TABS
                            selectedTab = 3
                            true
                        }
                        Key.Five -> {
                            currentDestination = MainScreenDestination.VACANT_ROOMS
                            true
                        }
                        Key.Six -> {
                            currentDestination = MainScreenDestination.ACCOUNT
                            true
                        }
                        Key.Comma -> {
                            currentDestination = MainScreenDestination.SETTINGS
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }

            if (windowInfo.useNavigationRail) {
                Row(
                    modifier = desktopShortcutModifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    AdaptiveNavigationRailOrSidebar(
                        expandedSidebar = windowInfo.useExpandedSidebar,
                        canToggleSidebar = windowInfo.useExpandedSidebar,
                        sidebarCollapsed = sidebarCollapsed,
                        onToggleSidebar = { sidebarCollapsed = !sidebarCollapsed },
                        currentDestination = currentDestination,
                        selectedTab = selectedTab,
                        savedProgramme = savedProgramme,
                        onSelectTab = { index ->
                            currentDestination = MainScreenDestination.TABS
                            selectedTab = index
                        },
                        onSelectDestination = { dest ->
                            currentDestination = dest
                        },
                        onOpenAcademicProfile = { showSidebarProfileSheet = true }
                    )

                    VerticalDivider(color = navBorder, thickness = 1.dp)

                    BoxWithConstraints(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        val paneWidthDp = maxWidth.value.toInt().coerceAtLeast(1)
                        val paneHeightDp = maxHeight.value.toInt().coerceAtLeast(1)
                        val paneWindow = rememberContainerWindow(paneWidthDp, paneHeightDp)
                        CompositionLocalProvider(LocalLecturesWindow provides paneWindow) {
                            DestinationContent(modifier = Modifier.fillMaxSize())
                        }
                    }
                }
            } else {
                DestinationContent(
                    modifier = desktopShortcutModifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}

