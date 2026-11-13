package com.lectures

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.analytics.AnalyticsManager
import com.lectures.core.auth.AuthRepository
import com.lectures.core.database.AppDatabase
import com.lectures.core.sync.SyncConflictResolver
import com.lectures.core.sync.SyncRepository
import com.lectures.core.sync.SyncState
import com.lectures.core.sync.UserSettingsSyncManager
import com.lectures.features.feedback.FeedbackCategory
import com.lectures.features.feedback.FeedbackManager
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase1SecurityAndSyncAuditTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var etagStore: ETagStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        context.getSharedPreferences("mustime_feedback", Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("mustime_analytics", Context.MODE_PRIVATE).edit().clear().commit()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        etagStore = ETagStore(context)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun noteConflictMergePreservesAttachmentAndAvoidsDuplication() {
        val local = LectureNote(
            naturalKey = "CSC3101|Mon|09:00",
            title = "AI Notes",
            content = "Search heuristics and A* proof",
            updatedAt = 1000L,
            attachmentName = "slides.pdf",
            attachmentPath = "attachments/slides.pdf",
            ownerId = "userA"
        )
        val remote = LectureNote(
            naturalKey = "CSC3101|Mon|09:00",
            title = null,
            content = "Constraint satisfaction problems",
            updatedAt = 2000L,
            attachmentName = null,
            attachmentPath = null,
            ownerId = "userA"
        )

        val firstPass = SyncConflictResolver.mergeNotes(listOf(local), listOf(remote))
        assertEquals(1, firstPass.conflictsForkedCount)
        val merged = firstPass.mergedList.single()
        assertEquals("AI Notes", merged.title)
        assertEquals("slides.pdf", merged.attachmentName)
        assertEquals("attachments/slides.pdf", merged.attachmentPath)
        assertTrue(merged.content.contains("Search heuristics"))
        assertTrue(merged.content.contains("Constraint satisfaction"))

        // Second pass against identical remote content should not re-append divider
        val secondPass = SyncConflictResolver.mergeNotes(listOf(merged), listOf(remote))
        assertEquals(1, secondPass.mergedList.size)
        val postSecond = secondPass.mergedList.single()
        assertEquals(merged.content, postSecond.content)
    }

    @Test
    fun noteTombstoneWinsWhenNewerThanRemoteActiveNote() {
        val localDeleted = LectureNote(
            naturalKey = "CSC3102|Tue|10:00",
            content = "Old text",
            updatedAt = 5000L,
            isDeleted = true,
            ownerId = "userA"
        )
        val remoteActive = LectureNote(
            naturalKey = "CSC3102|Tue|10:00",
            content = "Old text",
            updatedAt = 2000L,
            isDeleted = false,
            ownerId = "userA"
        )

        val result = SyncConflictResolver.mergeNotes(listOf(localDeleted), listOf(remoteActive))
        assertEquals(1, result.mergedList.size)
        assertTrue(result.mergedList.single().isDeleted)
        assertEquals(1, result.toPushToRemote.size)
    }

    @Test
    fun multiTenantOwnerStampingPromotionAndSignOutPurgeIsolatesAccounts() = runBlocking {
        var currentUid: String? = null
        val repo = TimetableRepository(
            dao = db.timetableDao(),
            eventDao = db.customEventDao(),
            assignmentDao = db.assignmentDao(),
            etagStore = etagStore,
            currentUserIdProvider = { currentUid }
        )

        // 1. Guest mode records (ownerId == null)
        repo.saveNote("KEY_GUEST", "Guest study note")
        repo.saveCustomEvent(
            CustomEvent(
                clientUuid = "evt-guest",
                title = "Guest Group Study",
                dayOfWeek = "Monday",
                startTime = "18:00",
                endTime = "19:00"
            )
        )
        repo.saveAssignment(
            Assignment(
                clientUuid = "task-guest",
                title = "Guest Quiz",
                courseCode = "CSC1101",
                dueDate = "2026-12-01"
            )
        )

        // 2. Sign in as userA -> sync promotes guest items & repository stamps new items with userA
        currentUid = "userA"
        val syncRepo = SyncRepository(db = db, firestore = null)
        val settingsManager = UserSettingsSyncManager(etagStore)
        val syncResult = syncRepo.performFullUserSync("userA", settingsManager)

        // When Firestore is unavailable, performFullUserSync must explicitly report ERROR
        assertEquals(SyncState.ERROR, syncResult.status)
        assertNotNull(syncResult.errorMessage)
        assertEquals(1, syncResult.notesCount)
        assertEquals(1, syncResult.eventsCount)
        assertEquals(1, syncResult.assignmentsCount)

        // Create another assignment while signed in as userA
        repo.saveAssignment(
            Assignment(
                clientUuid = "task-userA",
                title = "User A Lab",
                courseCode = "CSC2101",
                dueDate = "2026-12-02"
            )
        )

        // Pre-seed a separate account's item in DB to verify scoped purge does not touch userB
        db.timetableDao().upsertNote(
            LectureNote(
                naturalKey = "KEY_USER_B",
                content = "User B private note",
                updatedAt = 1000L,
                ownerId = "userB"
            )
        )

        // 3. Sign out userA -> purgeUserDataOnSignOut wipes ALL userA items (including promoted ones)
        syncRepo.purgeUserDataOnSignOut("userA")
        currentUid = null

        val remainingUserA = db.timetableDao().getAllNotesRawSyncForUser("userA")
        assertTrue(remainingUserA.none { it.ownerId == "userA" })
        assertTrue(db.customEventDao().getAllRawSyncForUser("userA").isEmpty())
        assertTrue(db.assignmentDao().getAllRawSyncForUser("userA").isEmpty())

        // User B record remains untouched
        val userBNote = db.timetableDao().getNoteDirect("KEY_USER_B")
        assertNotNull(userBNote)
        assertEquals("userB", userBNote?.ownerId)
    }

    @Test
    fun performFullUserSyncRejectsBlankUserIdWithErrorState() = runBlocking {
        val syncRepo = SyncRepository(db = db, firestore = null)
        val result = syncRepo.performFullUserSync("   ", UserSettingsSyncManager(etagStore))
        assertEquals(SyncState.ERROR, result.status)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun feedbackManagerRejectsBlankInputAndMaintainsThreadSafeQueue() = runBlocking {
        val manager = FeedbackManager(context, etagStore)
        val blankResult = manager.submitFeedback(FeedbackCategory.BUG_REPORT, "   \n  ")
        assertTrue(blankResult.isFailure)

        val validResult = manager.submitFeedback(FeedbackCategory.GENERAL, "Timetable search is snappy!")
        assertTrue(validResult.isSuccess)
        val recent = manager.recentSubmissions.first()
        assertEquals(1, recent.size)
        assertEquals("Timetable search is snappy!", recent[0].message)
        assertFalse(recent[0].instanceId.isBlank())
    }

    @Test
    fun analyticsManagerTracksFeaturesSafelyUnderConcurrency() = runBlocking {
        val analytics = AnalyticsManager(context)
        val installId1 = analytics.getInstallId()
        val installId2 = analytics.getInstallId()
        assertEquals(installId1, installId2)

        val jobs = (1..20).map {
            launch(Dispatchers.Default) {
                analytics.trackFeature("vacant_rooms_opened")
            }
        }
        jobs.forEach { it.join() }

        assertEquals(20, analytics.getFeatureCount("vacant_rooms_opened"))
    }

    @Test
    fun authRepositoryFailsCleanlyWithoutWebClientIdConfiguration() = runBlocking {
        val repo = AuthRepository.getInstance()
        val result = repo.signInWithGoogle(context)
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message.orEmpty()
        assertTrue(
            "Expected clean error when Auth/WebClient config is unavailable, got: $msg",
            msg.isNotBlank()
        )
    }

    @Test
    fun firestoreRulesMatchAllSyncedCollectionsAndDenyWildcards() {
        val rulesFile = listOf(
            File("firestore.rules"),
            File("../firestore.rules")
        ).firstOrNull { it.exists() }
        assertNotNull("firestore.rules must exist at workspace root", rulesFile)

        val rulesText = rulesFile!!.readText()
        listOf(
            "match /timetable/{entryId}",
            "match /user_telemetry/{installId}",
            "match /feedback/{feedbackId}",
            "match /users/{userId}",
            "match /notes/{noteId}",
            "match /events/{eventId}",
            "match /assignments/{assignmentId}",
            "match /settings/{docId}",
            "match /{document=**}"
        ).forEach { expectedBlock ->
            assertTrue("Missing Firestore rule block: $expectedBlock", rulesText.contains(expectedBlock))
        }
        assertFalse("Rules must never permit wildcard open access", rulesText.contains("allow read, write: if true"))
    }
}
