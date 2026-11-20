package com.lectures.features.tasks

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.features.timetable.ui.DarkBorderSubtle
import com.lectures.features.timetable.ui.DarkSurfaceBase
import com.lectures.features.timetable.ui.DarkSurfaceCard
import com.lectures.features.timetable.ui.TextMutedLight
import com.lectures.features.timetable.ui.TextPrimaryLight
import java.util.Calendar
import java.util.Locale

enum class TaskDueStatus {
    OVERDUE,
    DUE_TODAY,
    DUE_TOMORROW,
    UPCOMING,
    NO_DATE
}

fun getTaskDueStatus(dueDate: String, isCompleted: Boolean): TaskDueStatus {
    if (isCompleted) return TaskDueStatus.UPCOMING
    val millis = TaskDateTimeParser.calculateDueMillis(dueDate) ?: return TaskDueStatus.NO_DATE
    val now = Calendar.getInstance()
    val dueCal = Calendar.getInstance().apply { timeInMillis = millis }

    if (millis < now.timeInMillis) {
        return TaskDueStatus.OVERDUE
    }

    val isSameDay = now.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)
    if (isSameDay) return TaskDueStatus.DUE_TODAY

    val tomorrowCal = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrowCal.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
        tomorrowCal.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)
    if (isTomorrow) return TaskDueStatus.DUE_TOMORROW

    return TaskDueStatus.UPCOMING
}

fun resolveTaskCategoryAndTitle(title: String): Pair<TaskCategory, String> {
    val trimmed = title.trim()
    if (trimmed.startsWith("[") && trimmed.contains("]")) {
        val closeBracket = trimmed.indexOf("]")
        val categoryStr = trimmed.substring(1, closeBracket).trim()
        val cleanTitle = trimmed.substring(closeBracket + 1).trim()
        val cat = TaskCategory.fromName(categoryStr)
        return cat to cleanTitle
    }
    val lower = trimmed.lowercase(Locale.ROOT)
    val inferredCat = when {
        lower.contains("lab report") || lower.contains("practical") -> TaskCategory.LAB_REPORT
        lower.contains("project") || lower.contains("capstone") -> TaskCategory.PROJECT
        lower.contains("quiz") || lower.contains("exam") || lower.contains("test") -> TaskCategory.EXAM_PREP
        lower.contains("reading") || lower.contains("chapter") -> TaskCategory.READING
        else -> TaskCategory.ASSIGNMENT
    }
    return inferredCat to trimmed
}

@Composable
internal fun TasksNotificationPromptBanner(
    isDark: Boolean,
    areNotificationsAllowed: Boolean,
    onActionClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF78350F).copy(alpha = 0.4f) else Color(0xFFFEF3C7)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (!areNotificationsAllowed) {
                        "Enable notifications to receive deadline alarms."
                    } else {
                        "Deadline alert channel is muted in system settings."
                    },
                    fontSize = 12.sp,
                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                    lineHeight = 16.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onActionClick) {
                    Text(
                        if (!areNotificationsAllowed) "Allow" else "Settings",
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun TasksSearchField(
    query: String,
    isDark: Boolean,
    primaryColor: Color,
    cardBg: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by title or course...", fontSize = 14.sp, color = textSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor,
            unfocusedBorderColor = borderColor,
            focusedContainerColor = cardBg,
            unfocusedContainerColor = cardBg,
            focusedTextColor = textPrimary,
            unfocusedTextColor = textPrimary
        )
    )
}

@Composable
internal fun TasksFilterChipsRow(
    assignments: List<Assignment>,
    selectedFilter: String,
    isDark: Boolean,
    primaryColor: Color,
    cardBg: Color,
    borderColor: Color,
    textSecondary: Color,
    onFilterSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf("All", "Pending", "Overdue", "Completed", "High Priority")
        items(filters) { filter ->
            val isSelected = selectedFilter == filter
            val count = when (filter) {
                "Pending" -> assignments.count { !it.completed }
                "Overdue" -> assignments.count { !it.completed && getTaskDueStatus(it.dueDate, it.completed) == TaskDueStatus.OVERDUE }
                "Completed" -> assignments.count { it.completed }
                "High Priority" -> assignments.count { !it.completed && it.priority.equals("High", ignoreCase = true) }
                else -> assignments.size
            }
            val isOverdueFilter = filter == "Overdue"
            val chipBg = when {
                isSelected && isOverdueFilter -> Color(0xFFDC2626)
                isSelected -> primaryColor
                isOverdueFilter && count > 0 -> if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.4f) else Color(0xFFFEE2E2)
                else -> cardBg
            }
            val chipBorder = when {
                isSelected && isOverdueFilter -> Color(0xFFDC2626)
                isSelected -> primaryColor
                isOverdueFilter && count > 0 -> if (isDark) Color(0xFFB91C1C) else Color(0xFFFCA5A5)
                else -> borderColor
            }
            val chipTextColor = when {
                isSelected && isOverdueFilter -> Color.White
                isSelected -> MaterialTheme.colorScheme.onPrimary
                isOverdueFilter && count > 0 -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
                else -> textSecondary
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(chipBg)
                    .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                    .clickable { onFilterSelected(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverdueFilter && count > 0 && !isSelected) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = chipTextColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = "$filter ($count)",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = chipTextColor
                    )
                }
            }
        }
    }
}

