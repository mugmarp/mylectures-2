package com.lectures

import android.Manifest
import android.content.res.Configuration
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
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
import androidx.test.core.app.ApplicationProvider
import com.lectures.ui.AppTheme
import com.lectures.ui.MainScaffold
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainScaffoldSettingsNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: TimetableApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        runBlocking {
            app.repository.setProgrammePref("BCS I")
            app.repository.setOnboardingCompleted(true)
            app.repository.setNotificationsEnabled(true)
        }
    }

    private fun configuredCopy(widthDp: Int, heightDp: Int): Configuration {
        return Configuration(app.resources.configuration).apply {
            screenWidthDp = widthDp
            screenHeightDp = heightDp
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun mobileSettingsAccountBackPreservesDataSyncSubScreenBeforeReturningToHubAndTimetable() {
        var backDispatcher: OnBackPressedDispatcher? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
                    MainScaffold()
                }
            }
        }

        // Open Settings via Mobile Timetable top-bar Academic Info sheet
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("topbar_academic_info_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("topbar_academic_info_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("academic_sheet_settings_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("academic_sheet_settings_btn").performClick()

        // Navigate into Data, Sync & Reset -> Sign In -> AccountScreen
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_data_sync_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_data_sync_card").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_google_signin_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_google_signin_btn").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("account_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Account & Cloud Sync").assertIsDisplayed()

        // Account back button returns directly to Data, Sync & Reset (NOT Timetable)
        composeRule.onNodeWithTag("account_back_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("ACCOUNT & CLOUD SYNC").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Data, Sync & Reset").assertIsDisplayed()

        // Re-open AccountScreen and verify system BackHandler preserves the same return path
        composeRule.onNodeWithTag("settings_google_signin_btn").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("account_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.runOnUiThread {
            backDispatcher?.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("ACCOUNT & CLOUD SYNC").fetchSemanticsNodes().isNotEmpty()
        }

        // Next back returns to Settings Hub, and following back returns to Timetable
        composeRule.runOnUiThread {
            backDispatcher?.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_data_sync_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Settings").assertIsDisplayed()

        composeRule.runOnUiThread {
            backDispatcher?.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("topbar_academic_info_btn").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("topbar_academic_info_btn").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun desktopSettingsAccountBackPreservesSelectedCategoryInSettingsScreen() {
        var backDispatcher: OnBackPressedDispatcher? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
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

        // System BackHandler from Account returns directly to Settings with DATA_SYNC selected
        composeRule.runOnUiThread {
            backDispatcher?.onBackPressed()
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(
                hasText("ACCOUNT & CLOUD SYNC") and hasAnyAncestor(hasTestTag("settings_detail_pane"))
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_category_rail").assertIsDisplayed()
    }
}
