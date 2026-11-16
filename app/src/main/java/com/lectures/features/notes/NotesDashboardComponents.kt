package com.lectures.features.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.features.timetable.ui.DarkBorderSubtle
import com.lectures.features.timetable.ui.DarkSurfaceCard

@Composable
internal fun NotesSearchBarCard(
    searchQuery: String,
    isDark: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurfaceCard else Color.White),
        border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            if (searchQuery.isNotBlank()) {
                IconButton(onClick = onClearSearch) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun NotesFilterChipsStrip(
    filterChips: List<Pair<String, Int>>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
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
                shape = RoundedCornerShape(12.dp)
            )
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
                    IconButton(onClick = onDismiss) {
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        msg,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D),
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = if (isDark) Color(0xFFA7F3D0) else Color(0xFF15803D),
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Clear Search & Filters", fontSize = 13.sp)
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCreateNote,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
    val courseInfo = availableCourses.find { it.code.equals(courseCode, ignoreCase = true) }
    val courseTitle = courseInfo?.title ?: "Academic Course"
    val dotColor = courseInfo?.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (_: Exception) { Color(0xFF2563EB) }
    } ?: Color(0xFF2563EB)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
                color = if (isDark) Color.White else Color(0xFF0F172A)
            )
        }
        Text(
            text = "$notesCount note${if (notesCount > 1) "s" else ""}",
            fontSize = 12.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}