@Composable
internal fun TasksEmptyStateCard(
    searchQuery: String,
    selectedFilter: String,
    primaryColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(primaryColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Assignment,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            val emptyTitle = when {
                searchQuery.isNotBlank() -> "No tasks match \"$searchQuery\""
                selectedFilter == "Overdue" -> "No overdue tasks"
                selectedFilter == "Completed" -> "No completed tasks yet"
                selectedFilter == "High Priority" -> "No high-priority tasks pending"
                selectedFilter == "Pending" -> "No pending tasks"
                else -> "No tasks scheduled"
            }
            val emptySubtitle = when {
                selectedFilter == "Overdue" -> "You're all caught up with your deadlines!"
                selectedFilter == "Completed" -> "Tasks you check off will appear here."
                else -> "Tap the '+' button to schedule an assignment, set deadlines, and configure reminders."
            }
            Text(
                text = emptyTitle,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = emptySubtitle,
                fontSize = 13.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Assignment,
    isDark: Boolean,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cardBg = if (isDark) DarkSurfaceCard else Color.White
    val textPrimary = if (isDark) Color.White else TextPrimaryLight
    val textSecondary = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val primaryColor = MaterialTheme.colorScheme.primary

    val (category, cleanTitle) = remember(task.title) { resolveTaskCategoryAndTitle(task.title) }
    val dueStatus = remember(task.dueDate, task.completed) { getTaskDueStatus(task.dueDate, task.completed) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 6.dp else 1.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isHighlighted) 2.dp else 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                when {
                    isHighlighted -> primaryColor
                    isDark && !task.completed && dueStatus == TaskDueStatus.OVERDUE -> Color(0xFFF87171).copy(alpha = 0.55f)
                    isDark -> DarkBorderSubtle
                    !task.completed && dueStatus == TaskDueStatus.OVERDUE -> Color(0xFFFCA5A5)
                    else -> Color(0xFFE2E8F0)
                }
            )
        ),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                2.dp,
                                if (task.completed) primaryColor else (if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1)),
                                CircleShape
                            )
                            .background(if (task.completed) primaryColor else Color.Transparent)
                            .clickable(onClick = onToggleComplete),
                        contentAlignment = Alignment.Center
                    ) {
                        if (task.completed) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) category.color.copy(alpha = 0.22f) else category.lightContainerColor,
                            border = BorderStroke(1.dp, category.color.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    category.icon,
                                    contentDescription = null,
                                    tint = category.color,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    category.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = category.color
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = cleanTitle,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (task.completed) textSecondary else textPrimary,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                            lineHeight = 20.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit or Reschedule Task",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (task.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, if (isDark) DarkBorderSubtle else Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 36.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier
                                .padding(top = 1.dp)
                                .size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.notes,
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 3,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isOverdueAndHigh = !task.completed && dueStatus == TaskDueStatus.OVERDUE && task.priority.equals("high", ignoreCase = true)

                if (task.completed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = if (isDark) Color(0xFF6EE7B7) else Color(0xFF16A34A),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Done",
                                color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF15803D),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                } else when (dueStatus) {
                    TaskDueStatus.OVERDUE -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.6f) else Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, if (isDark) Color(0xFFB91C1C) else Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    null,
                                    tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    if (isOverdueAndHigh) "Overdue • High Priority" else "Overdue",
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TaskDueStatus.DUE_TODAY -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) else Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    null,
                                    tint = if (isDark) Color(0xFFFCD34D) else Color(0xFFD97706),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "Today",
                                    color = if (isDark) Color(0xFFFCD34D) else Color(0xFFB45309),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    TaskDueStatus.DUE_TOMORROW -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    null,
                                    tint = primaryColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "Tomorrow",
                                    color = primaryColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    else -> {}
                }

                if (!isOverdueAndHigh) {
                    val (badgeBg, badgeText) = when (task.priority.lowercase(Locale.ROOT)) {
                        "high" -> {
                            if (isDark) Color(0xFF7F1D1D).copy(alpha = 0.5f) to Color(0xFFFCA5A5)
                            else Color(0xFFFFE4E6) to Color(0xFFE11D48)
                        }
                        "medium" -> {
                            if (isDark) Color(0xFF78350F).copy(alpha = 0.5f) to Color(0xFFFCD34D)
                            else Color(0xFFFEF3C7) to Color(0xFFD97706)
                        }
                        else -> {
                            if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.5f) to Color(0xFF93C5FD)
                            else Color(0xFFEFF6FF) to Color(0xFF2563EB)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .background(badgeBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            task.priority.replaceFirstChar { it.uppercase(Locale.ROOT) },
                            color = badgeText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
                ) {
                    Text(
                        task.courseCode,
                        fontSize = 11.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(task.dueDate, fontSize = 12.sp, color = textSecondary)

                if (task.reminderMinutes != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                if (task.completed) {
                                    if (isDark) DarkSurfaceBase else Color(0xFFF1F5F9)
                                } else {
                                    if (isDark) Color(0xFF1E3A8A).copy(alpha = 0.4f) else Color(0xFFEFF6FF)
                                },
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (task.completed) textSecondary else primaryColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                if (task.completed) "Off" else "${task.reminderMinutes}m",
                                fontSize = 10.sp,
                                color = if (task.completed) textSecondary else primaryColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
