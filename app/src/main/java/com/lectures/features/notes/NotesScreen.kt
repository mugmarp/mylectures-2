package com.lectures.features.notes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lectures.TimetableApplication
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.features.timetable.ui.DarkSurfaceBase
import com.lectures.ui.Chrome
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.components.FloatingIslandTopBar
import com.lectures.ui.rememberLecturesWindow
import kotlinx.coroutines.delay

@Composable
fun NotesScreen(
    onBack: (() -> Unit)? = null,
    onSettingsClick: () -> Unit = {},
    onReconfigureAcademicProfile: () -> Unit = {},
    onNavigateToTimetable: () -> Unit = {},
    onPeekProgramme: (String) -> Unit = {},
    onSetOfficialProgramme: (String) -> Unit = {},
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

    if (isEditing) {
        NoteEditorScreen(
            initialNote = noteToEdit,
            initialTab = editingInitialTab,
            availableCourses = uiState.availableCourses,
            timetableClasses = uiState.timetableClasses,
            onSettingsClick = onSettingsClick,
            onReconfigureAcademicProfile = onReconfigureAcademicProfile,
            onNavigateToTimetable = {
                isEditing = false
                editingNoteKey = null
                onNavigateToTimetable()
            },
            onPeekProgramme = { group ->
                isEditing = false
                editingNoteKey = null
                onPeekProgramme(group)
            },
            onSetOfficialProgramme = { group ->
                isEditing = false
                editingNoteKey = null
                onSetOfficialProgramme(group)
            },
            onAttachmentError = { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
            onWikiLinkClick = { linkTitle ->
                val target = viewModel.findNoteByTitle(linkTitle)
                if (target != null) {
                    editingNoteKey = target.naturalKey
                    editingInitialTab = if (target.content.isNotBlank()) NoteEditorTab.PREVIEW else NoteEditorTab.WRITE
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
        return
    }

    val windowInfo = rememberLecturesWindow()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listBottomPadding = Chrome.listBottomPadding(navBarBottom, windowInfo.useNavigationRail)
    val listState = rememberLazyListState()

    val startCreateNote = {
        focusManager.clearFocus()
        keyboardController?.hide()
        editingNoteKey = null
        editingInitialTab = NoteEditorTab.WRITE
        isEditing = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Academic Notes",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            if (uiState.userProgramme.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    uiState.userProgramme,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showProfileSheet = true },
                        modifier = Modifier.testTag("topbar_academic_info_btn")
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
                .background(if (isDark) DarkSurfaceBase else MaterialTheme.colorScheme.background)
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            NotesSearchBarCard(
                searchQuery = uiState.searchQuery,
                isDark = isDark,
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

            if (filterChips.size > 1) {
                NotesFilterChipsStrip(
                    filterChips = filterChips,
                    selectedFilter = uiState.selectedFilter,
                    onFilterSelected = { viewModel.onFilterSelected(it) }
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

            if (uiState.filteredNotes.isEmpty()) {
                NotesEmptyStateView(
                    searchQuery = uiState.searchQuery,
                    selectedFilter = uiState.selectedFilter,
                    onClearFilters = {
                        viewModel.onSearchQueryChanged("")
                        viewModel.onFilterSelected("All Notes")
                    },
                    onCreateNote = startCreateNote
                )
            } else if (windowInfo.isTablet) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 340.dp),
                        modifier = Modifier
                            .widthIn(max = 1040.dp)
                            .fillMaxHeight(),
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

    if (noteToDelete != null) {
        val target = noteToDelete
        AlertDialog(
            onDismissRequest = { deleteNoteKey = null },
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
                        deleteNoteKey = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteNoteKey = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showProfileSheet) {
        val enrolledProg = repository.getInitialProgramme() ?: ""
        AcademicProfileSheet(
            programme = uiState.userProgramme,
            enrolledProgramme = enrolledProg,
            onDismiss = { showProfileSheet = false },
            onPeekProgramme = { peekGroup ->
                showProfileSheet = false
                onPeekProgramme(peekGroup)
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
