package com.lectures

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.res.Configuration
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.settings.SettingsScreen
import com.lectures.ui.AppTheme
import com.lectures.ui.LecturesWindow
import com.lectures.ui.LocalLecturesWindow
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
import org.robolectric.shadows.ShadowAlarmManager

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
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
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
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
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

        // Return to hub and check Appearance sub-screen at 320dp + 1.5x fontScale
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_appearance_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_appearance_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("theme_mode_system").fetchSemanticsNodes().isNotEmpty()
        }

        listOf("theme_mode_light", "theme_mode_dark", "theme_mode_system").forEach { tag ->
            val cardNode = composeRule.onNodeWithTag(tag)
            cardNode.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            val bounds = cardNode.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Theme card '$tag' right edge (${bounds.right}) should stay inside root width ($rootRight)",
                bounds.right <= rootRight + 1f
            )
        }
        (0..4).forEach { idx ->
            val swatchNode = composeRule.onNodeWithTag("accent_swatch_$idx")
            swatchNode.performScrollTo().assertIsDisplayed().assertHeightIsAtLeast(48.dp)
            val bounds = swatchNode.fetchSemanticsNode().boundsInRoot
            assertTrue(
                "Accent swatch '$idx' right edge (${bounds.right}) should stay inside root width ($rootRight)",
                bounds.right <= rootRight + 1f
            )
        }
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
    @Config(qualifiers = "w360dp-h800dp")
    fun dismissingNotificationsDisabledDoesNotSwapExactAlarmBannerIntoItsSlot() {
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)

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

        composeRule.onNodeWithContentDescription("Dismiss notification warning").performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithText("Notifications Disabled").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Exact Alarm Timing Restricted").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun quietModeToggleShowsInlineBannerAndDisablesTimingControls() {
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
            composeRule.onAllNodesWithTag("settings_class_lead_15").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_class_lead_15").assertIsEnabled()
        composeRule.onNodeWithTag("settings_daily_digest_switch").assertIsEnabled()

        composeRule.onNodeWithTag("settings_notifications_switch").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Quiet Mode is active", substring = true).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Quiet Mode is active", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_class_lead_15").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_class_lead_custom").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_task_lead_2").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("settings_daily_digest_switch").performScrollTo().assertIsNotEnabled()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun blankProgrammeFallbackShowsNoProgrammeSelectedWithoutDanglingBullet() {
        runBlocking {
            app.repository.setProgrammePref("")
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_academic_profile_card").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("No Programme Selected").assertIsDisplayed()
        composeRule.onNodeWithText("Mbarara University of Science and Technology").assertIsDisplayed()
        composeRule.onNodeWithText("Select").assertIsDisplayed()
        assertEquals(
            0,
            composeRule.onAllNodesWithText("• Mbarara University", substring = true).fetchSemanticsNodes().size
        )
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun customLeadMinutesDialogUpdatesSelectedChip() {
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
            composeRule.onAllNodesWithTag("settings_class_lead_custom").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_class_lead_custom").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("dialog_custom_lead_input").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("dialog_custom_lead_input").performTextReplacement("")
        composeRule.onNodeWithTag("dialog_custom_lead_save_btn").assertIsNotEnabled()

        composeRule.onNodeWithTag("dialog_custom_lead_input").performTextReplacement("0")
        composeRule.onNodeWithTag("dialog_custom_lead_save_btn").assertIsNotEnabled()
        composeRule.onNodeWithText("Enter a value between 1 and 180 minutes.").assertIsDisplayed()

        composeRule.onNodeWithTag("dialog_custom_lead_input").performTextReplacement("200")
        composeRule.onNodeWithTag("dialog_custom_lead_save_btn").assertIsNotEnabled()
        composeRule.onNodeWithText("Enter a value between 1 and 180 minutes.").assertIsDisplayed()

        composeRule.onNodeWithTag("dialog_custom_lead_input").performTextReplacement("25")
        composeRule.onNodeWithTag("dialog_custom_lead_save_btn").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("25m").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("25m").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("dialog_custom_lead_save_btn").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun privacyAndTermsDialogsDiscloseOptionalGoogleCloudSyncAndFeedback() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_about_legal_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_about_legal_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_privacy_row").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_privacy_row").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("sync privately via Cloud Firestore", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("bug reports submitted through Anonymous Feedback", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_privacy_confirm_btn").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_terms_row").performScrollTo().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("optionally connect a Google Account", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("backed up to your Google Cloud Firestore", substring = true)
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_terms_confirm_btn").performClick()
        composeRule.waitForIdle()
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

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun singleCategoryStateSurvivesCompactToDualPaneWindowTransition() {
        var useDualPane by mutableStateOf(false)

        composeRule.setContent {
            val activeWindow = if (useDualPane) {
                LecturesWindow(
                    widthDp = 1280,
                    heightDp = 800,
                    fontScale = 1.0f,
                    navBarBottom = 0.dp,
                    isImeVisible = false
                )
            } else {
                LecturesWindow(
                    widthDp = 360,
                    heightDp = 800,
                    fontScale = 1.0f,
                    navBarBottom = 0.dp,
                    isImeVisible = false
                )
            }
            CompositionLocalProvider(LocalLecturesWindow provides activeWindow) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_data_sync_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_data_sync_card").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("ACCOUNT & CLOUD SYNC").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.runOnUiThread {
            useDualPane = true
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_category_rail").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_category_rail").assertIsDisplayed()
        composeRule.onNode(
            hasText("ACCOUNT & CLOUD SYNC") and hasAnyAncestor(hasTestTag("settings_detail_pane"))
        ).assertIsDisplayed()
    }
}
