package com.lectures.features.tasks

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.ui.Chrome
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.onEscapeKey
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf

@Composable
fun TasksScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToTimetable: () -> Unit = {},
    onPeekProgramme: (String) -> Unit = {},
    onResetToEnrolled: () -> Unit = {},
    onSetOfficialProgramme: (String) -> Unit = {},
    activeProgrammeOverride: String? = null,
    enrolledProgrammeOverride: String? = null,
    isPeeking: Boolean = false,
    highlightTaskId: Long? = null
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

    val isDark = LocalAppTheme.current.isDark
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val bg = ext.surfaceBase
    val cardBg = ext.surfaceCard
    val textPrimary = ext.textPrimary
    val textSecondary = ext.textSecondary
    val borderColor = ext.borderSubtle
    val primaryColor = MaterialTheme.colorScheme.primary

    val savedProgramme by repository.programmePref.collectAsState(initial = repository.getInitialProgramme())
    val effectiveProgramme = savedProgramme ?: repository.getInitialProgramme() ?: ""
    val studentSchedule by remember(effectiveProgramme) {
        if (effectiveProgramme.isNotBlank()) {
            repository.getLocalSchedule(effectiveProgramme)
        } else {
            flowOf(emptyList())
        }
    }.collectAsState(initial = emptyList())

    val availableCoursePairs = remember(studentSchedule) {
        studentSchedule
            .filter { it.courseCode.isNotBlank() }
            .map { it.courseCode to it.courseTitle }
            .distinctBy { it.first }
    }
    val alarmScheduler = remember(context) { TaskAlarmScheduler(context) }

    val viewModel: TasksViewModel = viewModel(
        factory = TasksViewModel.provideFactory(repository, alarmScheduler)
    )

    val uiState by viewModel.uiState.collectAsState()
    val assignments = uiState.assignments

    LaunchedEffect(uiState.messageSnackbar) {
        if (uiState.messageSnackbar != null) {
            delay(4000L)
            viewModel.clearMessage()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearMessage()
        }
    }

    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var editingTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }
    var deleteTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    val taskToEdit = remember(editingTaskId, assignments) {
        editingTaskId?.let { id -> assignments.firstOrNull { it.id == id } }
    }
    val taskToDelete = remember(deleteTaskId, assignments) {
        deleteTaskId?.let { id -> assignments.firstOrNull { it.id == id } }
    }

    var isNotificationBannerDismissed by rememberSaveable {
        mutableStateOf(NotificationPermissionHelper.isTaskBannerDismissed(context))
    }

    var areNotificationsAllowed by remember {
        mutableStateOf(NotificationPermissionHelper.areNotificationsEnabled(context))
    }
    var isTaskChannelBlocked by remember {
        mutableStateOf(NotificationPermissionHelper.isChannelBlocked(context, NotificationHelper.CHANNEL_ACADEMIC_TASKS))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        areNotificationsAllowed = NotificationPermissionHelper.areNotificationsEnabled(context)
        isTaskChannelBlocked = NotificationPermissionHelper.isChannelBlocked(context, NotificationHelper.CHANNEL_ACADEMIC_TASKS)
        if (areNotificationsAllowed && !isTaskChannelBlocked) {
            isNotificationBannerDismissed = false
            NotificationPermissionHelper.setTaskBannerDismissed(context, false)
        }
    }

    val defaultTaskLeadHours by repository.taskReminderLeadPref.collectAsState(
        initial = repository.getTaskReminderLeadHours()
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        NotificationPermissionHelper.setPromptedNotificationPermission(context, true)
        areNotificationsAllowed = NotificationPermissionHelper.areNotificationsEnabled(context)
        isTaskChannelBlocked = NotificationPermissionHelper.isChannelBlocked(context, NotificationHelper.CHANNEL_ACADEMIC_TASKS)
        if (areNotificationsAllowed && !isTaskChannelBlocked) {
            isNotificationBannerDismissed = false
            NotificationPermissionHelper.setTaskBannerDismissed(context, false)
        }
    }

    val filteredTasks = remember(assignments, uiState.searchQuery, uiState.selectedFilter) {
        assignments.filter { task ->
            val status = getTaskDueStatus(task.dueDate, task.completed)
            val matchesFilter = when (uiState.selectedFilter) {
                "Pending" -> !task.completed
                "Overdue" -> !task.completed && status == TaskDueStatus.OVERDUE
                "Completed" -> task.completed
                "High Priority" -> !task.completed && task.priority.equals("High", ignoreCase = true)
                else -> true
            }
            val query = uiState.searchQuery.trim()
            val matchesSearch = query.isBlank() ||
                task.title.contains(query, ignoreCase = true) ||
                task.courseCode.contains(query, ignoreCase = true) ||
                task.notes.contains(query, ignoreCase = true)
            matchesFilter && matchesSearch
        }.sortedWith(
            compareBy<Assignment> { it.completed }
                .thenByDescending { getTaskDueStatus(it.dueDate, it.completed) == TaskDueStatus.OVERDUE }
                .thenBy { TaskDateTimeParser.calculateDueMillis(it.dueDate) ?: Long.MAX_VALUE }
                .thenByDescending { it.priority.equals("High", ignoreCase = true) }
        )
    }

    val windowInfo = rememberLecturesWindow()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = Chrome.listBottomPadding(navBarBottom, windowInfo.useNavigationRail)
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    val searchFocusRequester = remember { FocusRequester() }
    val primaryInk = contrastOnColor(primaryColor)

    LaunchedEffect(highlightTaskId, filteredTasks, windowInfo.isExtraLargeWidth) {
        val targetId = highlightTaskId ?: return@LaunchedEffect
        val index = filteredTasks.indexOfFirst { it.id == targetId }
        if (index != -1) {
            if (windowInfo.isDualPane && windowInfo.isExtraLargeWidth) {
                gridState.animateScrollToItem((index + 2).coerceAtLeast(0))
            } else {
                listState.animateScrollToItem((index + 3).coerceAtLeast(0))
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = bg,
        modifier = Modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                val modPressed = event.isCtrlPressed || event.isMetaPressed
                when {
                    modPressed && event.key == Key.N -> {
                        editingTaskId = null
                        showAddDialog = true
                        true
                    }
                    modPressed && event.key == Key.F -> {
                        try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                        true
                    }
                    else -> false
                }
            } else false
        },
        floatingActionButton = {
            if (windowInfo.collapseFab) {
                FloatingActionButton(
                    onClick = {
                        editingTaskId = null
                        showAddDialog = true
                    },
                    containerColor = primaryColor,
                    contentColor = primaryInk,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                        .desktopHandCursor()
                        .testTag("add_task_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(24.dp))
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingTaskId = null
                        showAddDialog = true
                    },
                    containerColor = primaryColor,
                    contentColor = primaryInk,
                    shape = RoundedCornerShape(16.dp),
                    icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    text = { Text(text = "Add Task", fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                        .desktopHandCursor()
                        .testTag("add_task_fab")
                )
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
                                modifier = Modifier
                                    .size(48.dp)
                                    .desktopHandCursor()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textPrimary)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TaskAlt, contentDescription = null, tint = primaryInk, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }

                        Column {
                            Text("Tasks & Deadlines", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Spacer(modifier = Modifier.height(2.dp))
                            val pendingCount = assignments.count { !it.completed }
                            Text("$pendingCount pending task${if (pendingCount == 1) "" else "s"}", fontSize = 12.sp, color = textSecondary)
                        }
                    }

                    IconButton(
                        onClick = { showProfileSheet = true },
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                            .testTag("topbar_academic_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Academic Info",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            AnimatedVisibility(
                visible = (!areNotificationsAllowed || isTaskChannelBlocked) && !isNotificationBannerDismissed,
                enter = if (reducedMotion) fadeIn() else expandVertically() + fadeIn(),
                exit = if (reducedMotion) fadeOut() else shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(modifier = Modifier.widthIn(max = windowInfo.maxReadingWidth)) {
                        TasksNotificationPromptBanner(
                            isDark = isDark,
                            areNotificationsAllowed = areNotificationsAllowed,
                            onActionClick = {
                                if (!areNotificationsAllowed) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val activity = context as? Activity
                                        val shouldShowRationale = activity != null &&
                                            ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)
                                        val canLaunchRuntime = !NotificationPermissionHelper.hasPromptedNotificationPermission(context) || shouldShowRationale
                                        if (canLaunchRuntime) {
                                            NotificationPermissionHelper.setPromptedNotificationPermission(context, true)
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            NotificationPermissionHelper.openAppNotificationSettings(context)
                                        }
                                    } else {
                                        NotificationPermissionHelper.openAppNotificationSettings(context)
                                    }
                                } else {
                                    NotificationPermissionHelper.openChannelSettings(context, NotificationHelper.CHANNEL_ACADEMIC_TASKS)
                                }
                            },
                            onDismiss = {
                                isNotificationBannerDismissed = true
                                NotificationPermissionHelper.setTaskBannerDismissed(context, true)
                            }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = uiState.messageSnackbar != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.messageSnackbar?.let { msg ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .widthIn(max = windowInfo.maxReadingWidth)
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDark) Color(0xFF334155) else Color(0xFF1E293B)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(msg, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                IconButton(
                                    onClick = { viewModel.clearMessage() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .desktopHandCursor()
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            AnimatedContent(
                targetState = uiState.isLoading && assignments.isEmpty(),
                transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
                modifier = Modifier.fillMaxSize(),
                label = "tasks_loading_state"
            ) { isInitialLoading ->
                if (isInitialLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(36.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading tasks...", color = textSecondary, fontSize = 14.sp)
                        }
                    }
                } else if (windowInfo.isDualPane) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Row(
                        modifier = Modifier
                            .widthIn(max = windowInfo.maxReadingWidth)
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .widthIn(max = 380.dp)
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 4.dp, bottom = listBottomPadding),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TaskProgressDashboard(
                                assignments = assignments,
                                currentFilter = uiState.selectedFilter,
                                onFilterChange = { viewModel.setFilter(it) }
                            )
                        }

                        if (windowInfo.isExtraLargeWidth) {
                            LazyVerticalGrid(
                                state = gridState,
                                columns = GridCells.Fixed(2),
                                modifier = Modifier
                                    .weight(1.75f)
                                    .fillMaxHeight()
                                    .testTag("tasks_desktop_grid"),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
                            ) {
                                item(key = "search_field_item", span = { GridItemSpan(maxLineSpan) }) {
                                    TasksSearchField(
                                        query = uiState.searchQuery,
                                        isDark = isDark,
                                        primaryColor = primaryColor,
                                        cardBg = cardBg,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        focusRequester = searchFocusRequester,
                                        onQueryChange = { viewModel.setSearchQuery(it) }
                                    )
                                }
                                item(key = "filter_chips_item", span = { GridItemSpan(maxLineSpan) }) {
                                    TasksFilterChipsRow(
                                        assignments = assignments,
                                        selectedFilter = uiState.selectedFilter,
                                        isDark = isDark,
                                        primaryColor = primaryColor,
                                        cardBg = cardBg,
                                        borderColor = borderColor,
                                        textSecondary = textSecondary,
                                        onFilterSelected = { viewModel.setFilter(it) }
                                    )
                                }
                                if (filteredTasks.isEmpty()) {
                                    item(key = "empty_tasks_state_grid", span = { GridItemSpan(maxLineSpan) }) {
                                        TasksEmptyStateCard(
                                            searchQuery = uiState.searchQuery,
                                            selectedFilter = uiState.selectedFilter,
                                            primaryColor = primaryColor,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary
                                        )
                                    }
                                } else {
                                    gridItems(filteredTasks, key = { it.id }) { task ->
                                        TaskCard(
                                            task = task,
                                            isDark = isDark,
                                            isHighlighted = task.id == highlightTaskId,
                                            modifier = Modifier.animateItem(),
                                            onToggleComplete = { viewModel.toggleComplete(task) },
                                            onEdit = {
                                                editingTaskId = task.id
                                                showAddDialog = true
                                            },
                                            onDelete = { deleteTaskId = task.id }
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1.6f)
                                    .fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
                            ) {
                                item(key = "search_field_item") {
                                    TasksSearchField(
                                        query = uiState.searchQuery,
                                        isDark = isDark,
                                        primaryColor = primaryColor,
                                        cardBg = cardBg,
                                        borderColor = borderColor,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        focusRequester = searchFocusRequester,
                                        onQueryChange = { viewModel.setSearchQuery(it) }
                                    )
                                }
                                item(key = "filter_chips_item") {
                                    TasksFilterChipsRow(
                                        assignments = assignments,
                                        selectedFilter = uiState.selectedFilter,
                                        isDark = isDark,
                                        primaryColor = primaryColor,
                                        cardBg = cardBg,
                                        borderColor = borderColor,
                                        textSecondary = textSecondary,
                                        onFilterSelected = { viewModel.setFilter(it) }
                                    )
                                }
                                if (filteredTasks.isEmpty()) {
                                    item(key = "empty_tasks_state_dual") {
                                        TasksEmptyStateCard(
                                            searchQuery = uiState.searchQuery,
                                            selectedFilter = uiState.selectedFilter,
                                            primaryColor = primaryColor,
                                            textPrimary = textPrimary,
                                            textSecondary = textSecondary
                                        )
                                    }
                                } else {
                                    items(filteredTasks, key = { it.id }) { task ->
                                        TaskCard(
                                            task = task,
                                            isDark = isDark,
                                            isHighlighted = task.id == highlightTaskId,
                                            modifier = Modifier.animateItem(),
                                            onToggleComplete = { viewModel.toggleComplete(task) },
                                            onEdit = {
                                                editingTaskId = task.id
                                                showAddDialog = true
                                            },
                                            onDelete = { deleteTaskId = task.id }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .widthIn(max = 760.dp)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = listBottomPadding)
                    ) {
                        item(key = "task_progress_dashboard_item") {
                            TaskProgressDashboard(
                                assignments = assignments,
                                currentFilter = uiState.selectedFilter,
                                onFilterChange = { viewModel.setFilter(it) }
                            )
                        }
                        item(key = "search_field_item") {
                            TasksSearchField(
                                query = uiState.searchQuery,
                                isDark = isDark,
                                primaryColor = primaryColor,
                                cardBg = cardBg,
                                borderColor = borderColor,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                focusRequester = searchFocusRequester,
                                onQueryChange = { viewModel.setSearchQuery(it) }
                            )
                        }
                        item(key = "filter_chips_item") {
                            TasksFilterChipsRow(
                                assignments = assignments,
                                selectedFilter = uiState.selectedFilter,
                                isDark = isDark,
                                primaryColor = primaryColor,
                                cardBg = cardBg,
                                borderColor = borderColor,
                                textSecondary = textSecondary,
                                onFilterSelected = { viewModel.setFilter(it) }
                            )
                        }
                        if (filteredTasks.isEmpty()) {
                            item(key = "empty_tasks_state") {
                                TasksEmptyStateCard(
                                    searchQuery = uiState.searchQuery,
                                    selectedFilter = uiState.selectedFilter,
                                    primaryColor = primaryColor,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary
                                )
                            }
                        } else {
                            items(filteredTasks, key = { it.id }) { task ->
                                TaskCard(
                                    task = task,
                                    isDark = isDark,
                                    isHighlighted = task.id == highlightTaskId,
                                    modifier = Modifier.animateItem(),
                                    onToggleComplete = { viewModel.toggleComplete(task) },
                                    onEdit = {
                                        editingTaskId = task.id
                                        showAddDialog = true
                                    },
                                    onDelete = { deleteTaskId = task.id }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            isDark = isDark,
            initialTask = taskToEdit,
            availableCourses = availableCoursePairs,
            defaultReminderMinutes = defaultTaskLeadHours * 60,
            onDismiss = {
                showAddDialog = false
                editingTaskId = null
            },
            onConfirm = { title, course, dueDate, priority, reminderMinutes, notes, category ->
                val fullTitle = if (category != TaskCategory.GENERAL) {
                    "[${category.displayName}] ${title.trim()}"
                } else {
                    title.trim()
                }
                val currentTask = taskToEdit
                if (currentTask != null) {
                    viewModel.updateAssignment(
                        currentTask.copy(
                            title = fullTitle,
                            courseCode = course,
                            dueDate = dueDate,
                            priority = priority,
                            reminderMinutes = reminderMinutes,
                            notes = notes
                        )
                    )
                } else {
                    viewModel.saveAssignment(
                        title = fullTitle,
                        courseCode = course,
                        dueDate = dueDate,
                        priority = priority,
                        reminderMinutes = reminderMinutes,
                        notes = notes
                    )
                }
                showAddDialog = false
                editingTaskId = null
            }
        )
    }

    if (taskToDelete != null) {
        val task = taskToDelete
        AlertDialog(
            onDismissRequest = { deleteTaskId = null },
            properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
            modifier = Modifier
                .then(
                    if (windowInfo.preferDesktopDialogs) {
                        Modifier.widthIn(min = 360.dp, max = 480.dp)
                    } else {
                        Modifier
                    }
                )
                .onEscapeKey { deleteTaskId = null },
            containerColor = ext.surfaceCard,
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Delete Task?", fontWeight = FontWeight.Bold, color = textPrimary) },
            text = {
                Text(
                    "Are you sure you want to delete '${task.title}'? Any scheduled AlarmManager reminders for this task will be cancelled.",
                    color = textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAssignment(task.id)
                        deleteTaskId = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteTaskId = null },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text("Cancel", color = textSecondary)
                }
            }
        )
    }

    if (showProfileSheet) {
        val enrolledProg = enrolledProgrammeOverride?.takeIf { it.isNotBlank() }
            ?: (savedProgramme ?: repository.getInitialProgramme() ?: "")
        val activeProg = activeProgrammeOverride?.takeIf { it.isNotBlank() }
            ?: (savedProgramme ?: "")
        AcademicProfileSheet(
            programme = activeProg,
            enrolledProgramme = enrolledProg,
            isPeeking = isPeeking,
            onDismiss = { showProfileSheet = false },
            onPeekProgramme = { peekGroup ->
                showProfileSheet = false
                onPeekProgramme(peekGroup)
                onNavigateToTimetable()
            },
            onResetToEnrolled = {
                showProfileSheet = false
                onResetToEnrolled()
                onNavigateToTimetable()
            },
            onSetOfficialProgramme = { newGroup ->
                showProfileSheet = false
                onSetOfficialProgramme(newGroup)
                onNavigateToTimetable()
            },
            onReconfigureProfile = {
                showProfileSheet = false
                onReconfigureAcademicProfile()
            },
            onOpenSettings = {
                showProfileSheet = false
                onSettingsClick()
            },
            onNavigateToTimetable = {
                showProfileSheet = false
                onNavigateToTimetable()
            }
        )
    }
}
