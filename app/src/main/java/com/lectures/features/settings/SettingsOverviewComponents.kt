package com.lectures.features.settings

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.ui.AppThemeState
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.MotionSpec
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable

@Composable
internal fun SettingsCategoryRail(
    selectedCategory: SettingsCategory,
    isTightFont: Boolean,
    reducedMotion: Boolean,
    bottomPadding: Dp,
    railScrollState: ScrollState = rememberScrollState(),
    onSelectCategory: (SettingsCategory) -> Unit
) {
    val ext = LocalExtendedColors.current
    val categoryRailWidth = if (isTightFont) 316.dp else 272.dp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ext.surfaceCard,
        border = BorderStroke(1.dp, ext.borderSubtle),
        modifier = Modifier
            .width(categoryRailWidth)
            .fillMaxHeight()
            .padding(bottom = bottomPadding)
            .testTag("settings_category_rail")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(railScrollState)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )

            SettingsCategory.entries.forEach { category ->
                val isSelected = selectedCategory == category
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                    label = "settings_cat_bg"
                )
                val fgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                    animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                    label = "settings_cat_fg"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = MotionSpec.stateTween(reducedMotion = reducedMotion),
                    label = "settings_cat_icon"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .desktopHoverable(
                            hoverContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                        )
                        .background(bgColor, RoundedCornerShape(14.dp))
                        .clickable { onSelectCategory(category) }
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                        .testTag("settings_category_${category.name}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.title,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            color = fgColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = category.subtitle,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsOverviewContent(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    context: Context,
    uiState: SettingsUiState,
    appTheme: AppThemeState,
    isDark: Boolean,
    isTightFont: Boolean,
    horizontalPadding: Dp,
    bottomContentPadding: Dp,
    programmeDisplayName: String,
    activeProgramme: String,
    facultyDisplayName: String,
    areNotificationsAllowed: Boolean,
    canScheduleExactAlarms: Boolean,
    blockedChannels: List<String>,
    isPermissionBannerDismissed: Boolean,
    onDismissPermissionBanner: () -> Unit,
    onReconfigureAcademicProfile: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onOpenNotificationPreferences: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenDataAndBackup: () -> Unit,
    onOpenAboutAndLegal: () -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        SettingsAcademicProfileCard(
            programmeDisplayName = programmeDisplayName,
            activeProgramme = activeProgramme,
            facultyDisplayName = facultyDisplayName,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onReconfigureAcademicProfile = onReconfigureAcademicProfile
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsNotificationsSection(
            context = context,
            uiState = uiState,
            isDark = isDark,
            areNotificationsAllowed = areNotificationsAllowed,
            canScheduleExactAlarms = canScheduleExactAlarms,
            blockedChannels = blockedChannels,
            isPermissionBannerDismissed = isPermissionBannerDismissed,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onDismissPermissionBanner = onDismissPermissionBanner,
            onToggleNotifications = onToggleNotifications,
            onOpenNotificationPreferences = onOpenNotificationPreferences
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsAppearanceSection(
            appTheme = appTheme,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenAppearance = onOpenAppearance
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsDataSummaryCard(
            uiState = uiState,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenDataAndBackup = onOpenDataAndBackup
        )

        Spacer(modifier = Modifier.height(22.dp))

        SettingsAboutSummaryCard(
            uiState = uiState,
            isTightFont = isTightFont,
            horizontalPadding = horizontalPadding,
            onOpenAboutAndLegal = onOpenAboutAndLegal
        )

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@Composable
internal fun SettingsFeedbackBanners(
    modifier: Modifier = Modifier,
    errorMessage: String?,
    successMessage: String?,
    horizontalPadding: Dp = 20.dp,
    onDismiss: () -> Unit
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).desktopHandCursor()) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = successMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            successMessage?.let { success ->
                val ext = LocalExtendedColors.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = ext.successBg),
                    border = BorderStroke(1.dp, ext.successBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = ext.successIcon)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(success, color = ext.successText, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp).desktopHandCursor()) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = ext.successIcon, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun SettingsAcademicProfileCard(
    programmeDisplayName: String,
    activeProgramme: String,
    facultyDisplayName: String,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    onReconfigureAcademicProfile: () -> Unit
) {
    val ext = LocalExtendedColors.current
    SectionHeader("ACADEMIC PROFILE", horizontalPadding = horizontalPadding + 4.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .clip(RoundedCornerShape(16.dp))
            .desktopHoverable()
            .desktopHandCursor()
            .clickable(onClick = onReconfigureAcademicProfile)
            .testTag("settings_academic_profile_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
        border = BorderStroke(1.dp, ext.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth()
        ) {
            val stackSwitch = isTightFont && maxWidth < 300.dp
            val profileTextBlock: @Composable (Modifier) -> Unit = { colMod ->
                Column(modifier = colMod) {
                    Text(
                        text = programmeDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (isTightFont) 4 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (activeProgramme.isNotBlank()) {
                            "$activeProgramme • $facultyDisplayName"
                        } else {
                            facultyDisplayName
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isTightFont) 3 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            val switchPill: @Composable () -> Unit = {
                SettingsStatusPill(
                    text = if (activeProgramme.isNotBlank()) "Switch" else "Select",
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            }

            if (stackSwitch) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    profileTextBlock(Modifier.fillMaxWidth())
                    switchPill()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    profileTextBlock(Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(12.dp))
                    switchPill()
                }
            }
        }
    }
}
