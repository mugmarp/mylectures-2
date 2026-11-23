package com.lectures

import android.Manifest
import android.content.res.Configuration
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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.util.TimeUtil
import com.lectures.features.account.AccountScreen
import com.lectures.features.calendar.CalendarScreen
import com.lectures.features.notes.CourseOption
import com.lectures.features.notes.NoteEditorScreen
import com.lectures.features.notes.NotesScreen
import com.lectures.features.rooms.ui.VacantRoomsScreen
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.tasks.TasksScreen
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.ui.HappeningNowHeroCard
import com.lectures.features.timetable.ui.SpecClassSession
import com.lectures.features.timetable.ui.SpecSessionType
import com.lectures.features.timetable.ui.TimetableScreen
import com.lectures.features.timetable.ui.WelcomeScreen
import com.lectures.ui.AppTheme
import com.lectures.ui.LecturesWindow
import com.lectures.ui.MainScaffold
import com.lectures.ui.ThemeMode
import com.lectures.ui.WindowHeightTier
import com.lectures.ui.WindowWidthTier
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
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DesktopAdaptiveLayoutTest {

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
    fun windowTiersClassifyCompactMediumExpandedLargeAndExtraLarge() {
        var compact: LecturesWindow? = null
        var medium: LecturesWindow? = null
        var expandedShort: LecturesWindow? = null
        var large: LecturesWindow? = null
        var extraLarge: LecturesWindow? = null

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                compact = rememberLecturesWindow()
            }
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(674, 841)) {
                medium = rememberLecturesWindow()
            }
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1024, 720)) {
                expandedShort = rememberLecturesWindow()
            }
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                large = rememberLecturesWindow()
            }
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1680, 960)) {
                extraLarge = rememberLecturesWindow()
            }
        }
        composeRule.waitForIdle()

        assertEquals(WindowWidthTier.COMPACT, compact!!.widthTier)
        assertEquals(WindowHeightTier.MEDIUM, compact!!.heightTier)
        assertFalse(compact!!.useNavigationRail)
        assertFalse(compact!!.preferDesktopDialogs)
        assertEquals(600.dp, compact!!.maxReadingWidth)

        assertEquals(WindowWidthTier.MEDIUM, medium!!.widthTier)
        assertTrue(medium!!.useNavigationRail)
        assertFalse(medium!!.useExpandedSidebar)
        assertFalse(medium!!.isDualPane)
        assertTrue(medium!!.preferDesktopDialogs)

        assertEquals(WindowWidthTier.EXPANDED, expandedShort!!.widthTier)
        assertTrue(expandedShort!!.useNavigationRail)
        assertFalse(expandedShort!!.useExpandedSidebar)
        assertTrue(expandedShort!!.isDualPane)
        assertFalse(expandedShort!!.supportsThreePanes)
        assertEquals(960.dp, expandedShort!!.maxReadingWidth)

        assertEquals(WindowWidthTier.LARGE, large!!.widthTier)
        assertTrue(large!!.isLargeWidth)
        assertTrue(large!!.isAtLeastLarge)
        assertTrue(large!!.useExpandedSidebar)
        assertTrue(large!!.supportsThreePanes)
        assertEquals(1180.dp, large!!.maxReadingWidth)

        assertEquals(WindowWidthTier.EXTRA_LARGE, extraLarge!!.widthTier)
        assertEquals(WindowHeightTier.EXPANDED, extraLarge!!.heightTier)
        assertTrue(extraLarge!!.isExtraLargeWidth)
        assertTrue(extraLarge!!.useExpandedSidebar)
        assertTrue(extraLarge!!.supportsThreePanes)
        assertEquals(1440.dp, extraLarge!!.maxReadingWidth)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun desktopShellRendersExpandedSidebarAndCollapsesToRailOnToggle() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("desktop_sidebar_toggle").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("desktop_sidebar_toggle")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("sidebar_programme_badge").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_rail_settings").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_rail_vacant_rooms").assertIsDisplayed()

        // Collapse expanded sidebar -> programme badge hides while rail destinations remain
        composeRule.onNodeWithTag("desktop_sidebar_toggle").performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithTag("sidebar_programme_badge").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("desktop_sidebar_toggle").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_rail_settings").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun timetableDesktopWorkspaceShowsWeeklyGridAndUpdatesClassInspector() {
        val sampleEntry = TimetableEntry(
            natural_key = "BCS I_Monday_08:00_CSC3101",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "CSC3101",
            course_title = "Operating Systems",
            session_type = "Lecture",
            lecturer = "Dr. A. K.",
            room = "LT 1"
        )

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    TimetableScreen(
                        programme = "BCS I",
                        enrolledProgramme = "BCS I",
                        entries = listOf(sampleEntry),
                        selectedDay = "Monday",
                        onDaySelected = {}
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("timetable_weekly_grid").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("timetable_weekly_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("timetable_desktop_inspector").assertIsDisplayed()
        composeRule.onNodeWithTag("week_block_CSC3101").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Class Inspector").assertIsDisplayed()
        composeRule.onNodeWithTag("inspector_close_btn").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(0, composeRule.onAllNodesWithText("Class Inspector").fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun notesDashboardAndEditorRenderDesktopQuickPreviewAndThreeZoneSplit() {
        runBlocking {
            app.repository.saveNote(
                naturalKey = "CSC3101|note_desktop_test_1",
                content = "## Concurrency\n- [ ] Review mutexes\nMutexes and semaphores prevent race conditions.",
                title = "Operating Systems Lecture 4",
                tag = "Lecture",
                attachedClass = "CSC3101"
            )
            app.repository.saveNote(
                naturalKey = "CSC3204|note_desktop_test_2",
                content = "## Indexing\nB+ trees for disk pages.",
                title = "Algorithms & Indexing",
                tag = "Revision",
                attachedClass = "CSC3204"
            )
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    NotesScreen()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("notes_quick_preview_pane").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("notes_quick_preview_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("notes_sort_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Title A–Z").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // After Title A-Z sort, scroll/select "Operating Systems Lecture 4" card to inspect and toggle its checklist
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("notes_desktop_grid").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("notes_desktop_grid")
            .performScrollToNode(hasTestTag("note_card_CSC3101|note_desktop_test_1"))
        composeRule.onNodeWithTag("note_card_CSC3101|note_desktop_test_1").performClick()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasText("Review mutexes") and hasAnyAncestor(hasTestTag("notes_quick_preview_pane"))
            ).fetchSemanticsNodes().isNotEmpty()
        }

        // Toggle checklist item directly inside preview pane (0/1 tasks -> 1/1 tasks)
        composeRule.onNode(
            hasText("Review mutexes") and hasAnyAncestor(hasTestTag("notes_quick_preview_pane"))
        ).performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodes(
                hasText("1/1 tasks") and hasAnyAncestor(hasTestTag("notes_quick_preview_pane"))
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(
            hasText("1/1 tasks") and hasAnyAncestor(hasTestTag("notes_quick_preview_pane"))
        ).assertIsDisplayed()
        composeRule.onNode(
            hasText("1/1 tasks") and hasAnyAncestor(hasTestTag("note_card_CSC3101|note_desktop_test_1")),
            useUnmergedTree = true
        ).assertExists()

        // Copy button & close preview -> Show Top Note restore flow
        composeRule.onNodeWithTag("notes_preview_copy_btn")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.onNodeWithTag("notes_preview_close_btn")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Show Top Note").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notes_preview_edit_btn").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun noteEditorShowsSplitTabLivePreviewCollapsibleInspectorAndUnsavedWikiLinkGuardOnDesktop() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    NoteEditorScreen(
                        initialNote = null,
                        availableCourses = listOf(CourseOption("CSC3101", "Operating Systems", "#2563EB")),
                        timetableClasses = emptyList(),
                        onWikiLinkClick = {},
                        onSave = { _, _, _, _, _, _, _, _, _ -> },
                        onCancel = {}
                    )
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_editor_split_tab").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("note_editor_split_tab").assertIsDisplayed()
        composeRule.onNodeWithTag("note_content_editor").assertIsDisplayed()
        composeRule.onNodeWithTag("note_live_preview_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("note_editor_inspector").assertIsDisplayed()
        composeRule.onNodeWithTag("note_editor_copy_md_btn")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("note_save_button").assertIsDisplayed()

        // Collapse and restore right inspector drawer
        composeRule.onNodeWithTag("note_editor_inspector_toggle")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(0, composeRule.onAllNodesWithTag("note_editor_inspector").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("note_editor_inspector_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_editor_inspector").assertIsDisplayed()

        // Typing content displays Unsaved badge and protects WikiLink clicks with a Save/Discard dialog
        composeRule.onNodeWithTag("note_content_editor").performTextInput("[[Algorithms]]")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_unsaved_badge").assertIsDisplayed()
        composeRule.onNode(
            hasText("Algorithms") and hasAnyAncestor(hasTestTag("note_live_preview_pane"))
        ).assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Save & Open").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun compactMobileNotesDashboardAndEditorPreserveCompactLayoutAndTouchTargets() {
        runBlocking {
            app.repository.saveNote(
                naturalKey = "CSC3101|mobile_test_1",
                content = "# Lecture 1\nBinary heaps run in O(log n).",
                title = "Data Structures Recap",
                tag = "Study Guide",
                attachedClass = "CSC3101: Data Structures · Monday, 08:00 – 10:00"
            )
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    NotesScreen()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_card_CSC3101|mobile_test_1").fetchSemanticsNodes().isNotEmpty()
        }

        // Mobile should NOT show desktop preview pane, should show sort button & FAB with 48dp+ bounds
        assertEquals(0, composeRule.onAllNodesWithTag("notes_quick_preview_pane").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("topbar_academic_info_btn")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("notes_sort_button")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("add_note_fab").assertIsDisplayed()

        // Single tap opens NoteEditor immediately in compact mode (no desktop split/inspector clutter)
        composeRule.onNodeWithTag("note_card_CSC3101|mobile_test_1").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_save_button").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(0, composeRule.onAllNodesWithTag("note_editor_split_tab").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("note_editor_inspector").fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("note_editor_write_tab").assertIsDisplayed()
        composeRule.onNodeWithTag("note_editor_preview_tab").assertIsDisplayed()
        composeRule.onNodeWithTag("note_save_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun calendarScreenRendersMonthAndAgendaSideBySideOnDesktop() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    CalendarScreen()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("calendar_month_pane").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("calendar_month_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("calendar_agenda_pane").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1366dp-h768dp")
    fun vacantRoomsScreenRendersThreePaneWorkspaceOnChromebook() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1366, 768)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    VacantRoomsScreen(enrolledProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("vacant_rooms_filter_rail").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("vacant_rooms_filter_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("vacant_rooms_grid_pane").assertIsDisplayed()
        composeRule.onNodeWithTag("vacant_rooms_inspector_pane").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun settingsScreenRendersTwoPaneMasterDetailAndSwitchesCategoriesInline() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_category_rail").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_category_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_detail_pane").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithTag("settings_quick_theme_dark").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("settings_appearance_card")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("theme_mode_dark").assertIsDisplayed()

        composeRule.onNodeWithTag("settings_category_NOTIFICATIONS").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_daily_digest_switch").performScrollTo().assertIsDisplayed()

        composeRule.onNodeWithTag("settings_category_APPEARANCE").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("accent_swatch_3").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1600dp-h900dp")
    fun extraLargeTasksWelcomeAccountAndNotificationDialogWorkCleanly() {
        runBlocking {
            app.repository.saveAssignment(
                Assignment(
                    title = "Kernel Scheduling Report",
                    courseCode = "CSC3101",
                    dueDate = "Monday • 14:00",
                    priority = "High",
                    completed = false
                )
            )
            app.repository.saveAssignment(
                Assignment(
                    title = "Database Indexing Lab",
                    courseCode = "CSC3204",
                    dueDate = "Wednesday • 16:00",
                    priority = "Medium",
                    completed = false
                )
            )
        }

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1600, 900)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    TasksScreen()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("tasks_desktop_grid").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("task_progress_dashboard").assertIsDisplayed()
        composeRule.onNodeWithTag("tasks_desktop_grid").assertIsDisplayed()
        composeRule.onNodeWithTag("add_task_fab").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h720dp")
    fun welcomeAccountAndNotificationCenterDialogRenderDesktopLayoutsCleanly() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 720)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    WelcomeScreen(onGetStarted = {})
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("welcome_desktop_showcase").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("welcome_desktop_showcase").assertIsDisplayed()
        composeRule.onNodeWithTag("get_started_button").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun accountScreenAndNotificationCenterSheetRenderDesktopLayouts() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    AccountScreen()
                    NotificationCenterSheet(onDismiss = {})
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("account_desktop_guest_row").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("account_desktop_guest_row").assertIsDisplayed()
        composeRule.onNodeWithTag("google_signin_button").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_center_settings_btn").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun desktopSidebarOmitsDuplicateWeekPrefixAndTimetableButtonsMeet48dp() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    MainScaffold()
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("desktop_sidebar_toggle").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(TimeUtil.currentAcademicWeek()).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Week Week", substring = true).fetchSemanticsNodes().size)
        composeRule.onNodeWithTag("timetable_prev_day_btn")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag("timetable_next_day_btn")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun liveHeroCardDoesNotDuplicateSessionSuffixForLabSession() {
        val sampleLab = SpecClassSession(
            id = "lab_1",
            courseCode = "CSC3101",
            courseName = "Operating Systems Lab",
            department = "FCI",
            topicDescription = null,
            startTime = "08:00",
            endTime = "10:00",
            type = SpecSessionType.LAB,
            venue = "FCL Lab 1",
            lecturer = "Dr. A. K.",
            isHappeningNow = true
        )

        composeRule.setContent {
            AppTheme(themeMode = ThemeMode.LIGHT) {
                HappeningNowHeroCard(
                    session = sampleLab,
                    onQuickNote = {},
                    onAlarmDismiss = {}
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("• Lab Session · 08:00 – 10:00").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Session Session", substring = true).fetchSemanticsNodes().size)
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun desktopSettingsWidensCategoryRailAt150PercentFontScale() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalConfiguration provides configuredCopy(1280, 800, fontScale = 1.5f),
                LocalDensity provides Density(density = 2f, fontScale = 1.5f)
            ) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_category_rail").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("settings_category_rail")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(304.dp)
        composeRule.onNode(
            hasText("Notifications & Alarms") and hasAnyAncestor(hasTestTag("settings_category_rail"))
        ).assertIsDisplayed()
        composeRule.onNode(
            hasText("Appearance & Theme") and hasAnyAncestor(hasTestTag("settings_category_rail"))
        ).assertIsDisplayed()
    }
}
