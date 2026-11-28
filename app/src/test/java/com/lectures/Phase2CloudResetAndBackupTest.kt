package com.lectures

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.database.AppDatabase
import com.lectures.core.sync.SyncConflictResolver
import com.lectures.core.sync.SyncRepository
import com.lectures.features.account.AccountDeleteCloudDataDialog
import com.lectures.features.settings.SettingsBackupExporter
import com.lectures.features.settings.SettingsViewModel
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.ResetSyncStatus
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.ui.AppTheme
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase2CloudResetAndBackupTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var etagStore: ETagStore

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        context = app.applicationContext
        context.getSharedPreferences("phase2_cloud_backup_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        etagStore = ETagStore(context, "phase2_cloud_backup_prefs")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    private fun awaitCondition(timeoutMs: Long = 3_000L, predicate: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!predicate() && System.currentTimeMillis() < deadline) {
            testDispatcher.scheduler.advanceUntilIdle()
            if (predicate()) return
            Thread.sleep(20)
        }
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun dangerZoneResetsSoftDeleteAndPreventCloudZombieResurrection() = runBlocking {
        var currentUid: String? = null
        val syncRepo = SyncRepository(db = db, firestore = null)
        val guestRepo = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore,
            syncRepository = syncRepo,
            currentUserIdProvider = { currentUid }
        )

        // Guest mode resets return LOCAL_ONLY
        guestRepo.saveNote("KEY_G1", "Guest note", title = "Guest Title")
        assertEquals(ResetSyncStatus.LOCAL_ONLY, guestRepo.resetAllNotesCloudAware())
        assertTrue(db.timetableDao().getAllNotesSync().isEmpty())

        // Signed-in mode with unavailable/offline Firestore soft-deletes with tombstones & promotes owner
        currentUid = "userA"
        guestRepo.saveNote("CSC1101|Mon|09:00", "Algorithms lecture note", title = "Algorithms")
        guestRepo.saveAssignment(
            Assignment(
                clientUuid = "task-uuid-1",
                title = "Lab 1",
                courseCode = "CSC1101",
                dueDate = "2026-12-01"
            )
        )
        guestRepo.saveCustomEvent(
            CustomEvent(
                clientUuid = "evt-uuid-1",
                title = "Coding Club",
                dayOfWeek = "Tuesday",
                startTime = "17:00",
                endTime = "18:30"
            )
        )

        val preResetTs = System.currentTimeMillis() - 10_000L
        assertEquals(ResetSyncStatus.QUEUED_OFFLINE, guestRepo.resetAllNotesCloudAware())
        assertEquals(ResetSyncStatus.QUEUED_OFFLINE, guestRepo.resetAllAssignmentsCloudAware())
        assertEquals(ResetSyncStatus.QUEUED_OFFLINE, guestRepo.resetAllCustomEventsCloudAware())

        // Active views must be completely cleared
        assertTrue(db.timetableDao().getAllNotesSync().isEmpty())
        assertTrue(db.assignmentDao().getAllActiveSync().isEmpty())
        assertTrue(db.customEventDao().getAllActiveSync().isEmpty())

        // Tombstones remain locally and prevent stale remote copies from resurrecting on merge
        val localNoteTombstones = db.timetableDao().getAllNotesRawSyncForUser("userA")
        val noteTombstone = localNoteTombstones.first { it.naturalKey == "CSC1101|Mon|09:00" }
        assertTrue(noteTombstone.isDeleted)

        val remoteStaleNote = LectureNote(
            naturalKey = "CSC1101|Mon|09:00",
            title = "Algorithms",
            content = "Algorithms lecture note",
            updatedAt = preResetTs,
            isDeleted = false,
            ownerId = "userA"
        )
        val mergedNotes = SyncConflictResolver.mergeNotes(localNoteTombstones, listOf(remoteStaleNote))
        val resolvedNote = mergedNotes.mergedList.first { it.naturalKey == "CSC1101|Mon|09:00" }
        assertTrue("Note tombstone must block resurrection from stale cloud copy", resolvedNote.isDeleted)
        assertTrue(mergedNotes.toPushToRemote.any { it.naturalKey == "CSC1101|Mon|09:00" && it.isDeleted })

        val localTaskTombstones = db.assignmentDao().getAllRawSyncForUser("userA")
        val remoteStaleTask = Assignment(
            clientUuid = "task-uuid-1",
            title = "Lab 1",
            courseCode = "CSC1101",
            dueDate = "2026-12-01",
            updatedAt = preResetTs,
            isDeleted = false,
            ownerId = "userA"
        )
        val mergedTasks = SyncConflictResolver.mergeAssignments(localTaskTombstones, listOf(remoteStaleTask))
        assertTrue(mergedTasks.mergedList.single().isDeleted)
        assertEquals(1, mergedTasks.toPushToRemote.size)

        val localEventTombstones = db.customEventDao().getAllRawSyncForUser("userA")
        val remoteStaleEvent = CustomEvent(
            clientUuid = "evt-uuid-1",
            title = "Coding Club",
            dayOfWeek = "Tuesday",
            startTime = "17:00",
            endTime = "18:30",
            updatedAt = preResetTs,
            isDeleted = false,
            ownerId = "userA"
        )
        val mergedEvents = SyncConflictResolver.mergeEvents(localEventTombstones, listOf(remoteStaleEvent))
        assertTrue(mergedEvents.mergedList.single().isDeleted)
        assertEquals(1, mergedEvents.toPushToRemote.size)
    }

    @Test
    fun demoteUserToGuestOnCloudDeleteKeepsActiveRecordsAndPurgesTombstones() = runBlocking {
        val syncRepo = SyncRepository(db = db, firestore = null)

        db.timetableDao().upsertNote(
            LectureNote(
                naturalKey = "ACTIVE_A",
                title = "Keep me",
                content = "Offline copy",
                updatedAt = 2000L,
                isDeleted = false,
                ownerId = "userA"
            )
        )
        db.timetableDao().upsertNote(
            LectureNote(
                naturalKey = "DELETED_A",
                title = "Old deleted",
                content = "Gone",
                updatedAt = 2000L,
                isDeleted = true,
                ownerId = "userA"
            )
        )
        db.timetableDao().upsertNote(
            LectureNote(
                naturalKey = "ACTIVE_B",
                title = "User B note",
                content = "Isolated",
                updatedAt = 2000L,
                isDeleted = false,
                ownerId = "userB"
            )
        )
        db.assignmentDao().upsert(
            Assignment(
                clientUuid = "task-a",
                title = "Assignment A",
                courseCode = "BCS2101",
                dueDate = "Monday",
                ownerId = "userA"
            )
        )
        db.customEventDao().upsert(
            CustomEvent(
                clientUuid = "event-a",
                title = "Gym",
                dayOfWeek = "Friday",
                startTime = "18:00",
                endTime = "19:00",
                ownerId = "userA"
            )
        )

        syncRepo.demoteUserToGuestOnCloudDelete("userA")

        val activeNote = db.timetableDao().getNoteDirect("ACTIVE_A")
        assertNotNull(activeNote)
        assertNull("Demoted note should revert to Guest (ownerId == null)", activeNote?.ownerId)
        assertNull("Deleted user tombstones should be purged when cloud is deleted", db.timetableDao().getNoteRawDirect("DELETED_A"))

        val userBNote = db.timetableDao().getNoteDirect("ACTIVE_B")
        assertEquals("userB", userBNote?.ownerId)

        val demotedTask = db.assignmentDao().getByUuid("task-a")
        assertNull(demotedTask?.ownerId)
        val demotedEvent = db.customEventDao().getByUuid("event-a")
        assertNull(demotedEvent?.ownerId)
    }

    @Test
    fun backupExporterRoundTripAndLegacyImportRestoreWithoutDuplication() = runTest(testDispatcher) {
        val repo = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore
        )
        val notes = listOf(
            LectureNote(
                naturalKey = "BCS2101|Mon|09:00",
                title = "Graphs",
                content = "BFS and DFS traversal",
                updatedAt = 1_700_000_000_000L,
                colourTag = "#059669",
                alarmMinutes = 15,
                tag = "Lecture",
                attachedClass = "BCS2101 · Mon 09:00",
                isPinned = true
            )
        )
        val events = listOf(
            CustomEvent(
                id = 12L,
                clientUuid = "custom-uuid-123",
                title = "Peer Study",
                dayOfWeek = "Wednesday",
                startTime = "14:00",
                endTime = "16:00",
                location = "Library",
                notes = "Past papers",
                repeatWeekly = false,
                alarmMinutes = 20,
                category = "Study",
                colorTag = "#7C3AED",
                updatedAt = 1_700_000_001_000L
            )
        )
        val tasks = listOf(
            Assignment(
                id = 7L,
                clientUuid = "task-uuid-456",
                title = "Compiler Essay",
                courseCode = "CSC3102",
                dueDate = "Friday",
                reminderMinutes = 120,
                priority = "High",
                notes = "3 pages",
                completed = true,
                updatedAt = 1_700_000_002_000L
            )
        )

        val json = SettingsBackupExporter.buildBackupJson(
            programme = "BCS II",
            notes = notes,
            events = events,
            tasks = tasks,
            exportedAtMillis = 1_700_000_005_000L
        )
        val parsed = SettingsBackupExporter.parseBackupJson(json).getOrThrow()
        assertEquals("BCS II", parsed.programme)
        assertEquals(1, parsed.notes.size)
        assertEquals("#059669", parsed.notes.single().colourTag)
        assertEquals(15, parsed.notes.single().alarmMinutes)
        assertEquals(1, parsed.events.size)
        assertEquals("custom-uuid-123", parsed.events.single().clientUuid)
        assertFalse(parsed.events.single().repeatWeekly)
        assertEquals(20, parsed.events.single().alarmMinutes)
        assertEquals(1, parsed.assignments.size)
        assertEquals("task-uuid-456", parsed.assignments.single().clientUuid)
        assertEquals(120, parsed.assignments.single().reminderMinutes)
        assertTrue(parsed.assignments.single().completed)

        val vm = SettingsViewModel(
            repository = repo,
            dataLoader = DataLoader(context, db, "phase2_cloud_backup_prefs"),
            alarmScheduler = TaskAlarmScheduler(context),
            classAlarmScheduler = AlarmScheduler(context),
            ioDispatcher = testDispatcher,
            application = app
        )

        // First restore + duplicate second restore must succeed without UNIQUE constraint violations
        val firstRestore = vm.importUserDataFromJson(json)
        assertTrue(firstRestore.isSuccess)
        assertTrue(vm.uiState.value.successMessage?.contains("Backup restored (1 notes, 1 events, 1 tasks)") == true)

        val secondRestore = vm.importUserDataFromJson(json)
        assertTrue(secondRestore.isSuccess)
        assertEquals(1, db.timetableDao().getAllNotesSync().size)
        assertEquals(1, db.customEventDao().getAllActiveSync().size)
        assertEquals(1, db.assignmentDao().getAllActiveSync().size)

        // Legacy backup JSON (without clientUuid / colourTag) imports with deterministic UUID deduplication
        val legacyJson = """
            {
              "appVersion": "2.0",
              "exportedAt": 1700000000000,
              "programme": "BCS I",
              "notes": [
                { "naturalKey": "LEGACY_NOTE", "title": "Legacy Note", "content": "Restored text", "isPinned": false }
              ],
              "customEvents": [
                { "id": 1, "title": "Legacy Club", "category": "Club", "dayOfWeek": "Friday", "startTime": "16:00", "endTime": "17:00", "location": "Hall" }
              ],
              "assignments": [
                { "id": 2, "title": "Legacy Lab", "courseCode": "CSC1100", "dueDate": "Tomorrow", "priority": "Low", "completed": false }
              ]
            }
        """.trimIndent()
        assertTrue(vm.importUserDataFromJson(legacyJson).isSuccess)
        assertTrue(vm.importUserDataFromJson(legacyJson).isSuccess)
        assertEquals(2, db.timetableDao().getAllNotesSync().size)
        assertEquals(2, db.customEventDao().getAllActiveSync().size)
        assertEquals(2, db.assignmentDao().getAllActiveSync().size)

        // Corrupt JSON fails cleanly with descriptive message
        val badResult = vm.importUserDataFromJson("{ \"notALecturesBackup\": true }")
        assertTrue(badResult.isFailure)
        assertNotNull(vm.uiState.value.errorMessage)
    }

    @Test
    fun settingsViewModelResetAllCustomEventsClearsActivities() = runTest(testDispatcher) {
        val repo = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore
        )
        runBlocking {
            repo.saveCustomEvent(
                CustomEvent(
                    clientUuid = "evt-reset-1",
                    title = "Hackathon Prep",
                    dayOfWeek = "Thursday",
                    startTime = "18:00",
                    endTime = "20:00",
                    alarmMinutes = 15
                )
            )
        }
        assertEquals(1, db.customEventDao().getAllActiveSync().size)

        val vm = SettingsViewModel(
            repository = repo,
            dataLoader = DataLoader(context, db, "phase2_cloud_backup_prefs"),
            alarmScheduler = TaskAlarmScheduler(context),
            classAlarmScheduler = AlarmScheduler(context),
            ioDispatcher = testDispatcher,
            application = app
        )

        var done = false
        vm.resetAllCustomEvents { done = true }
        awaitCondition { done }

        assertTrue(done)
        assertEquals(0, db.customEventDao().getAllActiveSync().size)
        assertTrue(vm.uiState.value.successMessage?.contains("personal activities", ignoreCase = true) == true)
    }

    @Test
    fun accountDeleteCloudDataDialogTogglesLocalWipeAndInvokesConfirm() {
        var alsoClearLocal = false
        var confirmed = false

        composeRule.setContent {
            var checkedState by remember { mutableStateOf(false) }
            AppTheme(themeMode = ThemeMode.LIGHT) {
                AccountDeleteCloudDataDialog(
                    preferDesktopDialogs = false,
                    isDeleting = false,
                    alsoClearLocal = checkedState,
                    onToggleClearLocal = {
                        checkedState = it
                        alsoClearLocal = it
                    },
                    onConfirmDelete = { confirmed = true },
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Delete Cloud Backup Data?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete Cloud Backup").assertIsDisplayed()

        composeRule.onNodeWithTag("delete_cloud_clear_local_row").performClick()
        composeRule.waitForIdle()
        assertTrue(alsoClearLocal)
        composeRule.onNodeWithText("Delete Cloud & Local").assertIsDisplayed()

        composeRule.onNodeWithTag("dialog_delete_cloud_confirm_btn").performClick()
        composeRule.waitForIdle()
        assertTrue(confirmed)
    }
}
