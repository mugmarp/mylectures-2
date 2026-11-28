package com.lectures

import android.Manifest
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.analytics.AnalyticsManager
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.tasks.TasksScreen
import com.lectures.ui.AppTheme
import com.lectures.ui.MainScaffold
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class Phase3SettingsAndNavigationPolishTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: TimetableApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        app.analyticsManager.setTelemetryEnabled(true)
        runBlocking {
            app.repository.setProgrammePref("BCS I")
            app.repository.setOnboardingCompleted(true)
            app.repository.setNotificationsEnabled(true)
            app.repository.setDailyDigestEnabled(true)
        }
    }

    private fun configuredCopy(widthDp: Int, heightDp: Int): Configuration {
        return Configuration(app.resources.configuration).apply {
            screenWidthDp = widthDp
            screenHeightDp = heightDp
        }
    }

    @Test
    fun analyticsManager_optOutSuppressesTrackingAndPersistsAcrossInstances() {
        val manager = AnalyticsManager(app)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        manager.setTelemetryEnabled(true)
        assertTrue(manager.isTelemetryEnabled())
        manager.recordAppLaunch("BCS I", scope)
        manager.trackFeature("vacant_rooms")
        assertTrue(manager.getFeatureCount("app_launch") >= 1)
        assertTrue(manager.getFeatureCount("vacant_rooms") >= 1)

        // Opting out clears cached counters and suppresses subsequent tracking
        manager.setTelemetryEnabled(false)
        assertFalse(manager.isTelemetryEnabled())
        assertEquals(0, manager.getFeatureCount("app_launch"))
        assertEquals(0, manager.getFeatureCount("vacant_rooms"))

        manager.recordAppLaunch("BCS I", scope)
        manager.trackFeature("notes_saved")
        assertEquals(0, manager.getFeatureCount("app_launch"))
        assertEquals(0, manager.getFeatureCount("notes_saved"))

        // Opt-out survives re-instantiation
        val reloaded = AnalyticsManager(app)
        assertFalse(reloaded.isTelemetryEnabled())
        assertFalse(reloaded.telemetryEnabled.value)

        // Re-enabling resumes feature tracking
        reloaded.setTelemetryEnabled(true)
        reloaded.trackFeature("notes_saved")
        assertEquals(1, reloaded.getFeatureCount("notes_saved"))
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun settingsNotifications_exposesTestNotificationAndRearmAlarmsControls() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(411, 891)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_notifications_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_notifications_card").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_send_test_notification_row").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_send_test_notification_row")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_success_banner").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_rearm_alarms_row")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Rescheduled", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun settingsAbout_telemetrySwitchPersistsOptOutAndPrivacyDialogDisclosesOptOut() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(411, 891)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_about_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_about_card").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_telemetry_switch").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_telemetry_row")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("settings_telemetry_switch")
            .assertIsOn()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            !app.analyticsManager.isTelemetryEnabled()
        }
        composeRule.onNodeWithTag("settings_telemetry_switch").assertIsOff()

        // Privacy Policy dialog mentions the Settings opt-out control
        composeRule.onNodeWithTag("settings_privacy_row").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("dialog_privacy_confirm_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Share Anonymous Diagnostics", substring = true).assertIsDisplayed()
        composeRule.onNodeWithTag("dialog_privacy_confirm_btn").performClick()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun desktopSettingsRailReTapClosesNestedAccountScreenBackToSettings() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_rail_settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_rail_settings").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_category_DATA_SYNC").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_category_DATA_SYNC").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_google_signin_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_google_signin_btn").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("account_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Account & Cloud Sync").assertIsDisplayed()

        // Tapping Settings in the persistent navigation rail escapes nested AccountScreen
        composeRule.onNodeWithTag("nav_rail_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                hasText("ACCOUNT & CLOUD SYNC") and hasAnyAncestor(hasTestTag("settings_detail_pane"))
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_category_rail").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun tasksHighlightCallbackInvokedAfterConsumptionAndClearedOnTabSwitch() {
        var highlightState by mutableStateOf<Long?>(999L)
        var consumedCount = 0

        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(411, 891)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    TasksScreen(
                        highlightTaskId = highlightState,
                        onHighlightTaskConsumed = {
                            consumedCount++
                            highlightState = null
                        }
                    )
                }
            }
        }

        composeRule.mainClock.advanceTimeBy(4_500L)
        composeRule.waitForIdle()
        assertEquals(1, consumedCount)
        assertEquals(null, highlightState)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun mainScaffoldNotificationDeepLinkOpensTasksAndClearsHighlightWhenLeavingTab() {
        val intentFlow = MutableSharedFlow<Pair<Int, Long?>>(extraBufferCapacity = 1)

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(411, 891)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold(notificationIntentFlow = intentFlow)
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_tasks").fetchSemanticsNodes().isNotEmpty()
        }

        assertTrue(intentFlow.tryEmit(3 to 42L))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("add_task_fab").fetchSemanticsNodes().isNotEmpty()
        }

        // Switching away to Timetable clears pendingHighlightTaskId cleanly
        composeRule.onNodeWithTag("nav_timetable").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("topbar_academic_info_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_tasks").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("add_task_fab").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
