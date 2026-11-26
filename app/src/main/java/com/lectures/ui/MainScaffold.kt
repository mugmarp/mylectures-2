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
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.lectures.features.timetable.ui.TimetableRoute
import com.lectures.features.timetable.ui.WelcomeScreen
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.AdaptiveNavigationRailOrSidebar
import com.lectures.ui.components.desktopHandCursor
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

    val windowInfo = rememberLecturesWindow()
    val reducedMotion = rememberPrefersReducedMotion()

    fun AppNavState.depth(): Int = when (this) {
        is AppNavState.Welcome -> 0
        is AppNavState.FacultySelection -> 1
        is AppNavState.ProgrammeSelection -> 2
        is AppNavState.ClassGroupSelection -> 3
        is AppNavState.MainApp -> 4
    }

    AnimatedContent(
        targetState = navState,
        contentKey = { it::class },
        transitionSpec = {
            MotionSpec.directionalSlideTransform(
                forward = targetState.depth() >= initialState.depth(),
                reducedMotion = reducedMotion,
                useFadeOnly = windowInfo.useNavigationRail
            )
        },
        label = "OnboardingAndMainNav"
    ) { state ->
        when (state) {
            is AppNavState.Welcome -> {
                WelcomeScreen(
                    onGetStarted = { navState = AppNavState.FacultySelection }
                )
            }
            is AppNavState.FacultySelection -> {
                BackHandler {
                    navState = if (onboardingCompleted) AppNavState.MainApp else AppNavState.Welcome
                }
                FacultySelectionScreen(
                    onSelectFaculty = { faculty -> navState = AppNavState.ProgrammeSelection(faculty) },
                    onBack = {
                        navState = if (onboardingCompleted) AppNavState.MainApp else AppNavState.Welcome
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

            val ext = LocalExtendedColors.current
            val navBg = ext.surfaceCard
            val navBorder = ext.borderSubtle
            val primaryBlue = MaterialTheme.colorScheme.primary
            val textMuted = ext.textMuted

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

            val timetableUiState by timetableViewModel.uiState.collectAsState()
            val activeProgramme = timetableUiState.programme.ifBlank { savedProgramme ?: "" }
            val enrolledProgramme = timetableUiState.enrolledProgramme.ifBlank { savedProgramme ?: "" }
            val isPeekingProgramme = timetableUiState.isPeeking

            LaunchedEffect(currentDestination, selectedTab) {
                if (!(currentDestination == MainScreenDestination.TABS && selectedTab == 2) && isNotesEditing) {
                    isNotesEditing = false
                }
            }

            val navigateToTabSafely: (Int) -> Unit = { targetTab ->
                if (isNotesEditing && currentDestination == MainScreenDestination.TABS && selectedTab == 2 && targetTab != 2) {
                    android.widget.Toast.makeText(
                        context,
                        "Save or close your active note first",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    currentDestination = MainScreenDestination.TABS
                    selectedTab = targetTab
                }
            }
            val navigateToDestinationSafely: (MainScreenDestination) -> Unit = { targetDest ->
                if (isNotesEditing && currentDestination == MainScreenDestination.TABS && selectedTab == 2 && targetDest != MainScreenDestination.TABS) {
                    android.widget.Toast.makeText(
                        context,
                        "Save or close your active note first",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    currentDestination = targetDest
                }
            }

            val onPeekProgrammeAndGoToTimetable: (String) -> Unit = { group ->
                timetableViewModel.peekProgramme(group)
                currentDestination = MainScreenDestination.TABS
                selectedTab = 0
            }
            val onResetToEnrolledAndGoToTimetable: () -> Unit = {
                timetableViewModel.resetToEnrolledProgramme()
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
                    programme = activeProgramme,
                    enrolledProgramme = enrolledProgramme,
                    isPeeking = isPeekingProgramme,
                    onDismiss = { showSidebarProfileSheet = false },
                    onPeekProgramme = onPeekProgrammeAndGoToTimetable,
                    onResetToEnrolled = onResetToEnrolledAndGoToTimetable,
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
                    enter = if (reducedMotion) {
                        fadeIn(animationSpec = tween(0))
                    } else {
                        slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
                        ) + fadeIn()
                    },
                    exit = if (reducedMotion) {
                        fadeOut(animationSpec = tween(0))
                    } else {
                        slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(180)
                        ) + fadeOut()
                    },
                    modifier = modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .zIndex(100f)
                ) {
                    activeForegroundAlert?.let { alert ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = ext.surfaceCard,
                            border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.4f)),
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .widthIn(max = 520.dp)
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .desktopHandCursor()
                                .clickable {
                                    val targetTab = alert.navTab
                                    if (targetTab != null) {
                                        navigateToTabSafely(targetTab)
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
                                    onClick = { activeForegroundAlert = null },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .desktopHandCursor()
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
                            if (reducedMotion || windowInfo.useNavigationRail) {
                                MotionSpec.fadeTransform(
                                    enterMs = MotionSpec.TAB_FADE_IN_MS,
                                    exitMs = MotionSpec.TAB_FADE_OUT_MS,
                                    reducedMotion = reducedMotion
                                )
                            } else {
                                val isForward = targetState != MainScreenDestination.TABS
                                MotionSpec.directionalSlideTransform(
                                    forward = isForward,
                                    reducedMotion = false,
                                    useFadeOnly = false
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
                                            if (reducedMotion || windowInfo.useNavigationRail) {
                                                MotionSpec.fadeTransform(
                                                    enterMs = MotionSpec.TAB_FADE_IN_MS,
                                                    exitMs = MotionSpec.TAB_FADE_OUT_MS,
                                                    reducedMotion = reducedMotion
                                                )
                                            } else {
                                                val direction = if (targetState > initialState) 1 else -1
                                                (slideInHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (direction * width * 0.14f).toInt() } +
                                                        fadeIn(animationSpec = tween(180)))
                                                    .togetherWith(
                                                        slideOutHorizontally(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { width -> (-direction * width * 0.14f).toInt() } +
                                                                fadeOut(animationSpec = tween(120))
                                                    )
                                            }
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
                                                    onResetToEnrolled = { timetableViewModel.resetToEnrolledProgramme() },
                                                    onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                                    activeProgrammeOverride = activeProgramme,
                                                    enrolledProgrammeOverride = enrolledProgramme,
                                                    isPeeking = isPeekingProgramme
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
                                                onResetToEnrolled = { timetableViewModel.resetToEnrolledProgramme() },
                                                onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                                activeProgrammeOverride = activeProgramme,
                                                enrolledProgrammeOverride = enrolledProgramme,
                                                isPeeking = isPeekingProgramme,
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
                                                onResetToEnrolled = { timetableViewModel.resetToEnrolledProgramme() },
                                                onSetOfficialProgramme = onSetOfficialProgrammeAndGoToTimetable,
                                                activeProgrammeOverride = activeProgramme,
                                                enrolledProgrammeOverride = enrolledProgramme,
                                                isPeeking = isPeekingProgramme,
                                                highlightTaskId = pendingHighlightTaskId
                                            )
                                        }
                                    }

                                    // Mobile Floating Pill Dock: Rendered exclusively on phones when rail is inactive
                                    AnimatedVisibility(
                                        visible = !windowInfo.useNavigationRail && !isNotesEditing && !windowInfo.isImeVisible,
                                        enter = if (reducedMotion) EnterTransition.None else {
                                            slideInVertically(
                                                initialOffsetY = { it },
                                                animationSpec = MotionSpec.standardSpring()
                                            ) + fadeIn(animationSpec = tween(180))
                                        },
                                        exit = if (reducedMotion) ExitTransition.None else {
                                            slideOutVertically(
                                                targetOffsetY = { it },
                                                animationSpec = tween(140)
                                            ) + fadeOut(animationSpec = tween(120))
                                        },
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(start = Chrome.DockSideInset, end = Chrome.DockSideInset, bottom = Chrome.DockBottomGap)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth(),
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
                                                            animationSpec = MotionSpec.stateTween(180, reducedMotion),
                                                            label = "pill_color"
                                                        )
                                                        val iconScale by animateFloatAsState(
                                                            targetValue = if (isSelected && !reducedMotion) 1.08f else 1.0f,
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
                                                                    navigateToTabSafely(index)
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
                val cmd = (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) &&
                    !keyEvent.isAltPressed && !keyEvent.isShiftPressed
                if (cmd) {
                    when (keyEvent.key) {
                        Key.One -> {
                            navigateToTabSafely(0)
                            true
                        }
                        Key.Two -> {
                            navigateToTabSafely(1)
                            true
                        }
                        Key.Three -> {
                            navigateToTabSafely(2)
                            true
                        }
                        Key.Four -> {
                            navigateToTabSafely(3)
                            true
                        }
                        Key.Five -> {
                            navigateToDestinationSafely(MainScreenDestination.VACANT_ROOMS)
                            true
                        }
                        Key.Six -> {
                            navigateToDestinationSafely(MainScreenDestination.ACCOUNT)
                            true
                        }
                        Key.Comma -> {
                            navigateToDestinationSafely(MainScreenDestination.SETTINGS)
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
                        savedProgramme = activeProgramme,
                        isPeeking = isPeekingProgramme,
                        onSelectTab = navigateToTabSafely,
                        onSelectDestination = navigateToDestinationSafely,
                        onOpenAcademicProfile = {
                            if (isNotesEditing && currentDestination == MainScreenDestination.TABS && selectedTab == 2) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Save or close your active note first",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                showSidebarProfileSheet = true
                            }
                        }
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
}

