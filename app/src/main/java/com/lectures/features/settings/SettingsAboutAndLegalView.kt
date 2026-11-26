package com.lectures.features.settings

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
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lectures.BuildConfig
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.components.desktopHandCursor
import com.lectures.ui.components.desktopHoverable
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AboutAndLegalSettingsView(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    uiState: SettingsUiState,
    isTightFont: Boolean = false,
    horizontalPadding: Dp = 20.dp,
    bottomContentPadding: Dp = 32.dp,
    onCheckOrOpenUpdate: () -> Unit,
    onShowVersionDetails: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenDisclaimer: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit
) {
    val ext = LocalExtendedColors.current
    val headerHorizontalPadding = horizontalPadding + 4.dp

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(top = 14.dp)
    ) {
        SectionHeader("RELEASES & TIMETABLE DATA", horizontalPadding = headerHorizontalPadding)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val availableUpdate = uiState.availableUpdateInfo
                AboutActionRow(
                    title = "Lectures App Releases",
                    subtitle = if (availableUpdate != null) {
                        "Update available: v${availableUpdate.versionName} · Tap to install"
                    } else {
                        "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) · Tap to check updates"
                    },
                    subtitleColor = if (availableUpdate != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    titleFontSize = 15.sp,
                    titleFontWeight = FontWeight.SemiBold,
                    isTightFont = isTightFont,
                    leadingStartPadding = 54.dp,
                    testTag = "settings_app_releases_row",
                    onClick = onCheckOrOpenUpdate,
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.SystemUpdate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    trailing = {
                        if (uiState.isCheckingForAppUpdate) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (availableUpdate != null) {
                            SettingsStatusPill(
                                text = "Update",
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            SettingsStatusPill(
                                text = "Check",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = ext.borderSubtle
                )

                val isFinalRelease = uiState.isFinal || uiState.timetableVersion.contains("Final", ignoreCase = true)
                val badgeText = uiState.metadata?.displayBadge ?: if (isFinalRelease) "FINAL" else uiState.timetableStatus.uppercase(Locale.ROOT)
                val badgeBg = if (isFinalRelease) ext.successBg else ext.warningBg
                val badgeColor = if (isFinalRelease) ext.successText else ext.warningText

                AboutActionRow(
                    title = "Timetable Version & Mode",
                    subtitle = "${uiState.timetableVersion} · ${uiState.metadata?.semester ?: "Semester 1"}",
                    titleFontSize = 15.sp,
                    titleFontWeight = FontWeight.SemiBold,
                    isTightFont = isTightFont,
                    leadingStartPadding = 54.dp,
                    testTag = "settings_version_details_row",
                    onClick = onShowVersionDetails,
                    leading = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(badgeBg, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Outlined.Verified,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    trailing = {
                        SettingsStatusPill(
                            text = badgeText,
                            containerColor = badgeBg,
                            contentColor = badgeColor
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("DEVELOPER & SUPPORT", horizontalPadding = headerHorizontalPadding)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AboutActionRow(
                    title = "Developer",
                    subtitle = null,
                    titleFontSize = 15.sp,
                    titleFontWeight = FontWeight.Bold,
                    isTightFont = isTightFont,
                    testTag = "settings_developer_row",
                    onClick = onOpenDeveloper,
                    trailing = {
                        SettingsStatusPill(
                            text = "Profile",
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = ext.borderSubtle
                )

                AboutActionRow(
                    title = "Anonymous Feedback & Bugs",
                    subtitle = "Report timetable errors, venue bugs, or suggestions anonymously",
                    titleFontSize = 13.sp,
                    titleFontWeight = FontWeight.SemiBold,
                    isTightFont = isTightFont,
                    testTag = "settings_feedback_row",
                    onClick = onOpenFeedback,
                    titleBadge = {
                        SettingsStatusPill(
                            text = "Stateless",
                            containerColor = ext.successBg,
                            contentColor = ext.successText
                        )
                    },
                    trailing = {
                        SettingsStatusPill(
                            text = "Open",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionHeader("LEGAL & PRIVACY", horizontalPadding = headerHorizontalPadding)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ext.surfaceCard),
            border = BorderStroke(1.dp, ext.borderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AboutActionRow(
                    title = "Non-Affiliation Disclaimer",
                    subtitle = "Independent app — not affiliated with Mbarara University (MUST)",
                    titleFontSize = 13.sp,
                    titleFontWeight = FontWeight.SemiBold,
                    isTightFont = isTightFont,
                    testTag = "settings_disclaimer_row",
                    onClick = onOpenDisclaimer,
                    trailing = {
                        SettingsStatusPill(
                            text = "View",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = ext.borderSubtle
                )

                AboutActionRow(
                    title = "Disclaimer & Terms",
                    subtitle = "Permitted use, warranty disclaimers & academic policies",
                    titleFontSize = 13.sp,
                    titleFontWeight = FontWeight.SemiBold,
                    isTightFont = isTightFont,
                    testTag = "settings_terms_row",
                    onClick = onOpenTerms,
                    trailing = {
                        SettingsStatusPill(
                            text = "View",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = ext.borderSubtle
                )

                AboutActionRow(
                    title = "Privacy Policy",
                    subtitle = "Local-first SQLite storage & privacy policy",
                    titleFontSize = 13.sp,
                    titleFontWeight = FontWeight.SemiBold,
                    isTightFont = isTightFont,
                    testTag = "settings_privacy_row",
                    onClick = onOpenPrivacy,
                    trailing = {
                        SettingsStatusPill(
                            text = "View",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomContentPadding))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutActionRow(
    title: String,
    subtitle: String?,
    isTightFont: Boolean,
    testTag: String,
    onClick: () -> Unit,
    titleFontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
    titleFontWeight: FontWeight = FontWeight.SemiBold,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leadingStartPadding: Dp = 0.dp,
    leading: (@Composable () -> Unit)? = null,
    titleBadge: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackTrailing = maxWidth < 260.dp || (isTightFont && maxWidth < 340.dp)
        val rowModifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .desktopHoverable()
            .desktopHandCursor()
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 6.dp, horizontal = 4.dp)

        val textColumn: @Composable (Modifier) -> Unit = { colMod ->
            Column(modifier = colMod) {
                if (titleBadge != null) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            fontWeight = titleFontWeight,
                            fontSize = titleFontSize,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
                            titleBadge()
                        }
                    }
                } else {
                    Text(
                        text = title,
                        fontWeight = titleFontWeight,
                        fontSize = titleFontSize,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                }
            }
        }

        if (stackTrailing) {
            Column(
                modifier = rowModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leading != null) {
                        leading()
                        Spacer(modifier = Modifier.width(14.dp))
                    }
                    textColumn(Modifier.weight(1f))
                }
                Box(modifier = Modifier.padding(start = leadingStartPadding)) {
                    trailing()
                }
            }
        } else {
            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    leading()
                    Spacer(modifier = Modifier.width(14.dp))
                }
                textColumn(Modifier.weight(1f))
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
    }
}
