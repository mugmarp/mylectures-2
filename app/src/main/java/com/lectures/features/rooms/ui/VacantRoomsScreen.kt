package com.lectures.features.rooms.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.lectures.TimetableApplication
import com.lectures.core.util.TimeUtil
import com.lectures.features.rooms.BuildingLevel
import com.lectures.features.rooms.RoomType
import com.lectures.features.rooms.SuggestionValue
import com.lectures.features.rooms.UniversityDirectory
import com.lectures.features.timetable.ui.*
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.DedicatedDayPickerDialog
import com.lectures.ui.components.DedicatedTimePickerDialog
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import java.util.Calendar
import java.util.Locale

/**
 * Campus Vacant Room Finder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VacantRoomsScreen(
    onBack: (() -> Unit)? = null,
    enrolledProgramme: String? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as? TimetableApplication
    val repository = app?.repository

    val customEvents by (repository?.getCustomEvents() ?: kotlinx.coroutines.flow.flowOf(emptyList()))
        .collectAsState(initial = emptyList())

    val isDark = LocalAppTheme.current.isDark
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val bgColor = if (isDark) DarkSurfaceBase else SurfaceBaseLight
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textMuted = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val borderSubtle = if (isDark) DarkBorderSubtle else BorderSubtleLight

    // Current real-time clock defaults
    val calendar = remember { Calendar.getInstance() }
    val initialDay = remember { TimeUtil.todayName() }
    val initialHour = remember { calendar.get(Calendar.HOUR_OF_DAY) }
    val initialMinute = remember { calendar.get(Calendar.MINUTE) }
    val initialTimeStr = remember { String.format(Locale.US, "%02d:%02d", initialHour, initialMinute) }

    // Active Query Parameters
    var queryDay by rememberSaveable { mutableStateOf(initialDay) }
    var queryTime by rememberSaveable { mutableStateOf(initialTimeStr) }
    var isLiveNow by rememberSaveable { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, isLiveNow) {
        if (isLiveNow) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val nowCal = Calendar.getInstance()
                    queryDay = TimeUtil.todayName()
                    queryTime = String.format(
                        Locale.US,
                        "%02d:%02d",
                        nowCal.get(Calendar.HOUR_OF_DAY),
                        nowCal.get(Calendar.MINUTE)
                    )
                    kotlinx.coroutines.delay(60_000L)
                }
            }
        }
    }

    val allEntries by remember(queryDay) {
        app?.database?.timetableDao()?.getScheduleForDay(queryDay)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())

    val activeProgramme = enrolledProgramme ?: repository?.getInitialProgramme()
    val defaultScope = remember(activeProgramme) {
        programmeDefaults(activeProgramme)
    }

    var selectedCampus by rememberSaveable(defaultScope.campus, defaultScope.buildingCode) {
        mutableStateOf<String?>(defaultScope.campus)
    }
    var selectedBuilding by rememberSaveable(defaultScope.campus, defaultScope.buildingCode) {
        mutableStateOf<String?>(defaultScope.buildingCode)
    }

    // Filters
    var minGapMinutes by rememberSaveable { mutableIntStateOf(30) }
    var selectedLevel by rememberSaveable { mutableStateOf<BuildingLevel?>(null) }
    var studyFriendlyOnly by rememberSaveable { mutableStateOf(true) }
    var showPermanentlyVacant by rememberSaveable { mutableStateOf(true) }
    var hideLibrary by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var freeOnly by rememberSaveable { mutableStateOf(false) }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var isDefaultResetDismissed by rememberSaveable(defaultScope.campus, defaultScope.buildingCode) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        app?.analyticsManager?.trackFeature("vacant_rooms_search")
    }

    // Pickers visibility
    var campusDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var buildingDropdownExpanded by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDayPicker by rememberSaveable { mutableStateOf(false) }
    var selectedRoomIdForSchedule by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedCampus) {
        if (selectedBuilding != null) {
            val b = UniversityDirectory.BUILDINGS.firstOrNull { it.code == selectedBuilding }
            if (b != null && !b.campus.equals(selectedCampus, ignoreCase = true)) {
                selectedBuilding = UniversityDirectory.buildingsForCampus(selectedCampus).firstOrNull()?.code
            }
        }
    }

    LaunchedEffect(selectedCampus, selectedBuilding) {
        if (selectedCampus == defaultScope.campus && selectedBuilding == defaultScope.buildingCode) {
            isDefaultResetDismissed = false
        }
    }

    val campusBuildings = remember(selectedCampus) {
        UniversityDirectory.buildingsForCampus(selectedCampus)
    }

    val vacancyStatuses = remember(
        allEntries, customEvents, queryDay, queryTime, minGapMinutes,
        selectedCampus, selectedBuilding
    ) {
        UniversityDirectory.calculateRoomVacancy(
            allEntries = allEntries,
            dayOfWeek = queryDay,
            queryTimeStr = queryTime,
            minGapMinutes = minGapMinutes,
            customEvents = customEvents,
            campusFilter = selectedCampus,
            buildingFilter = selectedBuilding
        )
    }

    val hasLibraryInScope = remember(vacancyStatuses) {
        vacancyStatuses.any { it.room.type == RoomType.LIBRARY || it.room.suggestionValue == SuggestionValue.LIBRARY }
    }
    val categories = remember(hasLibraryInScope) {
        if (hasLibraryInScope) listOf("All", "Room", "Lab", "Library") else listOf("All", "Room", "Lab")
    }

    LaunchedEffect(hasLibraryInScope) {
        if (!hasLibraryInScope && selectedCategory == "Library") {
            selectedCategory = "All"
        }
    }

    val filteredRooms = remember(
        vacancyStatuses, selectedLevel, studyFriendlyOnly, minGapMinutes,
        searchQuery, showPermanentlyVacant, hideLibrary, freeOnly, selectedCategory
    ) {
        val base = vacancyStatuses.filter { status ->
            val matchesFree = !freeOnly || (!status.isOccupied && (status.gapMinutes >= minGapMinutes || status.freeUntil == "Rest of day"))
            val matchesLevel = selectedLevel == null || status.room.level == selectedLevel
            val matchesStudy = !studyFriendlyOnly || status.room.isStudyFriendly
            val matchesSearch = searchQuery.isBlank() ||
                status.room.name.contains(searchQuery, ignoreCase = true) ||
                status.room.code.contains(searchQuery, ignoreCase = true) ||
                status.room.aliases.any { it.contains(searchQuery, ignoreCase = true) } ||
                (status.currentSession?.courseCode?.contains(searchQuery, ignoreCase = true) == true) ||
                (status.currentSession?.courseTitle?.contains(searchQuery, ignoreCase = true) == true) ||
                (status.currentSession?.programmeGroup?.contains(searchQuery, ignoreCase = true) == true) ||
                (status.nextSession?.courseCode?.contains(searchQuery, ignoreCase = true) == true) ||
                (status.nextSession?.courseTitle?.contains(searchQuery, ignoreCase = true) == true) ||
                (status.nextSession?.programmeGroup?.contains(searchQuery, ignoreCase = true) == true)

            val matchesCategory = when (selectedCategory) {
                "Room" -> status.room.type == RoomType.CLASSROOM
                "Lab" -> status.room.type == RoomType.COMPUTER_LAB || status.room.type == RoomType.SCIENCE_LAB
                "Library" -> status.room.type == RoomType.LIBRARY || status.room.suggestionValue == SuggestionValue.LIBRARY
                else -> true
            }

            val matchesVacantFlag = showPermanentlyVacant || !status.room.isPermanentlyVacant

            val matchesGap = if (!status.isOccupied && minGapMinutes > 0 && studyFriendlyOnly) {
                status.gapMinutes >= minGapMinutes || status.freeUntil == "Rest of day"
            } else true

            matchesFree && matchesLevel && matchesStudy && matchesSearch && matchesCategory && matchesGap && matchesVacantFlag
        }
        base
            .filter { !hideLibrary || it.room.suggestionValue != SuggestionValue.LIBRARY }
            .sortedWith(
                compareBy(
                    { if (it.isOccupied) 1 else 0 },
                    { if (it.freeUntil == "Rest of day") -99999 else -it.gapMinutes },
                    { it.room.suggestionValue.suggestionRank },
                    { it.room.code }
                )
            )
    }

    val freeCount = vacancyStatuses.count {
        !it.isOccupied && (it.gapMinutes >= minGapMinutes || it.freeUntil == "Rest of day") && it.room.isStudyFriendly
    }
    val occupiedCount = vacancyStatuses.count { it.isOccupied && it.room.isStudyFriendly }
    val windowInfo = rememberLecturesWindow()
    val showDesktopSplit = windowInfo.isWideWorkspace
    val accentColor = MaterialTheme.colorScheme.primary
    val selectedRoomForSchedule = remember(selectedRoomIdForSchedule, vacancyStatuses) {
        selectedRoomIdForSchedule?.let { id -> vacancyStatuses.firstOrNull { it.room.id == id } }
    }

    val resetAllFilters: () -> Unit = {
        val now = Calendar.getInstance()
        queryDay = TimeUtil.todayName()
        queryTime = String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
        isLiveNow = true
        minGapMinutes = 30
        selectedLevel = null
        studyFriendlyOnly = true
        showPermanentlyVacant = true
        hideLibrary = false
        freeOnly = false
        selectedCategory = "All"
    }

    Scaffold(
        modifier = Modifier.onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && selectedRoomIdForSchedule != null) {
                selectedRoomIdForSchedule = null
                true
            } else false
        },
        containerColor = bgColor,
        topBar = {
            FloatingIslandTopBar {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (onBack == null) 16.dp else 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onBack != null) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.desktopHandCursor()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(accentColor, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MeetingRoom,
                                contentDescription = null,
                                tint = contrastOnColor(accentColor),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Vacant Room Finder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = textPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(StatusGreenLive.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${vacancyStatuses.size} rooms",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusGreenLive
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isLiveNow) StatusGreenLive.copy(alpha = 0.12f) else accentColor.copy(alpha = 0.12f),
                            modifier = Modifier
                                .desktopHandCursor()
                                .clickable {
                                    if (showDesktopSplit) {
                                        showTimePicker = true
                                    } else {
                                        showFilterSheet = true
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(if (isLiveNow) StatusGreenLive else accentColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (isLiveNow) "Live Now · $queryDay $queryTime" else "$queryDay · $queryTime",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLiveNow) (if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)) else accentColor
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Open Filter and Schedule Options",
                                    tint = if (isLiveNow) (if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)) else accentColor,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                    if (!isLiveNow) {
                        IconButton(
                            onClick = {
                                val nowCal = Calendar.getInstance()
                                queryDay = TimeUtil.todayName()
                                queryTime = String.format(
                                    Locale.US,
                                    "%02d:%02d",
                                    nowCal.get(Calendar.HOUR_OF_DAY),
                                    nowCal.get(Calendar.MINUTE)
                                )
                                isLiveNow = true
                            },
                            modifier = Modifier.desktopHandCursor()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "Reset to current live time",
                                tint = accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (!showDesktopSplit) {
                        IconButton(
                            onClick = { showFilterSheet = true },
                            modifier = Modifier.desktopHandCursor()
                        ) {
                            BadgedBox(
                                badge = {
                                    if (!isLiveNow || minGapMinutes != 30 || selectedLevel != null || !studyFriendlyOnly || freeOnly) {
                                        Badge(containerColor = accentColor)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Tune,
                                    contentDescription = "Filter & Schedule Options",
                                    tint = if (!isLiveNow || minGapMinutes != 30 || selectedLevel != null || freeOnly) accentColor else textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (showDesktopSplit) {
            val containerMaxWidth = if (windowInfo.isExtraLargeWidth) 1560.dp else windowInfo.maxReadingWidth
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = containerMaxWidth)
                        .fillMaxSize()
                        .padding(
                            top = padding.calculateTopPadding() + 8.dp,
                            start = 16.dp,
                            end = 16.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ZONE 1: Persistent Left Filter Rail
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = surfaceColor,
                        border = BorderStroke(1.dp, borderSubtle),
                        modifier = Modifier
                            .width(268.dp)
                            .fillMaxHeight()
                            .padding(bottom = padding.calculateBottomPadding() + 16.dp)
                            .testTag("vacant_rooms_filter_rail")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            VacantRoomsFilterOptionsContent(
                                queryDay = queryDay,
                                queryTime = queryTime,
                                isLiveNow = isLiveNow,
                                minGapMinutes = minGapMinutes,
                                selectedLevel = selectedLevel,
                                studyFriendlyOnly = studyFriendlyOnly,
                                hideLibrary = hideLibrary,
                                isDark = isDark,
                                showApplyButton = false,
                                onDismiss = {},
                                onResetAll = resetAllFilters,
                                onSelectLiveNow = {
                                    val now = Calendar.getInstance()
                                    queryDay = TimeUtil.todayName()
                                    queryTime = String.format(
                                        Locale.US,
                                        "%02d:%02d",
                                        now.get(Calendar.HOUR_OF_DAY),
                                        now.get(Calendar.MINUTE)
                                    )
                                    isLiveNow = true
                                },
                                onOpenDayPicker = { showDayPicker = true },
                                onOpenTimePicker = {
                                    isLiveNow = false
                                    showTimePicker = true
                                },
                                onPresetTimeSelected = { preset ->
                                    queryTime = preset
                                    isLiveNow = false
                                },
                                onMinGapSelected = { minGapMinutes = it },
                                onLevelSelected = { selectedLevel = it },
                                onToggleStudyFriendly = { studyFriendlyOnly = !studyFriendlyOnly },
                                onToggleHideLibrary = { hideLibrary = !hideLibrary }
                            )
                        }
                    }

                    // ZONE 2: Center Search, Chips & Adaptive Multi-Column Room Grid
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .testTag("vacant_rooms_grid_pane")
                    ) {
                        VacantRoomsSearchAndFilters(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedCampus = selectedCampus,
                            selectedBuilding = selectedBuilding,
                            defaultScope = defaultScope,
                            campusBuildings = campusBuildings,
                            campusDropdownExpanded = campusDropdownExpanded,
                            onCampusDropdownExpandedChange = { campusDropdownExpanded = it },
                            buildingDropdownExpanded = buildingDropdownExpanded,
                            onBuildingDropdownExpandedChange = { buildingDropdownExpanded = it },
                            onCampusSelected = { campus, building ->
                                selectedCampus = campus
                                selectedBuilding = building
                            },
                            onBuildingSelected = { building, campus ->
                                selectedBuilding = building
                                if (campus != null) selectedCampus = campus
                            },
                            freeOnly = freeOnly,
                            onToggleFreeOnly = { freeOnly = !freeOnly },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            isDefaultResetDismissed = isDefaultResetDismissed,
                            onResetToDefaultScope = {
                                selectedCampus = defaultScope.campus
                                selectedBuilding = defaultScope.buildingCode
                            },
                            isDark = isDark,
                            isNarrow = false
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        VacantRoomsStatusSummaryRow(
                            isLiveNow = isLiveNow,
                            queryDay = queryDay,
                            queryTime = queryTime,
                            freeCount = freeCount,
                            occupiedCount = occupiedCount,
                            selectedBuilding = selectedBuilding,
                            selectedCampus = selectedCampus,
                            isDark = isDark,
                            textMuted = textMuted
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 250.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (filteredRooms.isEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "empty_rooms_desktop") {
                                    VacantRoomsEmptyStateCard(
                                        surfaceColor = surfaceColor,
                                        borderSubtle = borderSubtle,
                                        textPrimary = textPrimary,
                                        textMuted = textMuted,
                                        hasActiveFilters = searchQuery.isNotBlank() || freeOnly || selectedLevel != null,
                                        onClearFilters = {
                                            searchQuery = ""
                                            freeOnly = false
                                            selectedLevel = null
                                        }
                                    )
                                }
                            } else {
                                items(filteredRooms, key = { it.room.id }) { status ->
                                    RoomVacancyCard(
                                        status = status,
                                        isDark = isDark,
                                        showCampus = selectedCampus == null,
                                        selectedBuilding = selectedBuilding,
                                        isSelected = status.room.id == selectedRoomIdForSchedule,
                                        modifier = Modifier.animateItem(),
                                        onClick = { selectedRoomIdForSchedule = status.room.id }
                                    )
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }, key = "data_provenance_notice_desktop") {
                                VacantRoomsProvenanceNotice(
                                    surfaceColor = surfaceColor,
                                    borderSubtle = borderSubtle,
                                    accentColor = accentColor,
                                    textMuted = textMuted
                                )
                            }
                        }
                    }

                    // ZONE 3: Docked Right Room Schedule Inspector
                    if (windowInfo.isWideWorkspace || selectedRoomForSchedule != null) {
                        val inspectorWidth = when {
                            windowInfo.isExtraLargeWidth -> 360.dp
                            windowInfo.isWideWorkspace -> 320.dp
                            else -> 300.dp
                        }
                        Box(
                            modifier = Modifier
                                .width(inspectorWidth)
                                .fillMaxHeight()
                                .padding(bottom = padding.calculateBottomPadding() + 16.dp)
                                .testTag("vacant_rooms_inspector_pane")
                        ) {
                            if (selectedRoomForSchedule != null) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = surfaceColor,
                                    border = BorderStroke(1.dp, borderSubtle),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    RoomScheduleInspectorContent(
                                        status = selectedRoomForSchedule,
                                        dayOfWeek = queryDay,
                                        isDark = isDark,
                                        showCloseButton = false,
                                        onDismiss = { selectedRoomIdForSchedule = null },
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(18.dp)
                                    )
                                }
                            } else {
                                EmptyRoomInspectorCard(isDark = isDark)
                            }
                        }
                    }
                }
            }
        } else {
            // Phones (< 600dp) and Medium/Compact Landscape Tablets (< 1000dp)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                LazyVerticalGrid(
                    columns = if (windowInfo.isTablet) GridCells.Adaptive(minSize = 265.dp) else GridCells.Fixed(1),
                    modifier = Modifier
                        .widthIn(max = windowInfo.maxReadingWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = padding.calculateTopPadding() + 6.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "search_and_quick_filters") {
                        VacantRoomsSearchAndFilters(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedCampus = selectedCampus,
                            selectedBuilding = selectedBuilding,
                            defaultScope = defaultScope,
                            campusBuildings = campusBuildings,
                            campusDropdownExpanded = campusDropdownExpanded,
                            onCampusDropdownExpandedChange = { campusDropdownExpanded = it },
                            buildingDropdownExpanded = buildingDropdownExpanded,
                            onBuildingDropdownExpandedChange = { buildingDropdownExpanded = it },
                            onCampusSelected = { campus, building ->
                                selectedCampus = campus
                                selectedBuilding = building
                            },
                            onBuildingSelected = { building, campus ->
                                selectedBuilding = building
                                if (campus != null) selectedCampus = campus
                            },
                            freeOnly = freeOnly,
                            onToggleFreeOnly = { freeOnly = !freeOnly },
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { selectedCategory = it },
                            isDefaultResetDismissed = isDefaultResetDismissed,
                            onResetToDefaultScope = {
                                selectedCampus = defaultScope.campus
                                selectedBuilding = defaultScope.buildingCode
                            },
                            isDark = isDark,
                            isNarrow = windowInfo.isNarrow
                        )
                    }

                    item(span = { GridItemSpan(maxLineSpan) }, key = "status_summary_row") {
                        VacantRoomsStatusSummaryRow(
                            isLiveNow = isLiveNow,
                            queryDay = queryDay,
                            queryTime = queryTime,
                            freeCount = freeCount,
                            occupiedCount = occupiedCount,
                            selectedBuilding = selectedBuilding,
                            selectedCampus = selectedCampus,
                            isDark = isDark,
                            textMuted = textMuted
                        )
                    }

                    if (filteredRooms.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }, key = "empty_rooms") {
                            VacantRoomsEmptyStateCard(
                                surfaceColor = surfaceColor,
                                borderSubtle = borderSubtle,
                                textPrimary = textPrimary,
                                textMuted = textMuted,
                                hasActiveFilters = searchQuery.isNotBlank() || freeOnly || selectedLevel != null,
                                onClearFilters = {
                                    searchQuery = ""
                                    freeOnly = false
                                    selectedLevel = null
                                }
                            )
                        }
                    } else {
                        items(filteredRooms, key = { it.room.id }) { status ->
                            RoomVacancyCard(
                                status = status,
                                isDark = isDark,
                                showCampus = selectedCampus == null,
                                selectedBuilding = selectedBuilding,
                                isSelected = status.room.id == selectedRoomIdForSchedule,
                                modifier = Modifier.animateItem(),
                                onClick = { selectedRoomIdForSchedule = status.room.id }
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }, key = "data_provenance_notice") {
                        Column {
                            VacantRoomsProvenanceNotice(
                                surfaceColor = surfaceColor,
                                borderSubtle = borderSubtle,
                                accentColor = accentColor,
                                textMuted = textMuted
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet && !showDesktopSplit) {
        VacantRoomsFilterSheet(
            queryDay = queryDay,
            queryTime = queryTime,
            isLiveNow = isLiveNow,
            minGapMinutes = minGapMinutes,
            selectedLevel = selectedLevel,
            studyFriendlyOnly = studyFriendlyOnly,
            hideLibrary = hideLibrary,
            isDark = isDark,
            onDismiss = { showFilterSheet = false },
            onResetAll = resetAllFilters,
            onSelectLiveNow = {
                val now = Calendar.getInstance()
                queryDay = TimeUtil.todayName()
                queryTime = String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))
                isLiveNow = true
            },
            onOpenDayPicker = { showDayPicker = true },
            onOpenTimePicker = {
                isLiveNow = false
                showTimePicker = true
            },
            onPresetTimeSelected = { preset ->
                queryTime = preset
                isLiveNow = false
            },
            onMinGapSelected = { minGapMinutes = it },
            onLevelSelected = { selectedLevel = it },
            onToggleStudyFriendly = { studyFriendlyOnly = !studyFriendlyOnly },
            onToggleHideLibrary = { hideLibrary = !hideLibrary }
        )
    }

    if (showTimePicker) {
        DedicatedTimePickerDialog(
            initialTime = queryTime,
            title = "Set Vacancy Query Time",
            onTimeSelected = { newTime ->
                queryTime = newTime
                isLiveNow = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showDayPicker) {
        DedicatedDayPickerDialog(
            selectedDay = queryDay,
            onDaySelected = { newDay ->
                queryDay = newDay
                isLiveNow = false
            },
            onDismiss = { showDayPicker = false }
        )
    }

    if (!showDesktopSplit) {
        selectedRoomForSchedule?.let { status ->
            RoomScheduleDetailDialog(
                status = status,
                dayOfWeek = queryDay,
                isDark = isDark,
                onDismiss = { selectedRoomIdForSchedule = null }
            )
        }
    }
}

@Composable
private fun VacantRoomsStatusSummaryRow(
    isLiveNow: Boolean,
    queryDay: String,
    queryTime: String,
    freeCount: Int,
    occupiedCount: Int,
    selectedBuilding: String?,
    selectedCampus: String?,
    isDark: Boolean,
    textMuted: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(if (isLiveNow) StatusGreenLive else Color(0xFFF59E0B), CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isLiveNow) "$freeCount free now" else "$queryDay $queryTime · $freeCount free",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
            )
            if (occupiedCount > 0) {
                Text(
                    text = " · $occupiedCount in use",
                    fontSize = 12.sp,
                    color = textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val scopeSummary = selectedBuilding ?: selectedCampus ?: "All University"
        Text(
            text = scopeSummary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            color = textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun VacantRoomsEmptyStateCard(
    surfaceColor: Color,
    borderSubtle: Color,
    textPrimary: Color,
    textMuted: Color,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = surfaceColor,
        border = BorderStroke(1.dp, borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Outlined.MeetingRoom,
                contentDescription = null,
                tint = textMuted,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Rooms Match the Selected Filter",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Try lowering the minimum study gap, clearing search, or switching building.",
                fontSize = 12.sp,
                color = textMuted,
                textAlign = TextAlign.Center
            )
            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.desktopHandCursor()
                ) {
                    Text("Clear Search & Filters", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun VacantRoomsProvenanceNotice(
    surfaceColor: Color,
    borderSubtle: Color,
    accentColor: Color,
    textMuted: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = surfaceColor.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Room catalog and campus assignment sourced from the MUST Room Allocation " +
                    "timetable (2026/2027 Semester I). ${UniversityDirectory.ALL_ROOMS.size} rooms " +
                    "across ${UniversityDirectory.BUILDINGS.size} buildings. Verify venue changes on " +
                    "the official MUST website (timetable.must.ac.ug).",
                fontSize = 12.sp,
                color = textMuted
            )
        }
    }
}
