package com.lectures

import android.Manifest
import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.lectures.features.notes.CourseOption
import com.lectures.features.notes.NoteEditorScreen
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.ui.WelcomeScreen
import com.lectures.ui.AppTheme
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class RoborazziVisualSnapshotTest {

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
    @Config(qualifiers = "w360dp-h800dp")
    fun snapshotPhoneLightWelcome() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(360, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    WelcomeScreen(onGetStarted = {})
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("get_started_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w674dp-h841dp")
    fun snapshotTabletDarkSettings() {
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(674, 841)) {
                AppTheme(themeMode = ThemeMode.DARK) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun snapshotDesktopLightNoteEditorSplit() {
        val sampleNote = LectureNote(
            naturalKey = "CSC3101|roborazzi_fixed_note",
            content = "# Mutexes & Semaphores\n- [ ] Compare binary vs counting semaphores\n- [x] Trace Peterson's algorithm\n\nSee [[Deadlocks]] before lab.",
            updatedAt = 1_700_000_000_000L,
            title = "Operating Systems — Synchronization",
            tag = "Study Guide",
            attachedClass = "CSC3101: Operating Systems · Monday, 08:00 – 10:00"
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides configuredCopy(1280, 800)) {
                AppTheme(themeMode = ThemeMode.LIGHT) {
                    NoteEditorScreen(
                        initialNote = sampleNote,
                        availableCourses = listOf(CourseOption("CSC3101", "Operating Systems", "#2563EB")),
                        timetableClasses = emptyList(),
                        onSave = { _, _, _, _, _, _, _, _, _ -> },
                        onCancel = {}
                    )
                }
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("note_editor_split_tab").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun snapshotDesktopDarkLargeFontScaleSettings() {
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
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage()
    }
}
