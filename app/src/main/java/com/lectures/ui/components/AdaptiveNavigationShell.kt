package com.lectures.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.core.util.TimeUtil
import com.lectures.ui.Chrome
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MainScreenDestination

/**
 * Persistent leading NavigationRail (Medium/Expanded 600–1199dp or collapsed desktop)
 * and Expanded Desktop Sidebar (228dp on >= 1200dp) rendered continuously across all
 * primary tabs and utility workspaces (Vacant Rooms, Account & Sync, Settings).
 */
@Composable
fun AdaptiveNavigationRailOrSidebar(
    expandedSidebar: Boolean,
    canToggleSidebar: Boolean,
    sidebarCollapsed: Boolean,
    onToggleSidebar: () -> Unit,
    currentDestination: MainScreenDestination,
    selectedTab: Int,
    savedProgramme: String?,
    onSelectTab: (Int) -> Unit,
    onSelectDestination: (MainScreenDestination) -> Unit,
    onOpenAcademicProfile: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val haptic = LocalHapticFeedback.current
    val primaryBlue = MaterialTheme.colorScheme.primary
    val textMuted = ext.textMuted
    val navBg = ext.surfaceCard
    val navBorder = ext.borderSubtle

    val tabs = listOf("Timetable", "Calendar", "Notes", "Tasks")
    val filledIcons = listOf(
        Icons.Filled.CalendarToday,
        Icons.Filled.CalendarMonth,
        Icons.AutoMirrored.Filled.Notes,
        Icons.Filled.TaskAlt
    )
    val outlinedIcons = listOf(
        Icons.Outlined.CalendarToday,
        Icons.Outlined.CalendarMonth,
        Icons.AutoMirrored.Outlined.Notes,
        Icons.Outlined.TaskAlt
    )

    val isExpandedMode = expandedSidebar && !sidebarCollapsed
    val railWidth = if (isExpandedMode) Chrome.ExpandedSidebarWidth else Chrome.NavigationRailWidth

    Surface(
        color = navBg.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, navBorder),
        modifier = Modifier
            .width(railWidth)
            .fillMaxHeight()
            .animateContentSize(animationSpec = tween(durationMillis = 180))
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 10.dp, horizontal = if (isExpandedMode) 12.dp else 6.dp),
                horizontalAlignment = if (isExpandedMode) Alignment.Start else Alignment.CenterHorizontally
            ) {
                // Header Brand Emblem + Sidebar Toggle (Desktop >= 1200dp)
                if (isExpandedMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(primaryBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = "Lectures",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Lectures",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ext.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Week ${TimeUtil.currentAcademicWeek()}",
                                    fontSize = 11.sp,
                                    color = ext.textMuted,
                                    maxLines = 1
                                )
                            }
                        }
                        if (canToggleSidebar) {
                            IconButton(
                                onClick = onToggleSidebar,
                                modifier = Modifier
                                    .size(40.dp)
                                    .desktopHandCursor()
                                    .testTag("desktop_sidebar_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.MenuOpen,
                                    contentDescription = "Collapse sidebar",
                                    tint = ext.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Active Programme & Peek Context Card
                    val progLabel = savedProgramme?.takeIf { it.isNotBlank() } ?: "Set Programme"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = primaryBlue.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, primaryBlue.copy(alpha = 0.22f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .desktopHoverable()
                            .clickable(onClick = onOpenAcademicProfile)
                            .testTag("sidebar_programme_badge")
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = "PROGRAMME",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.6.sp,
                                color = primaryBlue
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = progLabel,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ext.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "WORKSPACE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = ext.textMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(primaryBlue)
                            .let { mod ->
                                if (canToggleSidebar) {
                                    mod.desktopHandCursor().clickable(onClick = onToggleSidebar)
                                } else mod
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (canToggleSidebar && sidebarCollapsed) Icons.Default.Menu else Icons.Default.School,
                            contentDescription = if (canToggleSidebar) "Expand sidebar" else "Lectures",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Primary Tabs
                tabs.forEachIndexed { index, tab ->
                    val isSelected = currentDestination == MainScreenDestination.TABS && selectedTab == index
                    val icon = if (isSelected) filledIcons[index] else outlinedIcons[index]
                    val tag = "nav_${tab.lowercase(java.util.Locale.ROOT)}"

                    if (isExpandedMode) {
                        DesktopSidebarItem(
                            label = tab,
                            shortcut = "^${index + 1}",
                            icon = icon,
                            isSelected = isSelected,
                            testTag = tag,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(index)
                            }
                        )
                    } else {
                        CompactRailItem(
                            label = tab,
                            icon = icon,
                            isSelected = isSelected,
                            testTag = tag,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(index)
                            }
                        )
                    }
                }

                if (isExpandedMode) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = ext.borderSubtle, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "CAMPUS & SYNC",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp,
                        color = ext.textMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Secondary / Utility Persistent Destinations
                val isRoomsSelected = currentDestination == MainScreenDestination.VACANT_ROOMS
                val isAccountSelected = currentDestination == MainScreenDestination.ACCOUNT
                val isSettingsSelected = currentDestination == MainScreenDestination.SETTINGS

                if (isExpandedMode) {
                    DesktopSidebarItem(
                        label = "Vacant Rooms",
                        shortcut = "^5",
                        icon = if (isRoomsSelected) Icons.Filled.MeetingRoom else Icons.Outlined.MeetingRoom,
                        isSelected = isRoomsSelected,
                        testTag = "nav_rail_vacant_rooms",
                        onClick = { onSelectDestination(MainScreenDestination.VACANT_ROOMS) }
                    )
                    DesktopSidebarItem(
                        label = "Account & Sync",
                        shortcut = "^6",
                        icon = if (isAccountSelected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                        isSelected = isAccountSelected,
                        testTag = "nav_rail_account",
                        onClick = { onSelectDestination(MainScreenDestination.ACCOUNT) }
                    )
                    DesktopSidebarItem(
                        label = "Settings",
                        shortcut = "^,",
                        icon = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                        isSelected = isSettingsSelected,
                        testTag = "nav_rail_settings",
                        onClick = { onSelectDestination(MainScreenDestination.SETTINGS) }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                } else {
                    CompactUtilityRailButton(
                        label = "Rooms",
                        icon = if (isRoomsSelected) Icons.Filled.MeetingRoom else Icons.Outlined.MeetingRoom,
                        isSelected = isRoomsSelected,
                        testTag = "nav_rail_vacant_rooms",
                        onClick = { onSelectDestination(MainScreenDestination.VACANT_ROOMS) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactUtilityRailButton(
                        label = "Account",
                        icon = if (isAccountSelected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                        isSelected = isAccountSelected,
                        testTag = "nav_rail_account",
                        onClick = { onSelectDestination(MainScreenDestination.ACCOUNT) }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactUtilityRailButton(
                        label = "Settings",
                        icon = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                        isSelected = isSettingsSelected,
                        testTag = "nav_rail_settings",
                        onClick = { onSelectDestination(MainScreenDestination.SETTINGS) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactRailItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val primary = MaterialTheme.colorScheme.primary
    val pillBg by animateColorAsState(
        targetValue = if (isSelected) primary.copy(alpha = 0.16f) else Color.Transparent,
        animationSpec = tween(150),
        label = "rail_pill"
    )

    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp, 30.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(pillBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) primary else ext.textMuted,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) primary else ext.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CompactUtilityRailButton(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) primary.copy(alpha = 0.14f) else Color.Transparent)
            .desktopHoverable()
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) primary else ext.textMuted,
                modifier = Modifier.size(21.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) primary else ext.textMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DesktopSidebarItem(
    label: String,
    shortcut: String,
    icon: ImageVector,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val primary = MaterialTheme.colorScheme.primary
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) primary.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(150),
        label = "sidebar_item_bg"
    )

    Row(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .desktopHoverable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) primary else ext.textMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(11.dp))
        Text(
            text = label,
            fontSize = 13.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) primary else ext.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = shortcut,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isSelected) primary.copy(alpha = 0.8f) else ext.textMuted.copy(alpha = 0.65f)
        )
    }
}
