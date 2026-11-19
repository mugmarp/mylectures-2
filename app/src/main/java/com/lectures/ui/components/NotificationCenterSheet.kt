package com.lectures.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lectures.core.notification.InAppNotificationItem
import com.lectures.core.notification.InAppNotificationStore
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.notification.NotificationItemType
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.ui.*
import com.lectures.ui.LocalAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
    activeNotesWithAlarms: List<LectureNote> = emptyList(),
    activeTasksWithAlarms: List<Assignment> = emptyList(),
    onCancelNoteAlarm: ((LectureNote) -> Unit)? = null,
    onCancelTaskAlarm: ((Assignment) -> Unit)? = null,
    onNavigateToTab: ((Int) -> Unit)? = null,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = LocalAppTheme.current.isDark
    val accentColor = MaterialTheme.colorScheme.primary
    val surfaceColor = if (isDark) DarkSurfaceCard else Color.White
    val textColor = if (isDark) Color.White else TextPrimaryLight
    val textSub = if (isDark) Color(0xFF94A3B8) else TextMutedLight
    val cardBg = if (isDark) DarkSurfaceBase else Color(0xFFF8FAFC)
    val cardBorder = if (isDark) DarkBorderSubtle else BorderSubtleLight

    val notifStore = remember(context) { InAppNotificationStore.getInstance(context) }
    val notificationHistory by notifStore.notifications.collectAsState()

    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Scheduled Alarms, 1 = Notification History

    var areNotificationsEnabled by remember(context) {
        mutableStateOf(NotificationPermissionHelper.areNotificationsEnabled(context))
    }
    var canExactAlarms by remember(context) {
        mutableStateOf(NotificationPermissionHelper.canScheduleExactAlarms(context))
    }
    var blockedChannels by remember(context) {
        mutableStateOf(NotificationPermissionHelper.getBlockedReminderChannels(context))
    }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                areNotificationsEnabled = NotificationPermissionHelper.areNotificationsEnabled(context)
                canExactAlarms = NotificationPermissionHelper.canScheduleExactAlarms(context)
                blockedChannels = NotificationPermissionHelper.getBlockedReminderChannels(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = surfaceColor,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .background(if (isDark) Color(0xFF475569) else Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(accentColor.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifications & Alarms",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = "Academic alerts, class countdowns & inbox",
                            fontSize = 13.sp,
                            color = textSub
                        )
                    }
                    IconButton(
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        },
                        modifier = Modifier.testTag("notification_center_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Notification settings",
                            tint = textSub
                        )
                    }
                }
            }

            // Show system alert health warning ONLY when action is needed
            val isFullyHealthy = areNotificationsEnabled && canExactAlarms && blockedChannels.isEmpty()
            if (!isFullyHealthy) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isDark) Color(0xFF78350F).copy(alpha = 0.3f) else Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                when {
                                    !areNotificationsEnabled -> NotificationPermissionHelper.openAppNotificationSettings(context)
                                    blockedChannels.isNotEmpty() -> NotificationPermissionHelper.openChannelSettings(context, blockedChannels.first())
                                    !canExactAlarms -> NotificationPermissionHelper.openExactAlarmSettings(context)
                                }
                            }
                            .testTag("notification_status_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                                        !areNotificationsEnabled -> "Notifications Paused"
                                        blockedChannels.isNotEmpty() -> "${blockedChannels.size} Alert Category Muted"
                                        else -> "Exact Alarm Timing Restricted"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                                )
                                Text(
                                    text = when {
                                        !areNotificationsEnabled -> "Tap to enable notifications in Android Settings"
                                        blockedChannels.isNotEmpty() -> "Tap to unmute alert categories in Android Settings"
                                        else -> "Tap to allow exact alarms so reminders ring on time"
                                    },
                                    fontSize = 11.sp,
                                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // Tab Selector: Scheduled Alarms vs Notification History
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val activeTotal = activeNotesWithAlarms.size + activeTasksWithAlarms.size
                    val unreadCount = notificationHistory.count { !it.isRead }

                    // Tab 0: Scheduled Alarms
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 0) surfaceColor else Color.Transparent)
                            .clickable { selectedSection = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Active Alarms ($activeTotal)",
                            fontSize = 12.sp,
                            fontWeight = if (selectedSection == 0) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedSection == 0) accentColor else textSub
                        )
                    }

                    // Tab 1: Alert History / Inbox
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selectedSection == 1) surfaceColor else Color.Transparent)
                            .clickable { selectedSection = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Alert History (${notificationHistory.size})",
                                fontSize = 12.sp,
                                fontWeight = if (selectedSection == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedSection == 1) accentColor else textSub
                            )
                            if (unreadCount > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                )
                            }
                        }
                    }
                }
            }

            // Section 0: Scheduled Alarms
            if (selectedSection == 0) {
                if (activeNotesWithAlarms.isEmpty() && activeTasksWithAlarms.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No custom alarms currently set",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'Set Alarm' on any lecture or task to schedule reminders.",
                                    fontSize = 12.sp,
                                    color = textSub,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(activeNotesWithAlarms) { note ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Alarm,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    val courseCode = note.naturalKey.split("|", ":").firstOrNull()?.trim() ?: "Class Note"
                                    val displayTitle = note.title?.ifBlank { null } ?: note.content.take(35).ifBlank { "Lecture Note" }
                                    Text(
                                        text = "$courseCode · ${note.alarmMinutes ?: 15}m before",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = displayTitle,
                                        fontSize = 11.sp,
                                        color = textSub
                                    )
                                }
                                if (onCancelNoteAlarm != null) {
                                    IconButton(
                                        onClick = { onCancelNoteAlarm(note) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AlarmOff,
                                            contentDescription = "Cancel Alarm",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    items(activeTasksWithAlarms) { task ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TaskAlt,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Task: ${task.title}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Text(
                                        text = "Due ${task.dueDate} · ${task.reminderMinutes ?: 30}m before",
                                        fontSize = 11.sp,
                                        color = textSub
                                    )
                                }
                                if (onCancelTaskAlarm != null) {
                                    IconButton(
                                        onClick = { onCancelTaskAlarm(task) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AlarmOff,
                                            contentDescription = "Cancel Reminder",
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: Alert History & Logging
            if (selectedSection == 1) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { notifStore.markAllAsRead() },
                            enabled = notificationHistory.any { !it.isRead }
                        ) {
                            Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mark all read", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { notifStore.clearAll() },
                            enabled = notificationHistory.isNotEmpty()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear history", fontSize = 12.sp)
                        }
                    }
                }

                if (notificationHistory.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = cardBg,
                            border = BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = textSub,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No notification history yet",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Fired alarms and reminders will be logged here.",
                                    fontSize = 11.sp,
                                    color = textSub
                                )
                            }
                        }
                    }
                } else {
                    items(notificationHistory, key = { it.id }) { item ->
                        val timeFormat = remember { SimpleDateFormat("h:mm a, MMM d", Locale.getDefault()) }
                        val timeStr = remember(item.timestamp) { timeFormat.format(Date(item.timestamp)) }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (item.isRead) cardBg else surfaceColor,
                            border = BorderStroke(1.dp, if (!item.isRead) accentColor.copy(alpha = 0.4f) else cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    notifStore.markAsRead(item.id)
                                    if (item.navTab != null && onNavigateToTab != null) {
                                        onNavigateToTab(item.navTab)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            when (item.type) {
                                                NotificationItemType.CLASS_ALARM -> Color(0xFFEF4444).copy(alpha = 0.15f)
                                                NotificationItemType.CLASS_REMINDER -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                NotificationItemType.TASK_REMINDER -> Color(0xFF2563EB).copy(alpha = 0.15f)
                                                else -> Color(0xFF8B5CF6).copy(alpha = 0.15f)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when (item.type) {
                                            NotificationItemType.CLASS_ALARM -> Icons.Default.Alarm
                                            NotificationItemType.TASK_REMINDER -> Icons.Outlined.TaskAlt
                                            else -> Icons.Outlined.Notifications
                                        },
                                        contentDescription = null,
                                        tint = when (item.type) {
                                            NotificationItemType.CLASS_ALARM -> Color(0xFFEF4444)
                                            NotificationItemType.CLASS_REMINDER -> Color(0xFF10B981)
                                            NotificationItemType.TASK_REMINDER -> Color(0xFF2563EB)
                                            else -> Color(0xFF8B5CF6)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = timeStr,
                                            fontSize = 10.sp,
                                            color = textSub
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.message,
                                        fontSize = 11.sp,
                                        color = textSub,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { notifStore.deleteNotification(item.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = textSub,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
