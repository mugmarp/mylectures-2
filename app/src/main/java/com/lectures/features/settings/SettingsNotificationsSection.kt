package com.lectures.features.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import com.lectures.ui.contrastOnColor
import com.lectures.ui.rememberPrefersReducedMotion

private fun formatTaskLeadShort(hours: Int): String = if (hours >= 24) {
    val days = hours / 24
    "${days}d"
} else {
    "${hours}h"
}

@Composable
internal fun NotificationHealthWarningBanner(
    context: Context,
    isDark: Boolean,
    areNotificationsAllowed: Boolean,
    canScheduleExactAlarms: Boolean,
    blockedChannels: List<String>,
    isPermissionBannerDismissed: Boolean,
    horizontalPadding: Dp = 20.dp,
    onDismissPermissionBanner: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val reducedMotion = rememberPrefersReducedMotion()
    val hasBanner = (!areNotificationsAllowed && !isPermissionBannerDismissed) ||
        (areNotificationsAllowed && blockedChannels.isNotEmpty()) ||
        (areNotificationsAllowed && blockedChannels.isEmpty() && !canScheduleExactAlarms)

    AnimatedVisibility(
        visible = hasBanner,
        enter = if (reducedMotion) fadeIn() else expandVertically() + fadeIn(),
        exit = if (reducedMotion) fadeOut() else shrinkVertically() + fadeOut()
    ) {
        Column {
            if (!areNotificationsAllowed && !isPermissionBannerDismissed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.warningBg),
                    border = BorderStroke(1.dp, ext.warningBorder.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ext.warningBorder.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = ext.warningIcon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notifications Disabled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ext.warningText
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Allow notifications in Android Settings so class alarms and deadline alerts can ring.",
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = ext.warningText.copy(alpha = 0.9f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val allowContainer = if (isDark) Color(0xFFD97706) else Color(0xFFB45309)
                            Button(
                                onClick = { NotificationPermissionHelper.openAppNotificationSettings(context) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = allowContainer,
                                    contentColor = contrastOnColor(allowContainer)
                                ),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .desktopHandCursor()
                            ) {
                                Text("Allow Notifications", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onDismissPermissionBanner,
                            modifier = Modifier
                                .size(48.dp)
                                .desktopHandCursor()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification warning",
                                tint = ext.warningText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (areNotificationsAllowed && blockedChannels.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.warningBg),
                    border = BorderStroke(1.dp, ext.warningBorder.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = null,
                            tint = ext.warningIcon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alert Category Muted",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ext.warningText
                            )
                            val mutedCount = blockedChannels.size
                            Text(
                                text = if (mutedCount == 1) {
                                    "1 reminder category is turned off in Android Settings."
                                } else {
                                    "$mutedCount reminder categories are turned off in Android Settings."
                                },
                                fontSize = 12.sp,
                                color = ext.warningText.copy(alpha = 0.9f)
                            )
                        }
                        TextButton(
                            onClick = { NotificationPermissionHelper.openChannelSettings(context, blockedChannels.first()) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .desktopHandCursor()
                        ) {
                            Text(
                                "Unmute",
                                fontWeight = FontWeight.Bold,
                                color = ext.warningIcon
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else if (areNotificationsAllowed && !canScheduleExactAlarms) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.warningBg),
                    border = BorderStroke(1.dp, ext.warningBorder.copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = ext.warningIcon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Exact Alarm Timing Restricted",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = ext.warningText
                            )
                            Text(
                                text = "Allow 'Alarms & reminders' so lecture alerts ring at the exact minute.",
                                fontSize = 12.sp,
                                color = ext.warningText.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { NotificationPermissionHelper.openExactAlarmSettings(context) },
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .desktopHandCursor()
                                .testTag("settings_exact_alarm_fix_btn")
                        ) {
                            Text(
                                text = "Fix",
                                fontWeight = FontWeight.Bold,
                                color = ext.warningIcon
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun SettingsNotificationsSection(
    context: Context,
    uiState: SettingsUiState,
    isDark: Boolean,
    areNotificationsAllowed: Boolean,
    canScheduleExactAlarms: Boolean,
    blockedChannels: List<String>,
    isPermissionBannerDismissed: Boolean,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onDismissPermissionBanner: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenNotificationPreferences: () -> Unit
) {
    val ext = LocalExtendedColors.current
    SectionHeader("NOTIFICATIONS & ALARMS", horizontalPadding = horizontalPadding + 4.dp)

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
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .desktopHoverable()
                    .desktopHandCursor()
                    .clickable { onToggleNotifications(!uiState.notificationsEnabled) }
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .testTag("settings_notifications_toggle_row"),
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
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            !uiState.notificationsEnabled -> "Paused · Quiet mode active"
                            uiState.dailyDigestEnabled -> "Active · Lectures, deadlines & morning digest"
                            else -> "Active · Lectures & coursework deadlines"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isTightFont) 2 else 1,
                        overflow = TextOverflow.Ellipsis
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

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 18.dp),
                color = ext.borderSubtle
            )

            val taskLeadShort = formatTaskLeadShort(uiState.taskReminderLeadHours)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                    .desktopHoverable()
                    .desktopHandCursor()
                    .clickable(onClick = onOpenNotificationPreferences)
                    .padding(horizontal = 18.dp, vertical = 15.dp)
                    .testTag("settings_notifications_configure_row"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reminder Timing & Sound",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Classes: ${uiState.classAlarmLeadMinutes}m · Tasks: $taskLeadShort · Digest ${if (uiState.dailyDigestEnabled) "on" else "off"}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isTightFont) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open notification preferences",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
