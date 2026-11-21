package com.lectures.features.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.rememberLifecycleGatedClockPulse
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.ui.DarkBorderSubtle
import com.lectures.features.timetable.ui.DarkSurfaceCard
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCardItem(
    note: LectureNote,
    courseCode: String,
    isDark: Boolean = false,
    isSelected: Boolean = false,
    enableDoubleClickEdit: Boolean = false,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onEdit: () -> Unit = onOpen,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val ext = LocalExtendedColors.current
    val accent = MaterialTheme.colorScheme.primary

    val categoryTag = note.tag ?: "Lecture"

    val scheduleTag = remember(note.naturalKey, note.attachedClass) {
        val classText = note.attachedClass.orEmpty()
        val combined = "${note.naturalKey}|$classText"
        val dayMatch = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            .firstOrNull { combined.contains(it, ignoreCase = true) }
        when {
            dayMatch != null -> "📅 $dayMatch"
            classText.isNotBlank() -> "📅 Attached Class"
            else -> "📝 Course Note"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .desktopHandCursor()
            .combinedClickable(
                onClick = onOpen,
                onDoubleClick = if (enableDoubleClickEdit) onEdit else null
            )
            .testTag("note_card_${note.naturalKey}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> if (isDark) accent.copy(alpha = 0.14f) else accent.copy(alpha = 0.06f)
                isDark -> DarkSurfaceCard
                else -> Color.White
            }
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) accent else if (isDark) DarkBorderSubtle else ext.borderSubtle
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 3.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .desktopHoverable()
                .padding(16.dp)
        ) {
            // Tag Row with Pins and Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val tagBg = when (categoryTag) {
                        "Lab Prep" -> if (isDark) Color(0xFF312E81) else Color(0xFFEEF2FF)
                        "Study Guide" -> if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)
                        "Revision", "Exam Prep" -> if (isDark) Color(0xFF064E3B) else Color(0xFFECFDF5)
                        "Assignment" -> if (isDark) Color(0xFF581C87) else Color(0xFFF3E8FF)
                        else -> if (isDark) com.lectures.features.timetable.ui.DarkSurfaceContainer else Color(0xFFF1F5F9)
                    }
                    val tagText = when (categoryTag) {
                        "Lab Prep" -> if (isDark) Color(0xFFA5B4FC) else Color(0xFF4338CA)
                        "Study Guide" -> if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                        "Revision", "Exam Prep" -> if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857)
                        "Assignment" -> if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
                        else -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagBg)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            categoryTag,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = tagText,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) com.lectures.features.timetable.ui.DarkSurfaceContainer else Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            scheduleTag,
                            fontSize = 11.sp,
                            color = ext.textSecondary,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(
                            if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (note.isPinned) accent else ext.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .desktopHandCursor()
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options",
                                tint = ext.textMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Note") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                modifier = Modifier.desktopHandCursor()
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                                onClick = {
                                    showMenu = false
                                    onTogglePin()
                                },
                                leadingIcon = {
                                    Icon(
                                        if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                        contentDescription = null
                                    )
                                },
                                modifier = Modifier.desktopHandCursor()
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                modifier = Modifier.desktopHandCursor()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Note Title
            Text(
                text = note.title ?: "Academic Note: $courseCode",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color(0xFF0F172A),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Note Content Preview (2 lines, stripped of raw markdown symbols)
            val cleanPreview = remember(note.content) {
                MarkdownUtils.stripMarkdown(note.content).ifBlank { "Tap to add lecture notes, checklist items, or study formulas..." }
            }
            Text(
                text = cleanPreview,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            val checklistProgress = remember(note.content) {
                MarkdownUtils.countChecklistProgress(note.content)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom Bar: Reminder Pill / Checklist Progress & Timestamp/Attachment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    val accentColor = MaterialTheme.colorScheme.primary
                    if (note.alarmMinutes != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = if (isDark) 0.22f else 0.1f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${note.alarmMinutes}m alert",
                                    fontSize = 11.sp,
                                    color = accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    if (checklistProgress != null) {
                        val (done, total) = checklistProgress
                        val isComplete = done == total
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isComplete) (if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7))
                                    else accentColor.copy(alpha = if (isDark) 0.22f else 0.1f)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isComplete) Icons.Default.CheckCircle else Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = if (isComplete) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)) else accentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "$done/$total tasks",
                                    fontSize = 11.sp,
                                    color = if (isComplete) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D)) else accentColor,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!note.attachmentName.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (note.attachmentPath != null) "1 File" else "Name only",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    val clockPulse = rememberLifecycleGatedClockPulse()
                    val relativeTime = remember(note.updatedAt, clockPulse) {
                        val diff = System.currentTimeMillis() - note.updatedAt
                        when {
                            diff < 0 -> "Just now"
                            diff < 60_000L -> "Just now"
                            diff < 3600_000L -> "${(diff / 60_000L).coerceAtLeast(1)}m ago"
                            diff < 86400_000L -> "${diff / 3600_000L}h ago"
                            diff < 172800_000L -> "Yesterday"
                            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(note.updatedAt))
                        }
                    }
                    Text(
                        text = relativeTime,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
