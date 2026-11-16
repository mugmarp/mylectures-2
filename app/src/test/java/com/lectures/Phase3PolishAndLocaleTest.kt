package com.lectures

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.core.database.AppDatabase
import com.lectures.core.util.TimeUtil
import com.lectures.features.calendar.CalendarViewModel
import com.lectures.features.tasks.TaskMetricsCalculator
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.TimetableConflictDetector
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.domain.TimetableMatcher
import com.lectures.features.timetable.domain.TimetableMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase3PolishAndLocaleTest {

    private lateinit var originalLocale: Locale
    private val turkishLocale = Locale.Builder().setLanguage("tr").setRegion("TR").build()
    private val frenchLocale = Locale.FRANCE

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        Dispatchers.resetMain()
        db.close()
    }

    private fun withDefaultLocale(locale: Locale, block: () -> Unit) {
        val prev = Locale.getDefault()
        Locale.setDefault(locale)
        try {
            block()
        } finally {
            Locale.setDefault(prev)
        }
    }

    @Test
    fun conflictDetectorDaysMatchResistsTurkishAndFrenchLocaleCaseFolding() {
        listOf(turkishLocale, frenchLocale).forEach { locale ->
            withDefaultLocale(locale) {
                assertTrue(TimetableConflictDetector.daysMatch("Friday", "FRIDAY"))
                assertTrue(TimetableConflictDetector.daysMatch("Friday", "fri"))
                assertTrue(TimetableConflictDetector.daysMatch("Wednesday", "wed"))
                assertTrue(TimetableConflictDetector.daysMatch("Monday", "monday"))
                assertFalse(TimetableConflictDetector.daysMatch("Monday", "Tuesday"))
            }
        }
    }

    @Test
    fun timetableMatcherParsesAndFiltersCohortsSafelyUnderTurkishLocale() {
        // Turkish lowercase/uppercase of 'I'/'i' breaks Locale.getDefault() matching for BIT, MIE, CIV, BIOLOGY
        withDefaultLocale(turkishLocale) {
            val bit = TimetableMatcher.parseGroupParts("bit i")
            assertNotNull(bit)
            assertEquals("BIT", bit!!.code)
            assertEquals("I", bit.year)

            val mie = TimetableMatcher.parseGroupParts("mie ii")
            assertNotNull(mie)
            assertEquals("MIE", mie!!.code)
            assertEquals("II", mie.year)

            val civ = TimetableMatcher.parseGroupParts("civ iii")
            assertNotNull(civ)
            assertEquals("CVE", civ!!.code)
            assertEquals("III", civ.year)

            val entries = listOf(
                TimetableEntry(
                    natural_key = "BIT I|CSC1101|Friday|08:00|LT1",
                    program_group = "BIT I",
                    day = "Friday",
                    time_slot = "08:00 - 10:00",
                    start_time = "08:00",
                    end_time = "10:00",
                    course_code = "CSC1101",
                    course_title = "Programming I",
                    session_type = "Lecture",
                    lecturer = "Dr. Ada",
                    room = "LT1"
                ),
                TimetableEntry(
                    natural_key = "MIE II|MIE2102|Friday|10:00|ENG2",
                    program_group = "MIE II",
                    day = "Friday",
                    time_slot = "10:00 - 12:00",
                    start_time = "10:00",
                    end_time = "12:00",
                    course_code = "MIE2102",
                    course_title = "Thermodynamics",
                    session_type = "Lecture",
                    lecturer = "Prof. Watt",
                    room = "ENG2"
                )
            )

            val bitFriday = TimetableMatcher.filterTimetable(entries, "bit i", "fri")
            assertEquals(1, bitFriday.size)
            assertEquals("CSC1101", bitFriday.single().courseCode)

            val mieFriday = TimetableMatcher.filterTimetable(entries, "MIE II", "FRIDAY")
            assertEquals(1, mieFriday.size)
            assertEquals("MIE2102", mieFriday.single().courseCode)
        }
    }

    @Test
    fun timeUtilAndTimetableMetadataNormalizeSafelyUnderTurkishAndFrenchLocales() {
        listOf(turkishLocale, frenchLocale).forEach { locale ->
            withDefaultLocale(locale) {
                assertEquals(8 * 60, TimeUtil.toMinutes("08:00 am"))
                assertEquals(14 * 60 + 30, TimeUtil.toMinutes("02:30 pm"))
                assertEquals(14 * 60 + 15, TimeUtil.toMinutes("14:15"))

                val septCal = Calendar.getInstance().apply {
                    set(2026, Calendar.SEPTEMBER, 1, 10, 0, 0)
                }
                val weekLabel = TimeUtil.semesterWeekLabelFor(septCal)
                assertTrue(weekLabel.startsWith("Week "))

                val draftMeta = TimetableMetadata(
                    versionLabel = "Interim Release",
                    status = "Active Draft",
                    isFinal = false,
                    draftNumber = null
                )
                assertEquals("ACTIVE DRAFT", draftMeta.displayBadge)

                val labelledDraft = TimetableMetadata(
                    versionLabel = "Draft 3 Interim",
                    status = "In Review",
                    isFinal = false,
                    draftNumber = null
                )
                assertEquals("DRAFT 3 INTERIM", labelledDraft.displayBadge)
            }
        }
    }

    @Test
    fun taskDueDatesFormattedWithUsLocaleRoundTripThroughParserUnderNonUsLocales() {
        listOf(turkishLocale, frenchLocale).forEach { locale ->
            withDefaultLocale(locale) {
                val targetCal = Calendar.getInstance(Locale.US).apply {
                    set(2026, Calendar.NOVEMBER, 20, 14, 30, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val formattedDateOnly = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(targetCal.time)
                val parsedDateOnlyMillis = TaskDateTimeParser.calculateDueMillis(formattedDateOnly)
                assertNotNull("Expected '$formattedDateOnly' to parse under $locale", parsedDateOnlyMillis)
                val parsedDateCal = Calendar.getInstance().apply { timeInMillis = parsedDateOnlyMillis!! }
                assertEquals(2026, parsedDateCal.get(Calendar.YEAR))
                assertEquals(Calendar.NOVEMBER, parsedDateCal.get(Calendar.MONTH))
                assertEquals(20, parsedDateCal.get(Calendar.DAY_OF_MONTH))

                val formattedWithTime = "$formattedDateOnly 14:30"
                val parsedWithTimeMillis = TaskDateTimeParser.calculateDueMillis(formattedWithTime)
                assertNotNull("Expected '$formattedWithTime' to parse under $locale", parsedWithTimeMillis)
                val parsedTimeCal = Calendar.getInstance().apply { timeInMillis = parsedWithTimeMillis!! }
                assertEquals(14, parsedTimeCal.get(Calendar.HOUR_OF_DAY))
                assertEquals(30, parsedTimeCal.get(Calendar.MINUTE))
            }
        }
    }

    @Test
    fun taskMetricsCalculatorResolvesDayAndPriorityDeterministicallyUnderTurkishLocale() {
        withDefaultLocale(turkishLocale) {
            val fixedWednesday = Calendar.getInstance().apply {
                set(2026, Calendar.NOVEMBER, 18, 9, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val assignments = listOf(
                Assignment(
                    id = 1L,
                    title = "Algorithms Lab",
                    courseCode = "CSC2101",
                    dueDate = "Friday",
                    priority = "high",
                    completed = false
                ),
                Assignment(
                    id = 2L,
                    title = "Discrete Math Sheet",
                    courseCode = "MTH1102",
                    dueDate = "Monday",
                    priority = "Medium",
                    completed = true
                )
            )

            val metrics = TaskMetricsCalculator.calculateWeeklyMetrics(
                assignments = assignments,
                scopeThisWeekOnly = true,
                calendar = fixedWednesday
            )
            assertEquals(2, metrics.totalCount)
            assertEquals(1, metrics.completedCount)
            assertEquals(1, metrics.pendingCount)
            assertEquals(1, metrics.highPriorityPending)
            val fridayProgress = metrics.days.first { it.dayOfWeek == Calendar.FRIDAY }
            assertEquals(1, fridayProgress.pendingCount)
        }
    }

    @Test
    fun calendarViewModelEmitsCanonicalEnglishDayNamesRegardlessOfSystemLocale() {
        val repo = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = ETagStore(context)
        )
        val canonicalEnglishDays = setOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )

        listOf(frenchLocale, turkishLocale).forEach { locale ->
            withDefaultLocale(locale) {
                val vm = CalendarViewModel(repo)
                assertTrue(
                    "Initial selectedDayOfWeek must be canonical English under $locale",
                    vm.uiState.value.selectedDayOfWeek in canonicalEnglishDays
                )

                vm.navigateToDate(2026, Calendar.NOVEMBER, 16) // Monday, Nov 16, 2026
                assertEquals("Monday", vm.uiState.value.selectedDayOfWeek)
                assertEquals("Friday", vm.getDayOfWeekNameForDay(20))

                vm.selectDay(21) // Saturday, Nov 21, 2026
                assertEquals("Saturday", vm.uiState.value.selectedDayOfWeek)
            }
        }
    }

    @Test
    fun backupAndDataExtractionRulesXmlParseCleanlyAndExcludeEphemeralPrefs() {
        val backupExcludes = collectExcludePaths(context.resources.getXml(R.xml.backup_rules))
        val extractionExcludes = collectExcludePaths(context.resources.getXml(R.xml.data_extraction_rules))

        val requiredSharedPrefExcludes = listOf(
            "mustime_sync_prefs.xml",
            "mustime_notification_prefs.xml",
            "app_update_prefs.xml",
            "mustime_analytics.xml",
            "mustime_inapp_notifications.xml"
        )
        val requiredDbExcludes = listOf(
            "mustime_db-wal",
            "mustime_db-shm",
            "androidx.work.workdb"
        )

        requiredSharedPrefExcludes.forEach { pref ->
            assertTrue("backup_rules.xml missing exclude for $pref", pref in backupExcludes)
            assertTrue("data_extraction_rules.xml missing exclude for $pref", pref in extractionExcludes)
        }
        requiredDbExcludes.forEach { dbFile ->
            assertTrue("backup_rules.xml missing DB exclude for $dbFile", dbFile in backupExcludes)
            assertTrue("data_extraction_rules.xml missing DB exclude for $dbFile", dbFile in extractionExcludes)
        }
    }

    private fun collectExcludePaths(parser: XmlPullParser): Set<String> {
        val paths = mutableSetOf<String>()
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "exclude") {
                val path = parser.getAttributeValue(null, "path")
                if (!path.isNullOrBlank()) {
                    paths.add(path)
                }
            }
            eventType = parser.next()
        }
        return paths
    }
}
