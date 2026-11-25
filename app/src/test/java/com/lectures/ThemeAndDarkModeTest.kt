package com.lectures

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.database.AppDatabase
import com.lectures.features.settings.SettingsScreen
import com.lectures.features.settings.SettingsViewModel
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.ui.AccentColors
import com.lectures.ui.AccentDarkColors
import com.lectures.ui.AppTheme
import com.lectures.ui.LocalAppTheme
import com.lectures.ui.LocalExtendedColors
import com.lectures.ui.ThemeMode
import com.lectures.ui.accentDisplayColors
import com.lectures.ui.components.AcademicProfileSheet
import com.lectures.ui.contrastOnColor
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeAndDarkModeTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: TimetableApplication
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TimetableRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        context = app.applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = ETagStore(context)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun contrastRatio(a: Color, b: Color): Float {
        val l1 = a.luminance()
        val l2 = b.luminance()
        val lighter = max(l1, l2) + 0.05f
        val darker = min(l1, l2) + 0.05f
        return lighter / darker
    }

    @Test
    fun settingsViewModel_switchingDarkToSystemAndLightUpdatesDarkModeFlagAccurately() = runTest(testDispatcher) {
        val vm = SettingsViewModel(
            repository = repository,
            dataLoader = DataLoader(context, db),
            alarmScheduler = TaskAlarmScheduler(context),
            ioDispatcher = testDispatcher,
            application = app as Application
        )

        testScheduler.advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, vm.uiState.value.themeMode)
        assertFalse(vm.uiState.value.darkMode)

        vm.setThemeMode(ThemeMode.DARK)
        testScheduler.advanceUntilIdle()
        assertEquals("DARK", repository.themeModePref.first())
        assertEquals(ThemeMode.DARK, vm.uiState.value.themeMode)
        assertTrue(vm.uiState.value.darkMode)

        // Switching to SYSTEM must NOT leave darkMode latched to true on a light host configuration
        vm.setThemeMode(ThemeMode.SYSTEM)
        testScheduler.advanceUntilIdle()
        assertEquals("SYSTEM", repository.themeModePref.first())
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)
        assertFalse(vm.uiState.value.darkMode)

        vm.setThemeMode(ThemeMode.LIGHT)
        testScheduler.advanceUntilIdle()
        assertEquals("LIGHT", repository.themeModePref.first())
        assertFalse(vm.uiState.value.darkMode)
    }

    @Test
    fun allAccentPalettesMeetWcagAaContrastOnFilledSurfacesInLightAndDark() {
        val lightSwatches = accentDisplayColors(isDark = false)
        val darkSwatches = accentDisplayColors(isDark = true)
        assertEquals(AccentColors.size, lightSwatches.size)
        assertEquals(AccentDarkColors.size, darkSwatches.size)

        (lightSwatches + darkSwatches).forEach { bg ->
            val fg = contrastOnColor(bg)
            val ratio = contrastRatio(fg, bg)
            assertTrue(
                "Expected WCAG AA >= 4.5:1 contrast for bg=$bg with fg=$fg, got $ratio",
                ratio >= 4.5f
            )
        }

        // Dark-mode Amber (#F59E0B) and Ocean Teal (#38BDF8) must select dark ink rather than white
        assertEquals(Color(0xFF0B1120), contrastOnColor(AccentDarkColors[3]))
        assertEquals(Color(0xFF0B1120), contrastOnColor(AccentDarkColors[4]))
    }

    @Test
    fun appThemeProvidesObsidianDarkTokensAndAccessibleOnPrimary() {
        var darkBackground = Color.Unspecified
        var darkCard = Color.Unspecified
        var darkOnPrimary = Color.Unspecified
        var resolvedIsDark = false

        composeRule.setContent {
            AppTheme(themeMode = ThemeMode.DARK, accentIndex = 3) {
                resolvedIsDark = LocalAppTheme.current.isDark
                darkBackground = MaterialTheme.colorScheme.background
                darkCard = LocalExtendedColors.current.surfaceCard
                darkOnPrimary = MaterialTheme.colorScheme.onPrimary
            }
        }
        composeRule.waitForIdle()

        assertTrue(resolvedIsDark)
        assertEquals(Color(0xFF101216), darkBackground)
        assertEquals(Color(0xFF181B22), darkCard)
        assertEquals(Color(0xFF0B1120), darkOnPrimary)
    }

    @Test
    fun settingsScreen_appearanceOnlyAccessedViaAppearanceCardAndSwitchesModesAndAccents() {
        runBlocking {
            app.repository.setProgrammePref("BCS I")
            app.repository.setThemeMode("LIGHT")
            app.repository.setThemeAccent(0)
        }

        var activeMode by mutableStateOf(ThemeMode.LIGHT)
        var activeAccent by mutableIntStateOf(0)
        var observedIsDark = false

        composeRule.setContent {
            AppTheme(
                themeMode = activeMode,
                accentIndex = activeAccent,
                onThemeModeChange = { activeMode = it },
                onAccentChange = { activeAccent = it }
            ) {
                observedIsDark = LocalAppTheme.current.isDark
                SettingsScreen(currentProgramme = "BCS I")
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_appearance_card").fetchSemanticsNodes().isNotEmpty()
        }

        // Redundant inline theme switcher pills must NOT exist on the main Settings list
        assertEquals(0, composeRule.onAllNodesWithTag("settings_quick_theme_light").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("settings_quick_theme_system").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("settings_quick_theme_dark").fetchSemanticsNodes().size)

        // Open full Appearance & Theme sub-screen exclusively via the Appearance & Theme card
        composeRule.onNodeWithTag("settings_appearance_card")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("theme_mode_system").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("theme_mode_system")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.SYSTEM, activeMode)

        composeRule.onNodeWithTag("theme_mode_dark").performClick()
        composeRule.waitForIdle()
        assertEquals(ThemeMode.DARK, activeMode)
        assertTrue(observedIsDark)

        composeRule.onNodeWithTag("accent_swatch_3")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()
        assertEquals(3, activeAccent)

        composeRule.onNodeWithTag("appearance_live_preview_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("WCAG Contrast OK").assertIsDisplayed()
    }

    @Test
    fun academicProfileSheet_removesRedundantThemeSwitcherAndRoutesToSettings() {
        var settingsOpened = false

        composeRule.setContent {
            AppTheme(
                themeMode = ThemeMode.LIGHT,
                accentIndex = 0
            ) {
                AcademicProfileSheet(
                    programme = "BCS I",
                    enrolledProgramme = "BCS I",
                    onDismiss = {},
                    onOpenSettings = { settingsOpened = true }
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("academic_sheet_settings_btn").fetchSemanticsNodes().isNotEmpty()
        }

        // Redundant STUDY THEME card and pills must NOT appear in AcademicProfileSheet
        assertEquals(0, composeRule.onAllNodesWithText("STUDY THEME").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("quick_theme_light").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("quick_theme_system").fetchSemanticsNodes().size)
        assertEquals(0, composeRule.onAllNodesWithTag("quick_theme_dark").fetchSemanticsNodes().size)

        composeRule.onNodeWithTag("academic_sheet_settings_btn")
            .performScrollTo()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.waitForIdle()

        assertTrue(settingsOpened)
    }

    @Test
    @Config(qualifiers = "w320dp-h720dp")
    fun appearanceSettingsView_handlesNarrowScreenAndLargeFontScaleCleanly() {
        val narrowScaledConfig = Configuration(app.resources.configuration).apply {
            screenWidthDp = 320
            screenHeightDp = 720
            fontScale = 1.35f
        }
        var activeMode by mutableStateOf(ThemeMode.DARK)
        var activeAccent by mutableIntStateOf(0)

        composeRule.setContent {
            CompositionLocalProvider(LocalConfiguration provides narrowScaledConfig) {
                AppTheme(
                    themeMode = activeMode,
                    accentIndex = activeAccent,
                    onThemeModeChange = { activeMode = it },
                    onAccentChange = { activeAccent = it }
                ) {
                    SettingsScreen(currentProgramme = "BCS I")
                }
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_appearance_card").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_appearance_card")
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("accent_swatch_4").fetchSemanticsNodes().isNotEmpty()
        }

        (0..4).forEach { idx ->
            composeRule.onNodeWithTag("accent_swatch_$idx")
                .performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        }
        composeRule.onNodeWithTag("accent_swatch_4").performClick()
        composeRule.waitForIdle()
        assertEquals(4, activeAccent)

        composeRule.onNodeWithText("WCAG Contrast OK")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun splashResources_useStandardIntrinsicBoundsAndOpaqueThemeColors() {
        val splashIcon = ContextCompat.getDrawable(app, R.drawable.ic_splash_logo_safe)
        assertTrue("Splash logo drawable must load cleanly", splashIcon != null)
        val density = app.resources.displayMetrics.density
        val expectedPx = (288f * density).toInt()
        assertEquals(expectedPx, splashIcon!!.intrinsicWidth)
        assertEquals(expectedPx, splashIcon.intrinsicHeight)

        val lightLaunchColor = ContextCompat.getColor(app, R.color.launch_background)
        assertEquals(0xFFF8FAFC.toInt(), lightLaunchColor)

        val nightConfig = Configuration(app.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
        }
        val nightContext = app.createConfigurationContext(nightConfig)
        val nightLaunchColor = ContextCompat.getColor(nightContext, R.color.launch_background)
        assertEquals(0xFF101216.toInt(), nightLaunchColor)
    }

    @Test
    fun mainActivity_setsOpaqueWindowBackgroundMatchingThemeInsteadOfTransparentBlack() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        composeRule.waitForIdle()

        val bg = activity.window.decorView.background as? ColorDrawable
        assertTrue("Activity window background must be an opaque ColorDrawable, not transparent", bg != null)
        assertEquals(0xFFF8FAFC.toInt(), bg!!.color)
        controller.pause().stop().destroy()
    }
}
