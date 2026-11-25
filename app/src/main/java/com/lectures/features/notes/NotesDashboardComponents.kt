package com.lectures.features.notes

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.storage.AttachmentStore
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberPrefersReducedMotion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun NotesSearchBarCard(
    searchQuery: String,
    isDark: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    focusRequester: FocusRequester? = null,
    showDesktopShortcutHint: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = {
                    Text(
                        "Search notes, tags, or courses...",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && searchQuery.isNotEmpty()) {
                            onClearSearch()
                            true
                        } else false
                    }
                    .testTag("notes_search_input"),
                singleLine = true
            )
            if (searchQuery.isNotBlank()) {
                IconButton(
                    onClick = onClearSearch,
                    modifier = Modifier
                        .size(48.dp)
                        .desktopHandCursor()
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (showDesktopShortcutHint) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ext.surfaceElevated,
                    border = BorderStroke(1.dp, ext.borderSubtle)
                ) {
                    Text(
                        text = "^F",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = ext.textMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotesFilterChipsStrip(
    filterChips: List<Pair<String, Int>>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    isTabletOrDesktop: Boolean = false,
    sortOrder: NoteSortOrder = NoteSortOrder.PINNED_FIRST,
    onSortOrderSelected: (NoteSortOrder) -> Unit = {}
) {
    val ext = LocalExtendedColors.current
    var sortMenuExpanded by remember { mutableStateOf(false) }

    if (isTabletOrDesktop) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filterChips.forEach { (label, count) ->
                    val selected = selectedFilter.equals(label, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterSelected(label) },
                        label = {
                            Text(
                                text = "$label ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.desktopHandCursor()
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box {
                AssistChip(
                    onClick = { sortMenuExpanded = true },
                    leadingIcon = {
                        Icon(
                            Icons.AutoMirrored.Outlined.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ext.textSecondary
                        )
                    },
                    label = {
                        Text(
                            text = sortOrder.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .desktopHandCursor()
                        .testTag("notes_sort_button")
                )

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    NoteSortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == sortOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                sortMenuExpanded = false
                                onSortOrderSelected(option)
                            },
                            modifier = Modifier.desktopHandCursor()
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterChips.size) { index ->
                    val (label, count) = filterChips[index]
                    val selected = selectedFilter.equals(label, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onFilterSelected(label) },
                        label = {
                            Text(
                                text = "$label ($count)",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.desktopHandCursor()
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { sortMenuExpanded = true },
                    modifier = Modifier
                        .size(48.dp)
                        .desktopHandCursor()
                        .testTag("notes_sort_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = "Sort notes (${sortOrder.label})",
                        tint = if (sortOrder != NoteSortOrder.PINNED_FIRST) MaterialTheme.colorScheme.primary else ext.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    NoteSortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = option.label,
                                    fontWeight = if (option == sortOrder) FontWeight.Bold else FontWeight.Normal,
                                    color = if (option == sortOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                sortMenuExpanded = false
                                onSortOrderSelected(option)
                            },
                            modifier = Modifier.desktopHandCursor()
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotesFeedbackBanners(
    errorMessage: String?,
    userMessage: String?,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(visible = errorMessage != null) {
        errorMessage?.let { err ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        err,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }

    AnimatedVisibility(visible = userMessage != null) {
        userMessage?.let { msg ->
            val ext = com.lectures.ui.LocalExtendedColors.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ext.successBg)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        msg,
                        color = ext.successText,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = ext.successText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun NotesEmptyStateView(
    searchQuery: String,
    selectedFilter: String,
    onClearFilters: () -> Unit,
    onCreateNote: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (searchQuery.isNotBlank() || selectedFilter != "All Notes") {
                        Icons.Outlined.SearchOff
                    } else {
                        Icons.AutoMirrored.Outlined.Notes
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = when {
                    searchQuery.isNotBlank() -> "No notes matching \"$searchQuery\""
                    selectedFilter != "All Notes" -> "No notes for $selectedFilter"
                    else -> "No lecture notes yet"
                },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    searchQuery.isNotBlank() || selectedFilter != "All Notes" ->
                        "Try adjusting your query or resetting your course filter to find what you're looking for."
                    else ->
                        "Write rich lecture summaries, link classes from your timetable, and set reminder alarms."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            if (searchQuery.isNotBlank() || selectedFilter != "All Notes") {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onClearFilters,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.desktopHandCursor()
                ) {
                    Text("Clear Search & Filters", fontSize = 13.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateNote,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.desktopHandCursor()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Note", fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                }
            }
        }
    }
}

@Composable
internal fun NotesCourseGroupHeader(
    courseCode: String,
    notesCount: Int,
    availableCourses: List<CourseOption>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val courseInfo = availableCourses.find { it.code.equals(courseCode, ignoreCase = true) }
    val courseTitle = courseInfo?.title ?: "Academic Course"
    val fallbackPrimary = MaterialTheme.colorScheme.primary
    val dotColor = remember(courseInfo?.color, fallbackPrimary) {
        courseInfo?.color?.let {
            try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { fallbackPrimary }
        } ?: fallbackPrimary
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$courseCode · $courseTitle",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = ext.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$notesCount note${if (notesCount > 1) "s" else ""}",
            fontSize = 12.sp,
            color = ext.textMuted
        )
    }
}

@Composable
internal fun NotesDesktopPreviewPane(
    note: LectureNote?,
    onEdit: (LectureNote) -> Unit,
    onTogglePin: (LectureNote) -> Unit,
    onDelete: (LectureNote) -> Unit,
    onToggleChecklist: ((LectureNote, Int) -> Unit)? = null,
    onClearSelection: (() -> Unit)? = null,
    onRestoreSelection: (() -> Unit)? = null,
    onWikiLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ext.surfaceCard,
        border = BorderStroke(1.dp, ext.borderSubtle),
        shadowElevation = 2.dp,
        modifier = modifier.testTag("notes_quick_preview_pane")
    ) {
        AnimatedContent(
            targetState = note,
            transitionSpec = { MotionSpec.fadeTransform(reducedMotion = reducedMotion) },
            contentKey = { it?.naturalKey ?: "empty_note_preview" },
            modifier = Modifier.fillMaxSize(),
            label = "notes_preview_pane_switch"
        ) { activeNote ->
        if (activeNote == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Notes,
                        contentDescription = null,
                        tint = accent.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "Select any note card to read it here, or double-click a card to jump into the Split Markdown Editor.",
                        fontSize = 13.sp,
                        color = ext.textMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                    if (onRestoreSelection != null) {
                        OutlinedButton(
                            onClick = onRestoreSelection,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.desktopHandCursor()
                        ) {
                            Text("Show Top Note", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            val courseCode = activeNote.naturalKey.split("|").firstOrNull()?.ifBlank { "General" } ?: "General"
            val formattedDate = remember(activeNote.updatedAt) {
                SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(activeNote.updatedAt))
            }
            val checklistProgress = remember(activeNote.content) {
                MarkdownUtils.countChecklistProgress(activeNote.content)
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = accent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = courseCode,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        if (!activeNote.tag.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = ext.surfaceElevated
                            ) {
                                Text(
                                    text = activeNote.tag.orEmpty(),
                                    fontSize = 11.sp,
                                    color = ext.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText(activeNote.title ?: courseCode, activeNote.content))
                                Toast.makeText(context, "Markdown copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .desktopHandCursor()
                                .testTag("notes_preview_copy_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Copy Markdown",
                                tint = ext.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onTogglePin(activeNote) },
                            modifier = Modifier
                                .size(48.dp)
                                .desktopHandCursor()
                        ) {
                            Icon(
                                imageVector = if (activeNote.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (activeNote.isPinned) "Unpin" else "Pin",
                                tint = if (activeNote.isPinned) accent else ext.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDelete(activeNote) },
                            modifier = Modifier
                                .size(48.dp)
                                .desktopHandCursor()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete note",
                                tint = ext.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (onClearSelection != null) {
                            IconButton(
                                onClick = onClearSelection,
                                modifier = Modifier
                                    .size(48.dp)
                                    .desktopHandCursor()
                                    .testTag("notes_preview_close_btn")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close preview", tint = ext.textMuted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(color = ext.borderSubtle)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = activeNote.title?.ifBlank { "Untitled Note" } ?: "Academic Note: $courseCode",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ext.textPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Updated $formattedDate",
                            fontSize = 11.5.sp,
                            color = ext.textMuted
                        )
                        if (activeNote.alarmMinutes != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = accent.copy(alpha = 0.12f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = accent, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("${activeNote.alarmMinutes}m alert", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, color = accent)
                                }
                            }
                        }
                        if (checklistProgress != null) {
                            val (done, total) = checklistProgress
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (done == total) ext.successBg else ext.surfaceElevated
                            ) {
                                Text(
                                    text = "$done/$total tasks",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (done == total) ext.successText else ext.textSecondary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (!activeNote.attachedClass.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ext.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Linked Class: ${activeNote.attachedClass}",
                                fontSize = 12.sp,
                                color = ext.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }

                    if (!activeNote.attachmentName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = ext.surfaceElevated,
                            border = BorderStroke(1.dp, ext.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .let { mod ->
                                    if (!activeNote.attachmentPath.isNullOrBlank()) {
                                        mod.desktopHoverable().clickable {
                                            val uri = AttachmentStore.contentUriFor(context, activeNote.attachmentPath)
                                            if (uri != null) {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    Toast.makeText(context, "No app can open this attachment", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    } else mod
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeNote.attachmentName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ext.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (activeNote.attachmentPath != null) "Click to open attached file" else "Attachment name saved",
                                        fontSize = 10.5.sp,
                                        color = ext.textMuted
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = ext.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = ext.borderSubtle.copy(alpha = 0.6f))

                    if (activeNote.content.isBlank()) {
                        Text(
                            text = "This note is empty yet. Click \"Open Split Editor\" below to start writing or apply a Cornell/Exam template.",
                            fontSize = 13.sp,
                            color = ext.textMuted
                        )
                    } else {
                        MarkdownViewer(
                            markdown = activeNote.content,
                            onToggleChecklist = onToggleChecklist?.let { cb -> { lineIdx -> cb(activeNote, lineIdx) } },
                            onWikiLinkClick = onWikiLinkClick,
                            onUrlClick = { rawUrl ->
                                val safe = if (rawUrl.startsWith("http://", true) || rawUrl.startsWith("https://", true)) rawUrl else "https://$rawUrl"
                                try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safe))) } catch (_: Exception) {}
                            }
                        )
                    }
                }

                HorizontalDivider(color = ext.borderSubtle)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = { onEdit(activeNote) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .desktopHandCursor()
                            .testTag("notes_preview_edit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = contrastOnColor(accent)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Split Editor", fontSize = 13.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        }
    }
}
