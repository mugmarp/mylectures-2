package com.lectures.features.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.components.RoomFloorBadge
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable

@Composable
internal fun MarkdownFormattingToolbar(
    isDark: Boolean,
    applyMarkdown: (prefix: String, suffix: String, placeholder: String) -> Unit,
    insertLinePrefix: (prefix: String) -> Unit,
    insertSnippet: (snippet: String) -> Unit,
    insertDateStamp: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .desktopHoverable()
                    .clickable { applyMarkdown("**", "**", "Bold text") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("B", fontWeight = FontWeight.Black, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .desktopHoverable()
                    .clickable { applyMarkdown("*", "*", "Italic text") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("I", fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF713F12) else Color(0xFFFEF08A))
                    .desktopHoverable()
                    .clickable { applyMarkdown("==", "==", "highlighted term") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "==",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFFEF08A) else Color(0xFF713F12)
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .desktopHoverable()
                    .clickable { insertLinePrefix("# ") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("H1", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .desktopHoverable()
                    .clickable { insertLinePrefix("## ") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("H2", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }

            IconButton(
                onClick = { insertLinePrefix("- [ ] ") },
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.Default.Checklist,
                    contentDescription = "Checklist item",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp)
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                    .desktopHoverable()
                    .clickable {
                        insertSnippet("> [!NOTE] Key Takeaway\n> Core concept explained here\n")
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "[!Note]",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                    .desktopHoverable()
                    .clickable {
                        insertSnippet("\n| Topic | Key Concept | Status |\n| :--- | :--- | :---: |\n| Point 1 | Definition | Ready |\n| Point 2 | Example | Review |\n")
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Table",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                    .desktopHoverable()
                    .clickable { applyMarkdown("$$", "$$", "E = mc^2") }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$$ Math",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .desktopHoverable()
                    .clickable { applyMarkdown("[[", "]]", "Course Note") }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("[[Link]]", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }

            IconButton(
                onClick = { insertLinePrefix("- ") },
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.FormatListBulleted,
                    contentDescription = "Bullet List",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = { insertLinePrefix("1. ") },
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.Default.FormatListNumbered,
                    contentDescription = "Numbered List",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = { insertLinePrefix("> ") },
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.Default.FormatQuote,
                    contentDescription = "Quote",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(19.dp)
                )
            }

            IconButton(
                onClick = { applyMarkdown("`", "`", "formula") },
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.Default.Code,
                    contentDescription = "Code or Formula",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = { insertSnippet("\n---\n") },
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.Default.HorizontalRule,
                    contentDescription = "Divider",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            IconButton(
                onClick = insertDateStamp,
                modifier = Modifier
                    .size(48.dp)
                    .desktopHandCursor()
            ) {
                Icon(
                    Icons.Outlined.Today,
                    contentDescription = "Insert Date Stamp",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun AttachedTimetableClassCard(
    attachedClassText: String,
    attachedLocation: String,
    lecturer: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .desktopHandCursor()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .desktopHoverable()
        ) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp, top = 14.dp, end = 16.dp, bottom = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "ATTACHED TIMETABLE CLASS",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (attachedLocation.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                attachedLocation,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Medium
                            )
                            val roomClean = attachedLocation.removePrefix("📍").trim()
                            if (roomClean.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                RoomFloorBadge(roomName = roomClean, compact = true)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = attachedClassText.substringBefore(" · "),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val detailLine = buildString {
                    val schedulePart = attachedClassText.substringAfter(" · ", "")
                    append(schedulePart)
                    if (lecturer.isNotBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append(lecturer)
                    }
                }
                if (detailLine.isNotBlank()) {
                    Text(
                        text = detailLine,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun PreClassAlertReminderCard(
    canScheduleClassAlert: Boolean,
    selectedAlarmMinutes: Int?,
    onOpenClassPicker: () -> Unit,
    onSelectAlarmMinutes: (Int?) -> Unit,
    onOpenCustomDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "PRE-CLASS ALERT REMINDER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        when {
                            !canScheduleClassAlert -> "Needs Class"
                            selectedAlarmMinutes != null -> "${selectedAlarmMinutes}m Alert"
                            else -> "Disabled"
                        },
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!canScheduleClassAlert) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .desktopHoverable()
                        .clickable(onClick = onOpenClassPicker)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Attach a timetable class to arm pre-class alerts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Attach",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val reminderOptions = listOf(
                        "None" to null,
                        "15m" to 15,
                        "30m" to 30,
                        "1h" to 60,
                        (if (selectedAlarmMinutes != null && selectedAlarmMinutes !in listOf(15, 30, 60)) "${selectedAlarmMinutes}m" else "Custom") to selectedAlarmMinutes
                    )

                    reminderOptions.forEachIndexed { idx, (label, minutes) ->
                        val isSelected = (idx == 4 && selectedAlarmMinutes != null && selectedAlarmMinutes !in listOf(15, 30, 60)) ||
                            (idx < 4 && selectedAlarmMinutes == minutes)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                                .desktopHoverable(enabled = !isSelected)
                                .desktopHandCursor()
                                .clickable {
                                    if (idx == 4) {
                                        onOpenCustomDialog()
                                    } else {
                                        onSelectAlarmMinutes(minutes)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun NoteAttachmentsSection(
    isCopyingAttachment: Boolean,
    attachmentName: String,
    attachmentPath: String?,
    onPickDocument: () -> Unit,
    onPickPhoto: () -> Unit,
    onOpenAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "ATTACHMENTS & SLIDES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.5.sp
        )
        Text(
            when {
                isCopyingAttachment -> "Saving file..."
                attachmentName.isNotBlank() -> "1 file attached"
                else -> "None"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }

    if (isCopyingAttachment) {
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .desktopHandCursor(!isCopyingAttachment)
                .clickable(enabled = !isCopyingAttachment, onClick = onPickDocument),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .desktopHoverable(enabled = !isCopyingAttachment)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Slide / Doc", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .desktopHandCursor(!isCopyingAttachment)
                .clickable(enabled = !isCopyingAttachment, onClick = onPickPhoto),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .desktopHoverable(enabled = !isCopyingAttachment)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Whiteboard", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

    if (attachmentName.isNotBlank()) {
        Spacer(modifier = Modifier.height(10.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .desktopHoverable(enabled = attachmentPath != null)
                        .clickable(enabled = attachmentPath != null, onClick = onOpenAttachment)
                        .padding(horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = attachmentName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (attachmentPath != null) "Tap to open" else "Name only — file not stored",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onRemoveAttachment,
                    modifier = Modifier
                        .size(48.dp)
                        .desktopHandCursor()
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove attachment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

