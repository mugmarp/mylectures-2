package com.lectures

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.update.AppUpdateInfo
import com.lectures.core.update.InAppUpdateManager
import com.lectures.core.util.TimeUtil
import com.lectures.features.notes.MarkdownUtils
import com.lectures.features.rooms.UniversityDirectory
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.domain.TimetableMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.io.File
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase4DomainAndUpdaterTest {

    @Test
    fun updaterRejectsNonHttpsUrlsAndAllowsVerifiedHttps() {
        assertTrue(InAppUpdateManager.isSecureHttpsUrl("https://github.com/mugmarp/mylectures/releases/latest"))
        assertFalse(InAppUpdateManager.isSecureHttpsUrl("http://github.com/mugmarp/mylectures/releases/latest"))
        assertFalse(InAppUpdateManager.isSecureHttpsUrl("file:///sdcard/update.apk"))
        assertFalse(InAppUpdateManager.isSecureHttpsUrl("https:///missing-host"))
        assertFalse(InAppUpdateManager.isSecureHttpsUrl("   "))
        assertFalse(InAppUpdateManager.isSecureHttpsUrl(null))

        val httpJson = """
            {
              "versionName": "9.9.9",
              "versionCode": 9090900,
              "downloadUrl": "http://example.com/release",
              "releaseNotes": "Insecure link"
            }
        """.trimIndent()
        assertNull(InAppUpdateManager.parseUpdateJson(httpJson))

        val httpsJson = """
            {
              "versionName": "9.9.9",
              "versionCode": 9090900,
              "downloadUrl": "https://github.com/mugmarp/mylectures/releases/tag/v9.9.9",
              "releaseNotes": "Valid HTTPS release"
            }
        """.trimIndent()
        val parsed = InAppUpdateManager.parseUpdateJson(httpsJson)
        assertNotNull(parsed)
        assertEquals("9.9.9", parsed?.versionName)
    }

    @Test
    fun updaterAntiDowngradeAndTagParsingHandleEdgeCases() {
        assertTrue(InAppUpdateManager.compareSemVer("2.1.0", "2.0.9") > 0)
        assertTrue(InAppUpdateManager.compareSemVer("2.0.0", "2.1.0") < 0)
        assertEquals(0, InAppUpdateManager.compareSemVer("v1.4.0-rc1", "1.4.0"))

        assertFalse(
            InAppUpdateManager.isNewerRelease(
                remoteVersionName = "1.0.0",
                remoteVersionCode = 9999999,
                localVersionName = "2.0.0",
                localVersionCode = 2000000
            )
        )
        assertTrue(
            InAppUpdateManager.isNewerRelease(
                remoteVersionName = "2.0.0",
                remoteVersionCode = 2000005,
                localVersionName = "2.0.0",
                localVersionCode = 2000000
            )
        )

        // Large suffix / overflow clamping check
        val code = InAppUpdateManager.parseVersionCodeFromTag("v12345.999.999-999999")
        assertTrue("Version code should stay positive and clamped", code > 0)
        assertEquals(999 * 1_000_000 + 99 * 10_000 + 99 * 100 + 99, code)
    }

    @Test
    fun openUpdatePageLaunchesActionViewIntentAndPurgesLegacyCache() {
        val context = ApplicationProvider.getApplicationContext<TimetableApplication>()
        val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
        val staleApk = File(updatesDir, "legacy.apk").apply { writeText("stale") }
        assertTrue(staleApk.exists())

        InAppUpdateManager.purgeStaleUpdateFiles(context)
        assertFalse(updatesDir.exists())

        val validUpdate = AppUpdateInfo(
            versionCode = 9000000,
            versionName = "9.0.0",
            releaseNotes = "Release",
            downloadUrl = "https://github.com/mugmarp/mylectures/releases/latest"
        )
        val okResult = InAppUpdateManager.openUpdatePage(context, validUpdate)
        assertTrue(okResult.isSuccess)

        val startedIntent = shadowOf(context).nextStartedActivity
        assertNotNull(startedIntent)
        assertEquals(Intent.ACTION_VIEW, startedIntent.action)
        assertEquals("https://github.com/mugmarp/mylectures/releases/latest", startedIntent.dataString)

        val badUpdate = validUpdate.copy(downloadUrl = "http://github.com/evil")
        val blockedResult = InAppUpdateManager.openUpdatePage(context, badUpdate)
        assertTrue(blockedResult.isFailure)
    }

    @Test
    fun timeUtilParses12HourFormatsAndAcademicRecessAccurately() {
        assertEquals(14 * 60 + 30, TimeUtil.toMinutes("02:30 PM"))
        assertEquals(0, TimeUtil.toMinutes("12:00 AM"))
        assertEquals(12 * 60 + 30, TimeUtil.toMinutes("12:30 PM"))
        assertEquals(8 * 60 + 15, TimeUtil.toMinutes("08:15"))
        assertEquals(8 * 60 + 15, TimeUtil.toMinutes("08:15 AM"))

        val augRecess = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 10, 10, 0, 0) }
        assertEquals("Academic Recess", TimeUtil.semesterWeekLabelFor(augRecess))
        assertEquals("Recess", TimeUtil.academicWeekFor(augRecess))

        val julyRecess = Calendar.getInstance().apply { set(2026, Calendar.JULY, 15, 10, 0, 0) }
        assertEquals("Academic Recess", TimeUtil.semesterWeekLabelFor(julyRecess))
        assertEquals("Recess", TimeUtil.academicWeekFor(julyRecess))

        val semStart = Calendar.getInstance().apply { set(2026, Calendar.AUGUST, 31, 10, 0, 0) }
        assertEquals("Week 2", TimeUtil.academicWeekFor(semStart))
        assertEquals("Week 2 • Sem 1", TimeUtil.semesterWeekLabelFor(semStart))
    }

    @Test
    fun roomDirectoryMatchesAbbreviatedDaysForCustomEventsAndEntries() {
        val roomCode = "Biochem Lab"
        val mondayEntry = TimetableEntry(
            natural_key = "MBR I|BCH1101|Monday|08:00",
            program_group = "MBR I",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "BCH1101",
            course_title = "Biochemistry",
            session_type = "Lab",
            lecturer = "Staff",
            room = roomCode
        )
        val customFriday = CustomEvent(
            id = 42L,
            title = "Study Club",
            dayOfWeek = "Fri",
            startTime = "14:00",
            endTime = "16:00",
            location = roomCode
        )

        val monResult = UniversityDirectory.calculateRoomVacancy(
            allEntries = listOf(mondayEntry),
            dayOfWeek = "Mon",
            queryTimeStr = "08:30",
            customEvents = listOf(customFriday)
        ).first { it.room.code == roomCode }
        assertTrue(monResult.isOccupied)
        assertEquals("BCH1101", monResult.currentSession?.course_code)

        val friResult = UniversityDirectory.calculateRoomVacancy(
            allEntries = listOf(mondayEntry),
            dayOfWeek = "Friday",
            queryTimeStr = "14:30",
            customEvents = listOf(customFriday)
        ).first { it.room.code == roomCode }
        assertTrue(friResult.isOccupied)
        assertEquals("Study Club", friResult.currentSession?.course_title)
    }

    @Test
    fun timetableMatcherRejectsBlankDayFilterWhileMatchingAbbreviations() {
        val entry = TimetableEntry(
            natural_key = "BCS I|CSC1101|Monday|09:00",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "09:00 - 11:00",
            start_time = "09:00",
            end_time = "11:00",
            course_code = "CSC1101",
            course_title = "Programming",
            session_type = "Lecture",
            lecturer = "Staff",
            room = "FCI 1"
        )

        assertEquals(1, TimetableMatcher.filterTimetable(listOf(entry), "BCS I", day = null).size)
        assertEquals(1, TimetableMatcher.filterTimetable(listOf(entry), "BCS I", day = "Mon").size)
        assertEquals(0, TimetableMatcher.filterTimetable(listOf(entry), "BCS I", day = "   ").size)
        assertEquals(0, TimetableMatcher.filterTimetable(listOf(entry), "BCS I", day = "").size)
    }

    @Test
    fun markdownSyntaxHighlightingPreservesExactOffsetsAcrossCrlfLines() {
        val rawText = "# Header One\r\n- [x] Finished task\r\n> [!NOTE] Reminder"
        val highlighted = MarkdownUtils.highlightMarkdownSyntax(
            rawText = rawText,
            isDark = false,
            primaryColor = Color(0xFF2563EB)
        )

        assertEquals(rawText.length, highlighted.text.length)
        assertEquals(rawText, highlighted.text)

        val headerSpan = highlighted.spanStyles.firstOrNull {
            rawText.substring(it.start, it.end) == "# Header One"
        }
        assertNotNull("Heading span should align strictly before \\r\\n", headerSpan)

        val checklistTextStart = rawText.indexOf("Finished task")
        val checklistTextEnd = checklistTextStart + "Finished task".length
        val strikeSpan = highlighted.spanStyles.firstOrNull {
            it.start == checklistTextStart && it.end == checklistTextEnd
        }
        assertNotNull("Checklist strikethrough span should align without CRLF drift", strikeSpan)

        val calloutStart = rawText.indexOf("> [!NOTE] Reminder")
        val calloutSpan = highlighted.spanStyles.firstOrNull {
            it.start == calloutStart && it.end == rawText.length
        }
        assertNotNull("Callout span should extend cleanly to end of third CRLF line", calloutSpan)
    }
}
