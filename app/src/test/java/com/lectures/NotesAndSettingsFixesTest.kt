package com.lectures

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.database.AppDatabase
import com.lectures.core.sync.SyncRepository
import com.lectures.features.notes.MarkdownBlock
import com.lectures.features.notes.MarkdownUtils
import com.lectures.features.notes.NotesViewModel
import com.lectures.features.settings.SettingsViewModel
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.ui.AccentColorNames
import com.lectures.ui.AccentColors
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotesAndSettingsFixesTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var etagStore: ETagStore
    private lateinit var repository: TimetableRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        context = app.applicationContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        etagStore = ETagStore(context)
        repository = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun markdownUtils_extractsValidTagsOnly() {
        val sample = """
            ## Lecture Heading
            Link to https://portal.university.edu/course/#section1
            Issue #1 and hex #FFF should not count as tags.
            Study #midterm and #exam-prep tonight!
        """.trimIndent()

        val tags = MarkdownUtils.extractInlineTags(sample)
        assertEquals(listOf("#midterm", "#exam-prep"), tags)
    }

    @Test
    fun markdownUtils_parsesPlusBulletsNestedNumbersAndCallouts() {
        val md = """
            + Top plus bullet
            1. Step one
              2. Nested step two
            > [!NOTE] Objective
            > Core idea
        """.trimIndent()

        val blocks = MarkdownUtils.parseBlocks(md)
        assertTrue(blocks.any { it is MarkdownBlock.BulletItem && it.text == "Top plus bullet" })
        assertTrue(blocks.any { it is MarkdownBlock.NumberedItem && it.number == "2" && it.indentLevel == 1 })
        assertTrue(blocks.any { it is MarkdownBlock.Callout && it.title == "Objective" && it.content == "Core idea" })
    }

    @Test
    fun alarmScheduler_doesNotScheduleStandaloneNoteWithoutSchedule() {
        val scheduler = AlarmScheduler(context)
        val unscheduledNote = LectureNote(
            naturalKey = "CSC1101|Note|12345",
            content = "Plain standalone note",
            updatedAt = System.currentTimeMillis(),
            alarmMinutes = 15,
            title = "Standalone Note",
            attachedClass = null
        )
        assertFalse(
            "Standalone note without day/time must not fall back to a fake Monday 09:00 alarm",
            scheduler.scheduleClassAlarmFromNote(unscheduledNote, 15)
        )

        val attachedNote = LectureNote(
            naturalKey = "SWE2107|Note|99999",
            content = "Class summary",
            updatedAt = System.currentTimeMillis(),
            alarmMinutes = 30,
            title = "Systems Analysis",
            attachedClass = "SWE2107: Systems Analysis · Monday, 11:00 – 13:00"
        )
        assertTrue(
            "Note with attachedClass schedule string should parse day/time and schedule alarm",
            scheduler.scheduleClassAlarmFromNote(attachedNote, 30)
        )
    }

    private fun awaitCondition(
        testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
        timeoutMs: Long = 3_000L,
        predicate: () -> Boolean
    ) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            testScheduler.advanceUntilIdle()
            if (predicate()) return
            Thread.sleep(20)
        }
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun notesViewModel_filtersWikiLinkLookupChecklistAndUnschedulableAlarmClearing() = runTest(testDispatcher) {
        val scheduler = AlarmScheduler(context)

        runBlocking {
            repository.saveNote(
                naturalKey = "BCS1101|Algo|Mon|08:00|Lab 1",
                content = "- [ ] Review Big-O\n- [x] Setup IDE",
                alarmMinutes = 15,
                title = "Algorithms Intro",
                tag = "Lecture",
                attachedClass = "BCS1101: Algorithms · Monday, 08:00 – 10:00",
                attachmentName = null,
                attachmentPath = null,
                colourTag = "#2563EB",
                isPinned = true
            )
            repository.saveNote(
                naturalKey = "MAT1102|Note|222",
                content = "See [[Algorithms Intro]] for recurrence proof.",
                alarmMinutes = null,
                title = "Linear Algebra Notes",
                tag = "Revision",
                attachedClass = null,
                attachmentName = null,
                attachmentPath = null,
                colourTag = "#059669",
                isPinned = false
            )
        }

        val vm = NotesViewModel(repository, scheduler)
        awaitCondition(testScheduler) {
            vm.uiState.value.notes.any { it.naturalKey == "BCS1101|Algo|Mon|08:00|Lab 1" } &&
                vm.uiState.value.notes.any { it.naturalKey == "MAT1102|Note|222" }
        }

        vm.onFilterSelected("Pinned")
        assertEquals(1, vm.uiState.value.filteredNotes.size)
        assertEquals("Algorithms Intro", vm.uiState.value.filteredNotes.first().title)

        vm.onFilterSelected("Checklists")
        assertEquals(1, vm.uiState.value.filteredNotes.size)

        vm.onFilterSelected("Alerts")
        assertEquals(1, vm.uiState.value.filteredNotes.size)

        vm.onFilterSelected("All Notes")
        val linked = vm.findNoteByTitle("algorithms intro")
        assertNotNull(linked)
        assertEquals("BCS1101|Algo|Mon|08:00|Lab 1", linked?.naturalKey)

        // Toggle line 0 checklist
        vm.toggleChecklist("BCS1101|Algo|Mon|08:00|Lab 1", 0)
        awaitCondition(testScheduler) {
            vm.uiState.value.notes.firstOrNull { it.naturalKey == "BCS1101|Algo|Mon|08:00|Lab 1" }
                ?.content?.startsWith("- [x] Review Big-O") == true
        }
        val updated = repository.getAllNotes().first().first { it.naturalKey == "BCS1101|Algo|Mon|08:00|Lab 1" }
        assertTrue(updated.content.startsWith("- [x] Review Big-O"))

        // Saving a standalone note with no attached class clears phantom alarmMinutes
        vm.saveNote(
            naturalKey = null,
            courseCode = "ENG1101",
            title = "Essay Outline",
            content = "Draft outline",
            alarmMinutes = 15,
            tag = "Assignment",
            attachedClass = "",
            attachmentName = "",
            attachmentPath = null
        )
        awaitCondition(testScheduler) {
            vm.uiState.value.notes.any { it.title == "Essay Outline" }
        }
        val savedStandalone = repository.getAllNotes().first().first { it.title == "Essay Outline" }
        assertNull("Unschedulable standalone note should not store alarmMinutes", savedStandalone.alarmMinutes)
    }

    @Test
    fun settingsViewModel_preferencesAndThemeAccentsWorkCorrectly() = runTest(testDispatcher) {
        assertEquals(5, AccentColors.size)
        assertEquals(5, AccentColorNames.size)

        val vm = SettingsViewModel(
            repository = repository,
            dataLoader = DataLoader(context, db),
            alarmScheduler = TaskAlarmScheduler(context),
            ioDispatcher = testDispatcher,
            application = app
        )

        vm.setTaskReminderLeadHours(12)
        vm.setAlarmVibration(false)
        vm.setThemeMode(ThemeMode.SYSTEM)
        testScheduler.advanceUntilIdle()

        assertEquals(12, repository.taskReminderLeadPref.first())
        assertFalse(repository.alarmVibrationPref.first())
        assertEquals(ThemeMode.SYSTEM, vm.uiState.value.themeMode)

        // Quiet mode blocks rescheduleAllAlarms from arming alerts
        vm.setNotificationsEnabled(false)
        awaitCondition(testScheduler) {
            !vm.uiState.value.notificationsEnabled && vm.uiState.value.successMessage != null
        }
        vm.clearFeedbackMessages()
        vm.rescheduleAllAlarms()
        awaitCondition(testScheduler) {
            vm.uiState.value.successMessage?.contains("Quiet Mode", ignoreCase = true) == true
        }
        assertTrue(vm.uiState.value.successMessage?.contains("Quiet Mode", ignoreCase = true) == true)

        // Denying POST_NOTIFICATIONS makes sendTestNotification report an actionable error
        org.robolectric.Shadows.shadowOf(app).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        vm.sendTestNotification(context)
        assertNotNull(vm.uiState.value.errorMessage)
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    }

    @Test
    fun settingsViewModel_offlineSyncPreservesCachedEntriesAndResetClearsAttachments() = runTest(testDispatcher) {
        val syncRepo = SyncRepository(db, firestore = null)
        val repoWithSync = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore,
            syncRepository = syncRepo
        )

        runBlocking {
            db.timetableDao().upsertEntries(
                listOf(
                    TimetableEntry(
                        natural_key = "BCS2101|Data Structures|Monday|09:00|Lab 1",
                        program_group = "BCS II",
                        day = "Monday",
                        time_slot = "09:00 - 11:00",
                        start_time = "09:00",
                        end_time = "11:00",
                        course_code = "BCS2101",
                        course_title = "Data Structures",
                        session_type = "Lecture",
                        lecturer = "Dr. Mugisha",
                        room = "Lab 1"
                    )
                )
            )
        }

        val vm = SettingsViewModel(
            repository = repoWithSync,
            dataLoader = DataLoader(context, db),
            alarmScheduler = TaskAlarmScheduler(context),
            ioDispatcher = testDispatcher,
            application = app
        )

        vm.syncTimetable()
        awaitCondition(testScheduler) {
            vm.uiState.value.successMessage != null || vm.uiState.value.errorMessage != null
        }
        assertEquals("Existing DB entry must not be wiped when remote fetch returns null", 1, db.timetableDao().getEntryCount())
        assertTrue(vm.uiState.value.successMessage?.contains("Offline mode", ignoreCase = true) == true)

        // Create a dummy attachment file and verify resetAllNotes clears files/attachments/
        val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
        val dummyFile = File(attachmentsDir, "test_slide.pdf").apply { writeText("pdf") }
        assertTrue(dummyFile.exists())

        var resetDone = false
        vm.resetAllNotes { resetDone = true }
        awaitCondition(testScheduler) { resetDone }
        assertTrue(
            "Expected resetDone=true (isResetting=${vm.uiState.value.isResetting}, err=${vm.uiState.value.errorMessage}, msg=${vm.uiState.value.successMessage})",
            resetDone
        )
        assertFalse("All attachment files must be deleted on note reset", dummyFile.exists())
    }

    @Test
    fun settingsViewModel_dailyDigestTogglePersistsAndSyncs() = runTest(testDispatcher) {
        val vm = SettingsViewModel(
            repository = repository,
            dataLoader = DataLoader(context, db),
            alarmScheduler = TaskAlarmScheduler(context),
            ioDispatcher = testDispatcher,
            application = app
        )

        assertTrue(vm.uiState.value.dailyDigestEnabled)
        vm.setDailyDigestEnabled(false)
        testScheduler.advanceUntilIdle()

        assertFalse(repository.dailyDigestEnabledPref.first())
        assertFalse(vm.uiState.value.dailyDigestEnabled)

        vm.setDailyDigestEnabled(true)
        testScheduler.advanceUntilIdle()
        assertTrue(repository.dailyDigestEnabledPref.first())
        assertTrue(vm.uiState.value.dailyDigestEnabled)
    }
}
