package com.lectures.features.settings

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.rememberPrefersReducedMotion

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotificationSettingsView(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    context: Context,
    uiState: SettingsUiState,
    isDark: Boolean,
    areNotificationsAllowed: Boolean,
    canScheduleExactAlarms: Boolean,
    blockedChannels: List<String>,
    isPermissionBannerDismissed: Boolean,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    bottomContentPadding: Dp = 32.dp,
    onDismissPermissionBanner: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectClassLeadMinutes: (Int) -> Unit,
    onOpenCustomLeadDialog: () -> Unit,
    onSelectTaskLeadHours: (Int) -> Unit,
    onToggleDailyDigest: (Boolean) -> Unit
) {
    val ext = LocalExtendedColors.current
    val headerHorizontalPadding = horizontalPadding + 4.dp
    val reducedMotion = rememberPrefersReducedMotion()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        NotificationHealthWarningBanner(
            context = context,
            isDark = isDark,
            areNotificationsAllowed = areNotificationsAllowed,
            canScheduleExactAlarms = canScheduleExactAlarms,
            blockedChannels = blockedChannels,
            isPermissionBannerDismissed = isPermissionBannerDismissed,
            horizontalPadding = horizontalPadding,
            onDismissPermissionBanner = onDismissPermissionBanner
        )

        // Master Switch Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (uiState.notificationsEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (uiState.notificationsEnabled) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                        contentDescription = null,
                        tint = if (uiState.notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Class & Task Reminders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (uiState.notificationsEnabled) {
                            "Enabled for scheduled lectures, tasks, and activities"
                        } else {
                            "Quiet Mode active — all scheduled alerts are paused"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = onToggleNotifications,
                    modifier = Modifier
                        .desktopHandCursor()
                        .testTag("settings_notifications_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("REMINDER TIMING", horizontalPadding = headerHorizontalPadding)

        val timingEnabled = uiState.notificationsEnabled
        val timingAlpha by animateFloatAsState(
            targetValue = if (timingEnabled) 1f else 0.6f,
            animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
            label = "notif_timing_alpha"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .alpha(timingAlpha),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                if (!timingEnabled) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp)
                    ) {
                        Text(
                            text = "Quiet Mode is active — turn on Class & Task Reminders above to adjust alert timing.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                // 1. Lecture & Class Lead Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Before Lectures & Labs",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Alert lead time before class begins",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val classLeadOptions = listOf(10 to "10m", 15 to "15m", 30 to "30m", 45 to "45m", 60 to "60m")
                val isCustomActive = uiState.classAlarmLeadMinutes !in listOf(10, 15, 30, 45, 60)

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wrapClassChips = maxWidth < 315.dp || (isTightFont && maxWidth < 400.dp)
                    if (wrapClassChips) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            classLeadOptions.forEach { (mins, label) ->
                                LeadTimeChip(
                                    label = label,
                                    isSelected = uiState.classAlarmLeadMinutes == mins,
                                    enabled = timingEnabled,
                                    reducedMotion = reducedMotion,
                                    testTag = "settings_class_lead_$mins",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectClassLeadMinutes(mins) }
                                )
                            }
                            LeadTimeChip(
                                label = if (isCustomActive) "${uiState.classAlarmLeadMinutes}m" else "Custom",
                                isSelected = isCustomActive,
                                enabled = timingEnabled,
                                reducedMotion = reducedMotion,
                                testTag = "settings_class_lead_custom",
                                modifier = Modifier.weight(1f),
                                onClick = onOpenCustomLeadDialog
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            classLeadOptions.forEach { (mins, label) ->
                                LeadTimeChip(
                                    label = label,
                                    isSelected = uiState.classAlarmLeadMinutes == mins,
                                    enabled = timingEnabled,
                                    reducedMotion = reducedMotion,
                                    testTag = "settings_class_lead_$mins",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectClassLeadMinutes(mins) }
                                )
                            }
                            LeadTimeChip(
                                label = if (isCustomActive) "${uiState.classAlarmLeadMinutes}m" else "Custom",
                                isSelected = isCustomActive,
                                enabled = timingEnabled,
                                reducedMotion = reducedMotion,
                                testTag = "settings_class_lead_custom",
                                modifier = Modifier.weight(1.3f),
                                onClick = onOpenCustomLeadDialog
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = ext.borderSubtle
                )

                // 2. Task & Deadline Lead Time
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Before Task Deadlines",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Default lead time for new coursework tasks",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val taskLeadOptions = listOf(1 to "1h", 2 to "2h", 6 to "6h", 12 to "12h", 24 to "1d")
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val wrapTaskChips = maxWidth < 290.dp || (isTightFont && maxWidth < 360.dp)
                    if (wrapTaskChips) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 3
                        ) {
                            taskLeadOptions.forEach { (hours, label) ->
                                LeadTimeChip(
                                    label = label,
                                    isSelected = uiState.taskReminderLeadHours == hours,
                                    enabled = timingEnabled,
                                    reducedMotion = reducedMotion,
                                    testTag = "settings_task_lead_$hours",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectTaskLeadHours(hours) }
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            taskLeadOptions.forEach { (hours, label) ->
                                LeadTimeChip(
                                    label = label,
                                    isSelected = uiState.taskReminderLeadHours == hours,
                                    enabled = timingEnabled,
                                    reducedMotion = reducedMotion,
                                    testTag = "settings_task_lead_$hours",
                                    modifier = Modifier.weight(1f),
                                    onClick = { onSelectTaskLeadHours(hours) }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = ext.borderSubtle
                )

                // 3. Morning Schedule Digest Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Morning Schedule Digest",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Daily 7:30 AM summary of today's classes & rooms",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.dailyDigestEnabled,
                        onCheckedChange = onToggleDailyDigest,
                        enabled = timingEnabled,
                        modifier = Modifier
                            .desktopHandCursor(enabled = timingEnabled)
                            .testTag("settings_daily_digest_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("SOUND & SYSTEM DELIVERY", horizontalPadding = headerHorizontalPadding)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .desktopHoverable()
                        .desktopHandCursor()
                        .clickable { NotificationPermissionHelper.openAppNotificationSettings(context) }
                        .padding(18.dp)
                        .testTag("settings_system_sound_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sounds, Vibration & Banners",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Customize alert tones and vibration in Android Settings",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open Android notification settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    color = ext.borderSubtle
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                        .desktopHoverable()
                        .desktopHandCursor()
                        .clickable { NotificationPermissionHelper.openExactAlarmSettings(context) }
                        .padding(18.dp)
                        .testTag("settings_exact_alarm_row"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                if (canScheduleExactAlarms) ext.successBg else ext.warningBg,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (canScheduleExactAlarms) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = if (canScheduleExactAlarms) ext.successIcon else ext.warningIcon,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exact Alarm Timing",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (canScheduleExactAlarms) {
                                "On time · Lectures and tasks ring at the exact minute"
                            } else {
                                "Delayed · Tap to allow 'Alarms & reminders'"
                            },
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    SettingsStatusPill(
                        text = if (canScheduleExactAlarms) "Active" else "Fix",
                        containerColor = if (canScheduleExactAlarms) ext.successBg else ext.warningBg,
                        contentColor = if (canScheduleExactAlarms) ext.successText else ext.warningText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
private fun LeadTimeChip(
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    reducedMotion: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val chipBg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "lead_chip_bg_$testTag"
    )
    val chipText by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
        label = "lead_chip_text_$testTag"
    )
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .desktopHandCursor(enabled = enabled)
            .background(chipBg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = chipText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
