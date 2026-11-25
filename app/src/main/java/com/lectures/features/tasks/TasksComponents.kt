package com.lectures.features.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TaskCategory
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberLecturesWindow
import com.lectures.ui.rememberPrefersReducedMotion
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
    val ext = LocalExtendedColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ext.warningBg),
        border = BorderStroke(1.dp, ext.warningBorder.copy(alpha = 0.55f))
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
                    tint = ext.warningIcon,
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
                    color = ext.warningText,
                    lineHeight = 16.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onActionClick,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .desktopHandCursor()
                ) {
                    Text(
                        if (!areNotificationsAllowed) "Allow" else "Settings",
                        fontWeight = FontWeight.Bold,
                        color = ext.warningText
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .desktopHandCursor()
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = ext.warningText,
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
    focusRequester: FocusRequester? = null,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("Search by title or course...", fontSize = 14.sp, color = textSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = textSecondary, modifier = Modifier.size(20.dp)) },
        shape = RoundedCornerShape(20.dp),
        singleLine = true,
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
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

@OptIn(ExperimentalLayoutApi::class)
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
    val windowInfo = rememberLecturesWindow()
    val reducedMotion = rememberPrefersReducedMotion()
    val filters = listOf("All", "Pending", "Overdue", "Completed", "High Priority")

    val chipContent: @Composable (String) -> Unit = { filter ->
        val isSelected = selectedFilter == filter
        val count = when (filter) {
            "Pending" -> assignments.count { !it.completed }
            "Overdue" -> assignments.count { !it.completed && getTaskDueStatus(it.dueDate, it.completed) == TaskDueStatus.OVERDUE }
            "Completed" -> assignments.count { it.completed }
            "High Priority" -> assignments.count { !it.completed && it.priority.equals("High", ignoreCase = true) }
            else -> assignments.size
        }
        val ext = LocalExtendedColors.current
        val isOverdueFilter = filter == "Overdue"
        val targetChipBg = when {
            isSelected && isOverdueFilter -> ext.dangerIcon
            isSelected -> primaryColor
            isOverdueFilter && count > 0 -> ext.dangerBg
            else -> cardBg
        }
        val targetChipBorder = when {
            isSelected && isOverdueFilter -> ext.dangerIcon
            isSelected -> primaryColor
            isOverdueFilter && count > 0 -> ext.dangerBorder
            else -> borderColor
        }
        val targetChipTextColor = when {
            isSelected && isOverdueFilter -> contrastOnColor(ext.dangerIcon)
            isSelected -> contrastOnColor(primaryColor)
            isOverdueFilter && count > 0 -> ext.dangerText
            else -> textSecondary
        }

        val chipBg by animateColorAsState(
            targetValue = targetChipBg,
            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
            label = "task_filter_bg"
        )
        val chipBorder by animateColorAsState(
            targetValue = targetChipBorder,
            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
            label = "task_filter_border"
        )
        val chipTextColor by animateColorAsState(
            targetValue = targetChipTextColor,
            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
            label = "task_filter_text"
        )

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(chipBg)
                .border(1.dp, chipBorder, RoundedCornerShape(16.dp))
                .desktopHandCursor()
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

    if (windowInfo.isTablet) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter -> chipContent(filter) }
        }
    } else {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { filter -> chipContent(filter) }
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

@OptIn(ExperimentalLayoutApi::class)
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
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val cardBg = ext.surfaceCard
    val textPrimary = ext.textPrimary
    val textSecondary = ext.textSecondary
    val primaryColor = MaterialTheme.colorScheme.primary

    val (category, cleanTitle) = remember(task.title) { resolveTaskCategoryAndTitle(task.title) }
    val dueStatus = remember(task.dueDate, task.completed) { getTaskDueStatus(task.dueDate, task.completed) }

    val targetBorderColor = when {
        isHighlighted -> primaryColor
        !task.completed && dueStatus == TaskDueStatus.OVERDUE -> ext.dangerBorder
        else -> ext.borderSubtle
    }
    val cardBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "task_card_border"
    )
    val cardBorderWidth by animateDpAsState(
        targetValue = if (isHighlighted) 2.dp else 1.dp,
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "task_card_border_w"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (task.completed) 0.82f else 1f,
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "task_card_alpha"
    )
    val checkBg by animateColorAsState(
        targetValue = if (task.completed) primaryColor else Color.Transparent,
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "task_check_bg"
    )
    val checkBorder by animateColorAsState(
        targetValue = if (task.completed) primaryColor else ext.textMuted.copy(alpha = 0.55f),
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "task_check_border"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (task.completed) 1.06f else 1f,
        animationSpec = if (reducedMotion) androidx.compose.animation.core.snap() else MotionSpec.bouncyCheckSpring(),
        label = "task_check_scale"
    )
    val titleColor by animateColorAsState(
        targetValue = if (task.completed) textSecondary else textPrimary,
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "task_title_color"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 6.dp else 1.dp),
        border = BorderStroke(cardBorderWidth, cardBorderColor),
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = cardAlpha }
            .clip(RoundedCornerShape(18.dp))
            .desktopHandCursor()
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .desktopHoverable()
                .padding(start = 4.dp, top = 4.dp, end = 12.dp, bottom = 16.dp)
        ) {
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .desktopHandCursor()
                            .semantics {
                                contentDescription = if (task.completed) "Mark task incomplete" else "Mark task complete"
                            }
                            .testTag("task_toggle_${task.id}")
                            .clickable(onClick = onToggleComplete),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                                .clip(CircleShape)
                                .border(2.dp, checkBorder, CircleShape)
                                .background(checkBg),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = task.completed,
                                enter = if (reducedMotion) fadeIn() else scaleIn(animationSpec = MotionSpec.bouncyCheckSpring()) + fadeIn(),
                                exit = if (reducedMotion) fadeOut() else scaleOut() + fadeOut()
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.padding(top = 12.dp)) {
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
                            color = titleColor,
                            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
                            lineHeight = 20.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit or Reschedule Task",
                            tint = primaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(48.dp)
                            .desktopHandCursor()
                    ) {
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
                    color = ext.surfaceBase,
                    border = BorderStroke(1.dp, ext.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 48.dp, end = 4.dp)
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

            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isOverdueAndHigh = !task.completed && dueStatus == TaskDueStatus.OVERDUE && task.priority.equals("high", ignoreCase = true)

                if (task.completed) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ext.successBg,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = ext.successIcon,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                "Done",
                                color = ext.successText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else when (dueStatus) {
                    TaskDueStatus.OVERDUE -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ext.dangerBg,
                            border = BorderStroke(1.dp, ext.dangerBorder),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.WarningAmber,
                                    null,
                                    tint = ext.dangerIcon,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    if (isOverdueAndHigh) "Overdue • High Priority" else "Overdue",
                                    color = ext.dangerText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    TaskDueStatus.DUE_TODAY -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ext.warningBg,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    null,
                                    tint = ext.warningIcon,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "Today",
                                    color = ext.warningText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    TaskDueStatus.DUE_TOMORROW -> {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ext.infoBg,
                            modifier = Modifier.align(Alignment.CenterVertically)
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
                    }
                    else -> {}
                }

                if (!isOverdueAndHigh) {
                    val (badgeBg, badgeText) = when (task.priority.lowercase(Locale.ROOT)) {
                        "high" -> ext.dangerBg to ext.dangerText
                        "medium" -> ext.warningBg to ext.warningText
                        else -> ext.infoBg to ext.infoText
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
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

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ext.surfaceElevated,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Text(
                        task.courseCode,
                        fontSize = 11.sp,
                        color = textSecondary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = ext.warningIcon,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(task.dueDate, fontSize = 12.sp, color = textSecondary)
                }

                if (task.reminderMinutes != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .background(
                                if (task.completed) ext.surfaceElevated else ext.infoBg,
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
