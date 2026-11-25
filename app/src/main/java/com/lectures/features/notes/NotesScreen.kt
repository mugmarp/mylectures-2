package com.lectures.features.notes

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.ui.DarkSurfaceBase
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

@Composable
fun NotesScreen(
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
    onEditingChanged: (Boolean) -> Unit = {}
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
    val windowInfo = rememberLecturesWindow()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val classAlarmScheduler = remember(context) { AlarmScheduler(context) }
    val viewModel: NotesViewModel = viewModel(
        factory = NotesViewModel.provideFactory(repository, classAlarmScheduler)
    )

    val uiState by viewModel.uiState.collectAsState()

    var isEditing by rememberSaveable { mutableStateOf(false) }
    var editingNoteKey by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteNoteKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }

    val noteToEdit = remember(editingNoteKey, uiState.filteredNotes, uiState.notes) {
        val targetKey = editingNoteKey ?: return@remember null
        uiState.notes.firstOrNull { it.naturalKey == targetKey }
            ?: uiState.filteredNotes.firstOrNull { it.naturalKey == targetKey }
    }
    val noteToDelete = remember(deleteNoteKey, uiState.filteredNotes, uiState.notes) {
        val targetKey = deleteNoteKey ?: return@remember null
        uiState.notes.firstOrNull { it.naturalKey == targetKey }
            ?: uiState.filteredNotes.firstOrNull { it.naturalKey == targetKey }
    }

    var editingInitialTab by rememberSaveable { mutableStateOf(NoteEditorTab.WRITE) }

    LaunchedEffect(uiState.userMessage) {
        if (uiState.userMessage != null) {
            delay(4000L)
            viewModel.clearFeedback()
        }
    }

    LaunchedEffect(isEditing) {
        onEditingChanged(isEditing)
    }
    DisposableEffect(Unit) {
        onDispose {
            onEditingChanged(false)
        }
    }

    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = Chrome.listBottomPadding(navBarBottom, windowInfo.useNavigationRail)
    val listState = rememberLazyListState()
    val searchFocusRequester = remember { FocusRequester() }
    var desktopPreviewKey by rememberSaveable { mutableStateOf<String?>(null) }
    var desktopPreviewDismissed by rememberSaveable { mutableStateOf(false) }

    val startCreateNote = {
        focusManager.clearFocus()
        keyboardController?.hide()
        editingNoteKey = null
        editingInitialTab = if (windowInfo.isDualPane) NoteEditorTab.SPLIT else NoteEditorTab.WRITE
        isEditing = true
    }

    val openNoteInEditor: (LectureNote, NoteEditorTab?) -> Unit = { target, tabOverride ->
        focusManager.clearFocus()
        keyboardController?.hide()
        editingNoteKey = target.naturalKey
        editingInitialTab = tabOverride ?: if (windowInfo.isDualPane) {
            NoteEditorTab.SPLIT
        } else if (target.content.isNotBlank()) {
            NoteEditorTab.PREVIEW
        } else {
            NoteEditorTab.WRITE
        }
        isEditing = true
    }

    val activePreviewNote = remember(desktopPreviewKey, desktopPreviewDismissed, uiState.filteredNotes, uiState.notes) {
        if (desktopPreviewDismissed) {
            null
        } else {
            val target = desktopPreviewKey
            if (target != null) {
                uiState.filteredNotes.firstOrNull { it.naturalKey == target }
                    ?: uiState.notes.firstOrNull { it.naturalKey == target }
                    ?: uiState.filteredNotes.firstOrNull()
            } else {
                uiState.filteredNotes.firstOrNull()
            }
        }
    }

    AnimatedContent(
        targetState = isEditing,
        transitionSpec = {
            MotionSpec.directionalSlideTransform(
                forward = targetState,
                reducedMotion = reducedMotion,
                useFadeOnly = windowInfo.useNavigationRail
            )
        },
        contentKey = { it },
        label = "notes_editor_transition"
    ) { editing ->
        if (editing) {
            NoteEditorScreen(
                initialNote = noteToEdit,
                initialTab = editingInitialTab,
                availableCourses = uiState.availableCourses,
                timetableClasses = uiState.timetableClasses,
                onAttachmentError = { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                },
                onWikiLinkClick = { linkTitle ->
                    val target = viewModel.findNoteByTitle(linkTitle)
                    if (target != null) {
                        editingNoteKey = target.naturalKey
                        editingInitialTab = if (windowInfo.isDualPane) {
                            NoteEditorTab.SPLIT
                        } else if (target.content.isNotBlank()) {
                            NoteEditorTab.PREVIEW
                        } else {
                            NoteEditorTab.WRITE
                        }
                        isEditing = true
                    } else {
                        Toast.makeText(context, "Linked note \"$linkTitle\" not found", Toast.LENGTH_SHORT).show()
                    }
                },
                onSave = { key, course, title, content, alarm, tag, attachedClass, attachment, attachmentPath ->
                    viewModel.saveNote(
                        naturalKey = key,
                        courseCode = course,
                        title = title,
                        content = content,
                        alarmMinutes = alarm,
                        tag = tag,
                        attachedClass = attachedClass,
                        attachmentName = attachment,
                        attachmentPath = attachmentPath
                    )
                    isEditing = false
                    editingNoteKey = null
                },
                onCancel = {
                    isEditing = false
                    editingNoteKey = null
                }
            )
        } else {
            Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                val cmd = event.isCtrlPressed || event.isMetaPressed
                when {
                    cmd && event.key == Key.N -> {
                        startCreateNote()
                        true
                    }
                    cmd && event.key == Key.F -> {
                        try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
                        true
                    }
                    cmd && event.key == Key.E && activePreviewNote != null -> {
                        openNoteInEditor(activePreviewNote, NoteEditorTab.SPLIT)
                        true
                    }
                    event.key == Key.Escape && uiState.searchQuery.isNotEmpty() -> {
                        viewModel.onSearchQueryChanged("")
                        focusManager.clearFocus()
                        true
                    }
                    event.key == Key.Escape && activePreviewNote != null -> {
                        desktopPreviewKey = null
                        desktopPreviewDismissed = true
                        true
                    }
                    else -> false
                }
            } else false
        },
        floatingActionButton = {
            if (windowInfo.collapseFab) {
                FloatingActionButton(
                    onClick = startCreateNote,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                        .desktopHandCursor()
                        .testTag("add_note_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(24.dp))
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = startCreateNote,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = Chrome.fabBottomPadding(windowInfo.useNavigationRail))
                        .desktopHandCursor()
                        .testTag("add_note_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Note", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Note", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null,
                                tint = contrastOnColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Academic Notes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "${uiState.filteredNotes.size} active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                            if (uiState.userProgramme.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    uiState.userProgramme,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ext.surfaceBase)
                .padding(top = innerPadding.calculateTopPadding()),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .let {
                        if (windowInfo.isTablet) it.widthIn(max = windowInfo.maxReadingWidth) else it
                    }
                    .fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                NotesSearchBarCard(
                    searchQuery = uiState.searchQuery,
                    isDark = isDark,
                    focusRequester = searchFocusRequester,
                    showDesktopShortcutHint = windowInfo.isWideWorkspace,
                    onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onClearSearch = {
                        viewModel.onSearchQueryChanged("")
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                )

                Spacer(modifier = Modifier.height(10.dp))

                val filterChips = remember(uiState.notes, uiState.availableCourses) {
                    val base = mutableListOf("All Notes" to uiState.notes.size)
                    val pinnedCount = uiState.notes.count { it.isPinned }
                    if (pinnedCount > 0) base.add("Pinned" to pinnedCount)
                    val checklistCount = uiState.notes.count { MarkdownUtils.countChecklistProgress(it.content) != null }
                    if (checklistCount > 0) base.add("Checklists" to checklistCount)
                    val alertCount = uiState.notes.count { it.alarmMinutes != null }
                    if (alertCount > 0) base.add("Alerts" to alertCount)

                    val notesByCourse = uiState.notes.groupingBy {
                        it.naturalKey.split("|").firstOrNull()?.trim().orEmpty()
                    }.eachCount()
                    uiState.availableCourses.forEach { course ->
                        val count = notesByCourse[course.code] ?: 0
                        if (count > 0) {
                            base.add(course.code to count)
                        }
                    }
                    base
                }

                if (filterChips.size > 1 || windowInfo.isTablet) {
                    NotesFilterChipsStrip(
                        filterChips = filterChips,
                        selectedFilter = uiState.selectedFilter,
                        onFilterSelected = { viewModel.onFilterSelected(it) },
                        isTabletOrDesktop = windowInfo.isTablet,
                        sortOrder = uiState.sortOrder,
                        onSortOrderSelected = { viewModel.onSortOrderSelected(it) }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                NotesFeedbackBanners(
                    errorMessage = uiState.errorMessage,
                    userMessage = uiState.userMessage,
                    isDark = isDark,
                    onDismiss = { viewModel.clearFeedback() }
                )

                val grouped = remember(uiState.filteredNotes) {
                    uiState.filteredNotes.groupBy { note ->
                        note.naturalKey.split("|").firstOrNull()?.ifBlank { "General" } ?: "General"
                    }
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (uiState.filteredNotes.isEmpty()) {
                    NotesEmptyStateView(
                        searchQuery = uiState.searchQuery,
                        selectedFilter = uiState.selectedFilter,
                        onClearFilters = {
                            viewModel.onSearchQueryChanged("")
                            viewModel.onFilterSelected("All Notes")
                        },
                        onCreateNote = startCreateNote
                    )
                } else if (windowInfo.isWideWorkspace) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 290.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .testTag("notes_desktop_grid"),
                            contentPadding = PaddingValues(bottom = listBottomPadding),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            grouped.forEach { (courseCode, notes) ->
                                item(key = "grid_header_$courseCode", span = { GridItemSpan(maxLineSpan) }) {
                                    NotesCourseGroupHeader(
                                        courseCode = courseCode,
                                        notesCount = notes.size,
                                        availableCourses = uiState.availableCourses,
                                        isDark = isDark,
                                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                    )
                                }

                                items(notes, key = { it.naturalKey }) { note ->
                                    val isSelected = activePreviewNote?.naturalKey == note.naturalKey
                                    NoteCardItem(
                                        note = note,
                                        courseCode = courseCode,
                                        isDark = isDark,
                                        isSelected = isSelected,
                                        enableDoubleClickEdit = true,
                                        modifier = Modifier.animateItem(),
                                        onOpen = {
                                            desktopPreviewKey = note.naturalKey
                                            desktopPreviewDismissed = false
                                        },
                                        onEdit = { openNoteInEditor(note, NoteEditorTab.SPLIT) },
                                        onTogglePin = { viewModel.togglePin(note.naturalKey) },
                                        onDelete = { deleteNoteKey = note.naturalKey }
                                    )
                                }
                            }
                        }

                        val readerWidth = if (windowInfo.isExtraLargeWidth) 460.dp else 390.dp
                        NotesDesktopPreviewPane(
                            note = activePreviewNote,
                            onEdit = { selected -> openNoteInEditor(selected, NoteEditorTab.SPLIT) },
                            onTogglePin = { selected -> viewModel.togglePin(selected.naturalKey) },
                            onDelete = { selected -> deleteNoteKey = selected.naturalKey },
                            onToggleChecklist = { selected, lineIndex ->
                                viewModel.toggleChecklist(selected.naturalKey, lineIndex)
                            },
                            onClearSelection = if (activePreviewNote != null) {
                                {
                                    desktopPreviewKey = null
                                    desktopPreviewDismissed = true
                                }
                            } else null,
                            onRestoreSelection = if (desktopPreviewDismissed && uiState.filteredNotes.isNotEmpty()) {
                                {
                                    desktopPreviewDismissed = false
                                    desktopPreviewKey = uiState.filteredNotes.first().naturalKey
                                }
                            } else null,
                            onWikiLinkClick = { linkTitle ->
                                val target = viewModel.findNoteByTitle(linkTitle)
                                if (target != null) {
                                    desktopPreviewKey = target.naturalKey
                                    desktopPreviewDismissed = false
                                } else {
                                    Toast.makeText(context, "Linked note \"$linkTitle\" not found", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .width(readerWidth)
                                .fillMaxHeight()
                                .padding(bottom = Chrome.listBottomPadding(0.dp, useNavigationRail = true))
                        )
                    }
                } else if (windowInfo.isTablet) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 320.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = listBottomPadding),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        grouped.forEach { (courseCode, notes) ->
                            item(key = "grid_header_$courseCode", span = { GridItemSpan(maxLineSpan) }) {
                                NotesCourseGroupHeader(
                                    courseCode = courseCode,
                                    notesCount = notes.size,
                                    availableCourses = uiState.availableCourses,
                                    isDark = isDark,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                                )
                            }

                            items(notes, key = { it.naturalKey }) { note ->
                                NoteCardItem(
                                    note = note,
                                    courseCode = courseCode,
                                    isDark = isDark,
                                    modifier = Modifier.animateItem(),
                                    onOpen = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        editingNoteKey = note.naturalKey
                                        editingInitialTab = if (note.content.isNotBlank()) NoteEditorTab.PREVIEW else NoteEditorTab.WRITE
                                        isEditing = true
                                    },
                                    onEdit = {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        editingNoteKey = note.naturalKey
                                        editingInitialTab = NoteEditorTab.WRITE
                                        isEditing = true
                                    },
                                    onTogglePin = { viewModel.togglePin(note.naturalKey) },
                                    onDelete = { deleteNoteKey = note.naturalKey }
                                )
                            }
                        }
                    }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = listBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    grouped.forEach { (courseCode, notes) ->
                        item(key = "header_$courseCode") {
                            NotesCourseGroupHeader(
                                courseCode = courseCode,
                                notesCount = notes.size,
                                availableCourses = uiState.availableCourses,
                                isDark = isDark,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        }

                        items(notes, key = { it.naturalKey }) { note ->
                            NoteCardItem(
                                note = note,
                                courseCode = courseCode,
                                isDark = isDark,
                                modifier = Modifier.animateItem(),
                                onOpen = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    editingNoteKey = note.naturalKey
                                    editingInitialTab = if (note.content.isNotBlank()) NoteEditorTab.PREVIEW else NoteEditorTab.WRITE
                                    isEditing = true
                                },
                                onEdit = {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    editingNoteKey = note.naturalKey
                                    editingInitialTab = NoteEditorTab.WRITE
                                    isEditing = true
                                },
                                onTogglePin = { viewModel.togglePin(note.naturalKey) },
                                onDelete = { deleteNoteKey = note.naturalKey }
                            )
                        }
                    }
                }
            }
        }
        }
    }

    if (noteToDelete != null) {
        val target = noteToDelete
        AlertDialog(
            onDismissRequest = { deleteNoteKey = null },
            properties = DialogProperties(usePlatformDefaultWidth = !windowInfo.preferDesktopDialogs),
            modifier = Modifier
                .widthIn(min = 340.dp, max = 460.dp)
                .onEscapeKey { deleteNoteKey = null },
            icon = {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Delete this note?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete \"${target.title ?: "Untitled Note"}\"? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(
                            naturalKey = target.naturalKey,
                            attachmentPath = target.attachmentPath,
                            context = context
                        )
                        if (desktopPreviewKey == target.naturalKey) {
                            desktopPreviewKey = null
                        }
                        deleteNoteKey = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { deleteNoteKey = null },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp).desktopHandCursor()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showProfileSheet) {
        val enrolledProg = enrolledProgrammeOverride?.takeIf { it.isNotBlank() }
            ?: (repository.getInitialProgramme() ?: "")
        val activeProg = activeProgrammeOverride?.takeIf { it.isNotBlank() }
            ?: uiState.userProgramme
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
    }
}
