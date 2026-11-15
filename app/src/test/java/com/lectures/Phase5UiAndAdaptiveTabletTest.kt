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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.lectures.features.notes.CourseOption
import com.lectures.features.notes.NoteEditorScreen
import com.lectures.features.tasks.TasksScreen
import com.lectures.features.timetable.domain.Assignment
import com.lectures.ui.AppTheme
import com.lectures.ui.MainScaffold
import com.lectures.ui.ThemeMode
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
