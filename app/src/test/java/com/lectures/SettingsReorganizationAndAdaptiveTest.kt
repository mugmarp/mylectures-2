package com.lectures

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.settings.SettingsScreen
import com.lectures.ui.AppTheme
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsReorganizationAndAdaptiveTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: TimetableApplication
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runBlocking {
            app.repository.setProgrammePref("BCS I")
            app.repository.setOnboardingCompleted(true)
            app.repository.setNotificationsEnabled(true)
            app.repository.setDailyDigestEnabled(true)
            app.repository.setClassAlarmLeadMinutes(30)
            app.repository.setTaskReminderLeadHours(2)
        }
        resetReminderChannelsToDefault()
    }

    @After
    fun tearDown() {
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        resetReminderChannelsToDefault()
    }

    private fun resetReminderChannelsToDefault() {
        NotificationHelper.createNotificationChannels(app)
        listOf(
            NotificationHelper.CHANNEL_CLASS_ALARMS to NotificationManager.IMPORTANCE_HIGH,
            NotificationHelper.CHANNEL_CLASSES to NotificationManager.IMPORTANCE_HIGH,
            NotificationHelper.CHANNEL_ACADEMIC_TASKS to NotificationManager.IMPORTANCE_HIGH,
            NotificationHelper.CHANNEL_DAILY_DIGEST to NotificationManager.IMPORTANCE_DEFAULT
        ).forEach { (id, importance) ->
            notificationManager.getNotificationChannel(id)?.let { channel ->
                channel.importance = importance
            }
        }
    }

    private fun configuredCopy(widthDp: Int, heightDp: Int, fontScale: Float = 1.0f): Configuration {
        return Configuration(app.resources.configuration).apply {
            screenWidthDp = widthDp
            screenHeightDp = heightDp
            this.fontScale = fontScale
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun mobileSettingsHubOrganizesIntoFourSectionsAndNavigatesSubScreensWithBackHandling() {
        var backDispatcher: OnBackPressedDispatcher? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_notifications_switch").fetchSemanticsNodes().isNotEmpty()
        }

        // Hub displays Academic Profile + 4 focused category entry points
        composeRule.onNodeWithText("ACADEMIC PROFILE").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notifications_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notifications_configure_row")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("settings_appearance_card")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("settings_data_sync_card")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("settings_about_legal_card")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        // Detailed sections must NOT crowd the root hub
        assertEquals(0, composeRule.onAllNodesWithText("DANGER ZONE").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Reset All Lecture Notes").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Export Data Backup (JSON)").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Anonymous Feedback & Bugs").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Privacy Policy").fetchSemanticsNodes().size)

        // Open Data, Sync & Reset sub-screen
        composeRule.onNodeWithTag("settings_data_sync_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Data, Sync & Reset").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("ACCOUNT & CLOUD SYNC").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_google_signin_btn")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("TIMETABLE & LOCAL BACKUP").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Export Data Backup (JSON)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("DANGER ZONE").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset All Lecture Notes").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Reset Academic Tasks & Alarms").performScrollTo().assertIsDisplayed()

        // Top-bar Back returns to root hub
        composeRule.onNodeWithContentDescription("Back")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_about_legal_card").fetchSemanticsNodes().isNotEmpty()
        }

        // Open About, Updates & Legal sub-screen
        composeRule.onNodeWithTag("settings_about_legal_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("About, Updates & Legal").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("RELEASES & TIMETABLE DATA").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("DEVELOPER & SUPPORT").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("LEGAL & PRIVACY").performScrollTo().assertIsDisplayed()

        // Verify Disclaimer & Terms dialog uses shortened "I Understand" action with >= 48dp target
        composeRule.onNodeWithText("Disclaimer & Terms").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("I Understand")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithText("I Understand").fetchSemanticsNodes().size)

        // Verify Timetable Version & Mode dialog opens cleanly and dismisses
        composeRule.onNodeWithText("Timetable Version & Mode").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Academic Session").assertIsDisplayed()
        composeRule.onNodeWithText("Close")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithText("Academic Session").fetchSemanticsNodes().size)

        // System BackHandler returns from ABOUT_LEGAL to root hub
        composeRule.runOnUiThread {
            backDispatcher?.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_data_sync_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun mobileSubScreensKeepFeedbackBannersPinnedAndVisible() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_notifications_configure_row").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_notifications_configure_row").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_daily_digest_switch").fetchSemanticsNodes().isNotEmpty()
        }

        // Trigger feedback inside sub-screen; banner must stay pinned visible inside Notifications
        composeRule.onNodeWithTag("settings_daily_digest_switch").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Morning schedule digest", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithText("Notifications & Alarms").assertIsDisplayed()
        composeRule.onNodeWithText("Morning schedule digest", substring = true).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Dismiss")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("Morning schedule digest", substring = true).fetchSemanticsNodes().size
        )
    }

    @Test
    @Config(qualifiers = "w320dp-h720dp")
    fun narrowScreenWith150PercentFontScaleWrapsChipsAndStacksCardsWithoutClipping() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuredCopy(320, 720, fontScale = 1.5f),
                LocalDensity provides Density(density = 2f, fontScale = 1.5f)
            ) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_notifications_configure_row").fetchSemanticsNodes().isNotEmpty()
        }

        // 1. Notifications sub-screen: all 6 class chips and 5 task chips wrap cleanly with 48dp+ height
        composeRule.onNodeWithTag("settings_notifications_configure_row").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_daily_digest_switch").fetchSemanticsNodes().isNotEmpty()
        }

        val rootRight = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.right
        val leadLabels = listOf("10m", "15m", "30m", "45m", "60m", "Custom", "1h", "2h", "6h", "12h", "1d")
        leadLabels.forEach { label ->
            val node = composeRule.onNodeWithText(label)
            node.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Chip '$label' right edge (${bounds.right}) should stay inside root width ($rootRight)",
                bounds.right <= rootRight + 1f
            )
        }

        composeRule.onNodeWithText("15m").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("15 minutes before", substring = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        // Return to hub and check Data & Backup sub-screen at 320dp + 1.5x fontScale
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_data_sync_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_data_sync_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_google_signin_btn").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_google_signin_btn")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Sync Now")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Export Data Backup (JSON)")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reset All Lecture Notes")
            .performScrollTo()
            .assertIsDisplayed()

        // Return to hub and check About & Legal sub-screen (FlowRow Stateless badge + rows)
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_about_legal_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_about_legal_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("RELEASES & TIMETABLE DATA").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Anonymous Feedback & Bugs").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Stateless").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Non-Affiliation Disclaimer").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Privacy Policy").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun notificationHealthBannerPluralizesMutedChannelsAndAllowsDismissingPermissionWarning() {
        NotificationHelper.createNotificationChannels(app)
        notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_CLASSES)?.importance =
            NotificationManager.IMPORTANCE_NONE
        assertEquals(1, NotificationPermissionHelper.getBlockedReminderChannels(app).size)

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("1 reminder category is turned off in Android Settings.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("1 reminder category is turned off in Android Settings.").assertIsDisplayed()
        composeRule.onNodeWithText("Unmute").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun notificationHealthBannerUsesPluralCopyForMultipleMutedChannelsAndDismissesDisabledAlert() {
        NotificationHelper.createNotificationChannels(app)
        notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_CLASSES)?.importance =
            NotificationManager.IMPORTANCE_NONE
        notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_ACADEMIC_TASKS)?.importance =
            NotificationManager.IMPORTANCE_NONE
        assertEquals(2, NotificationPermissionHelper.getBlockedReminderChannels(app).size)

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("2 reminder categories are turned off in Android Settings.")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("2 reminder categories are turned off in Android Settings.").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun notificationPermissionDeniedBannerMeets48dpAndCanBeDismissed() {
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Notifications Disabled").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Allow Notifications")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithContentDescription("Dismiss notification warning")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithText("Notifications Disabled").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun desktopDualPaneSwitchesAcrossAllCategoriesAndOverviewSummaryCards() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_category_rail").fetchSemanticsNodes().isNotEmpty()
        }

        // Rail selection -> DATA_SYNC
        composeRule.onNodeWithTag("settings_category_DATA_SYNC").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                hasText("ACCOUNT & CLOUD SYNC") and hasAnyAncestor(hasTestTag("settings_detail_pane"))
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_category_rail").assertIsDisplayed()
        composeRule.onNodeWithText("DANGER ZONE").performScrollTo().assertIsDisplayed()

        // Rail selection -> ABOUT_DANGER
        composeRule.onNodeWithTag("settings_category_ABOUT_DANGER").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                hasText("RELEASES & TIMETABLE DATA") and hasAnyAncestor(hasTestTag("settings_detail_pane"))
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("LEGAL & PRIVACY").performScrollTo().assertIsDisplayed()

        // Back to OVERVIEW -> summary cards route directly to corresponding detail categories
        composeRule.onNodeWithTag("settings_category_OVERVIEW").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_data_sync_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_data_sync_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("TIMETABLE & LOCAL BACKUP").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_category_OVERVIEW").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_about_legal_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_about_legal_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("DEVELOPER & SUPPORT").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
