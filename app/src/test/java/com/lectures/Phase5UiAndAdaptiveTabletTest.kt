package com.lectures

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.lectures.features.feedback.AnonymousFeedbackSheet
import com.lectures.features.feedback.FeedbackManager
import com.lectures.features.notes.CourseOption
import com.lectures.features.notes.NoteEditorScreen
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.tasks.TasksScreen
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.domain.Assignment
import com.lectures.ui.AppTheme
import com.lectures.ui.MainScaffold
import com.lectures.ui.ThemeMode
import com.lectures.ui.components.NotificationCenterSheet
import com.lectures.ui.rememberLecturesWindow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase5UiAndAdaptiveTabletTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: TimetableApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            app.repository.setProgrammePref("BCS I")
            app.repository.setOnboardingCompleted(true)
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
    fun lecturesWindowClassifiesCompactMediumExpandedAndShortCorrectly() {
        var compactWindow: com.lectures.ui.LecturesWindow? = null
        var mediumWindow: com.lectures.ui.LecturesWindow? = null
        var expandedWindow: com.lectures.ui.LecturesWindow? = null
        var tightFontWindow: com.lectures.ui.LecturesWindow? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800, 1.0f)) {
                compactWindow = rememberLecturesWindow()
            }
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(674, 841, 1.0f)) {
                mediumWindow = rememberLecturesWindow()
            }
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800, 1.0f)) {
                expandedWindow = rememberLecturesWindow()
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuredCopy(1280, 800, 1.5f),
                LocalDensity provides Density(density = 2f, fontScale = 1.5f)
            ) {
                tightFontWindow = rememberLecturesWindow()
            }
        }
        composeRule.waitForIdle()

        assertTrue(compactWindow!!.isCompact)
        assertFalse(compactWindow!!.useNavigationRail)
        assertFalse(compactWindow!!.isDualPane)

        assertTrue(mediumWindow!!.isMedium)
        assertTrue(mediumWindow!!.isTablet)
        assertTrue(mediumWindow!!.useNavigationRail)
        assertFalse(mediumWindow!!.isDualPane)

        assertTrue(expandedWindow!!.isExpanded)
        assertTrue(expandedWindow!!.useNavigationRail)
        assertTrue(expandedWindow!!.isDualPane)

        assertTrue(tightFontWindow!!.isTightFont)
        assertTrue(tightFontWindow!!.dockIconOnly)
    }

    @Test
    fun compactPhoneRendersFloatingBottomDockWithoutNavigationRail() {
        composeRule.setContent {
            val config = configuredCopy(widthDp = 360, heightDp = 800)
            CompositionLocalProvider(LocalConfiguration provides config) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Timetable").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("nav_timetable").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_calendar").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_notes").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_tasks").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_rail_vacant_rooms").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w674dp-h841dp")
    fun mediumTabletRendersNavigationRailWithAccessibleTouchTargetsAndOpensVacantRooms() {
        composeRule.setContent {
            val config = configuredCopy(widthDp = 674, heightDp = 841)
            CompositionLocalProvider(LocalConfiguration provides config) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Timetable").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("nav_rail_vacant_rooms")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("nav_rail_settings")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeRule.onNodeWithTag("nav_rail_vacant_rooms").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Vacant Room Finder").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Vacant Room Finder").assertIsDisplayed()
    }

    @Test
    fun expandedTabletWithLargeFontScaleRendersDualPaneTasksCleanly() {
        runBlocking {
            app.repository.saveAssignment(
                Assignment(
                    title = "[Project] Distributed Systems Lab",
                    courseCode = "CSC3102",
                    dueDate = "Friday • 17:00",
                    priority = "High",
                    completed = false
                )
            )
        }

        composeRule.setContent {
            val config = configuredCopy(widthDp = 1280, heightDp = 800, fontScale = 1.5f)
            CompositionLocalProvider(
                LocalConfiguration provides config,
                LocalDensity provides Density(density = 2f, fontScale = 1.5f)
            ) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    TasksScreen()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Tasks & Deadlines").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText("Tasks & Deadlines").assertIsDisplayed()
        composeRule.onNodeWithTag("add_task_fab").assertIsDisplayed()
    }

    @Test
    fun noteEditorPreservesDraftTitleAndContentAcrossSavedInstanceStateRestoration() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent {
            AppTheme(themeMode = ThemeMode.LIGHT) {
                NoteEditorScreen(
                    initialNote = null,
                    availableCourses = listOf(CourseOption("CSC1101", "Structured Programming", "#2563EB")),
                    timetableClasses = emptyList(),
                    onSave = { _, _, _, _, _, _, _, _, _ -> },
                    onCancel = {}
                )
            }
        }

        composeRule.onNodeWithText("Enter note title or lecture topic...")
            .performTextInput("Concurrency Patterns")
        composeRule.waitForIdle()

        restorationTester.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Concurrency Patterns").assertIsDisplayed()
    }

    @Test
    fun settingsNotificationsUsesCompactSummaryAndSubScreenWithoutTestButtons() {
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        composeRule.setContent {
            val config = configuredCopy(widthDp = 360, heightDp = 800)
            CompositionLocalProvider(LocalConfiguration provides config) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }
        composeRule.waitForIdle()

        // No warning banner clutter when notifications and exact alarms are healthy
        assertEquals(0, composeRule.onAllNodesWithText("Exact Alarm Timing Granted").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("settings_notifications_switch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notifications_configure_row").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Test Alert").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Re-Arm All").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("OS Channels").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("settings_notifications_configure_row").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Morning Schedule Digest").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sounds, Vibration & Banners").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_daily_digest_switch").performScrollTo().assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Test Alert").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Re-Arm All").fetchSemanticsNodes().size)
    }

    @Test
    fun notificationCenterSheetHidesDeveloperTestAlarmCard() {
        composeRule.setContent {
            AppTheme(themeMode = ThemeMode.LIGHT) {
                NotificationCenterSheet(onDismiss = {})
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("notification_center_settings_btn").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Test Alarm & Alert").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithText("Class Alert").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w640dp-h840dp")
    fun mainScaffoldFoldableConstrainsInnerPaneBelow600dpWhileKeepingNavigationRail() {
        runBlocking {
            app.repository.saveNote(
                naturalKey = "BCS1101|foldable_test_note",
                content = "Foldable pane test content",
                title = "Discrete Math",
                tag = "Lecture"
            )
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(640, 840)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_notes").fetchSemanticsNodes().isNotEmpty()
        }

        // Rail visible on 640dp shell, but inner pane is 640 - 80 - 1 = 559dp (< 600dp),
        // so Notes collapses cleanly instead of squeezing desktop split panes.
        composeRule.onNodeWithTag("nav_notes").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_card_BCS1101|foldable_test_note").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, composeRule.onAllNodesWithTag("notes_quick_preview_pane").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun mainScaffoldDesktopKeepsWideMultiPaneWorkspacesWhen228dpSidebarIsOpen() {
        runBlocking {
            app.repository.saveNote(
                naturalKey = "BCS1101|desktop_shell_note",
                content = "# Trees\nAVL trees rebalance in O(log n).",
                title = "AVL Trees",
                tag = "Lecture"
            )
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("desktop_sidebar_toggle").fetchSemanticsNodes().isNotEmpty()
        }

        // With 228dp expanded sidebar open (remaining pane = 1051dp >= 960dp),
        // Notes must still open desktop split preview and Vacant Rooms must still open 3-pane workspace.
        composeRule.onNodeWithTag("nav_notes").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("notes_quick_preview_pane").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("notes_quick_preview_pane").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_rail_vacant_rooms").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("vacant_rooms_filter_rail").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("vacant_rooms_filter_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("vacant_rooms_grid_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("vacant_rooms_inspector_pane").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun noteEditorSegmentedTabsHaveAccessible48dpTouchTargetsOnPhone() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    NoteEditorScreen(
                        initialNote = null,
                        availableCourses = listOf(CourseOption("CSC1101", "Structured Programming", "#2563EB")),
                        timetableClasses = emptyList(),
                        onSave = { _, _, _, _, _, _, _, _, _ -> },
                        onCancel = {}
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_editor_write_tab").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("note_editor_write_tab")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("note_editor_preview_tab")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun onboardingFlowNavigatesWelcomeThroughClassGroupToMainApp() {
        runBlocking {
            app.repository.setOnboardingCompleted(false)
            app.repository.setProgrammePref("")
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("get_started_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("get_started_button").performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("onboarding_faculty_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("onboarding_faculty_list")
            .performScrollToNode(hasText("Computing & Informatics"))
        composeRule.onNodeWithText("Computing & Informatics").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("BCS").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("BCS").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Continue to Timetable").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Continue to Timetable").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_timetable").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_timetable").assertIsDisplayed()
        val onboardingDone = app.repository.isOnboardingCompleted()
        assertTrue(onboardingDone)
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun anonymousFeedbackSheetValidatesEmptyInputAndConfirmsSubmission() {
        val feedbackManager = FeedbackManager(app, ETagStore(app))
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(411, 891)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    AnonymousFeedbackSheet(
                        feedbackManager = feedbackManager,
                        onDismiss = {}
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("feedback_submit_button").fetchSemanticsNodes().isNotEmpty()
        }

        // Disabled until user enters feedback message
        composeRule.onNodeWithTag("feedback_submit_button").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithTag("feedback_message_input")
            .performScrollTo()
            .performTextInput("Room status worked accurately during lab prep.")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("feedback_submit_button").performScrollTo().performClick()
        composeRule.mainClock.advanceTimeBy(800)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Thank You for the Report!").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Thank You for the Report!").assertIsDisplayed()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase5ActivityRecreationTest {

    @get:Rule
    val emptyComposeRule = createEmptyComposeRule()

    @Test
    fun mainActivityDoesNotReplayInitialNotificationIntentAfterRecreation() {
        val app = ApplicationProvider.getApplicationContext<TimetableApplication>()
        runBlocking {
            app.repository.setProgrammePref("BCS I")
            app.repository.setOnboardingCompleted(true)
        }

        val launchIntent = Intent(app, MainActivity::class.java).apply {
            putExtra("EXTRA_NAV_TAB", 3) // Notification deep-link -> Tasks tab
        }

        ActivityScenario.launch<MainActivity>(launchIntent).use { scenario ->
            emptyComposeRule.waitUntil(timeoutMillis = 5_000) {
                emptyComposeRule.onAllNodesWithText("Tasks & Deadlines").fetchSemanticsNodes().isNotEmpty()
            }
            emptyComposeRule.onNodeWithText("Tasks & Deadlines").assertIsDisplayed()

            // User switches manually from Tasks to Notes tab
            emptyComposeRule.onNodeWithTag("nav_notes").performClick()
            emptyComposeRule.waitUntil(timeoutMillis = 5_000) {
                emptyComposeRule.onAllNodesWithText("Academic Notes").fetchSemanticsNodes().isNotEmpty()
            }
            emptyComposeRule.onNodeWithText("Academic Notes").assertIsDisplayed()

            // Recreate Activity (simulates rotation / configuration change / dark mode toggle)
            scenario.recreate()

            // Must preserve selectedTab == Notes and NOT replay EXTRA_NAV_TAB = 3
            emptyComposeRule.waitUntil(timeoutMillis = 5_000) {
                emptyComposeRule.onAllNodesWithText("Academic Notes").fetchSemanticsNodes().isNotEmpty()
            }
            emptyComposeRule.onNodeWithText("Academic Notes").assertIsDisplayed()
            assertEquals(0, emptyComposeRule.onAllNodesWithText("Tasks & Deadlines").fetchSemanticsNodes().size)
        }
    }
}
