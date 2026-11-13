package com.lectures

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.database.AppDatabase
import com.lectures.core.database.Converters
import com.lectures.features.notes.NotesViewModel
import com.lectures.features.onboarding.OnboardingDataStore
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ETagStore
import com.lectures.features.timetable.data.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase2PersistenceAndMigrationTest {

    private lateinit var context: Context
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>().applicationContext
        context.getSharedPreferences("mustime_settings", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        context.deleteDatabase("phase2_mig_10_12.db")
        context.deleteDatabase("phase2_prepopulated_verify.db")
    }

    @Test
    fun migration10To12MatchesRoomV12SchemaAndPreservesExistingData() = runBlocking {
        val dbName = "phase2_mig_10_12.db"
        context.deleteDatabase(dbName)

        // Create a v10 database matching schemas/10.json exactly
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(10) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS `timetable` (`natural_key` TEXT NOT NULL, `program_group` TEXT NOT NULL, `day` TEXT NOT NULL, `time_slot` TEXT NOT NULL, `start_time` TEXT NOT NULL, `end_time` TEXT, `course_code` TEXT NOT NULL, `course_title` TEXT NOT NULL, `session_type` TEXT, `lecturer` TEXT, `room` TEXT, `shared_with` TEXT NOT NULL, `floor` INTEGER, PRIMARY KEY(`natural_key`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_program_group_start_time` ON `timetable` (`program_group`, `start_time`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_day_start_time` ON `timetable` (`day`, `start_time`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_timetable_course_code` ON `timetable` (`course_code`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `lecture_notes` (`naturalKey` TEXT NOT NULL, `content` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, `colourTag` TEXT, `alarmMinutes` INTEGER, `title` TEXT, `tag` TEXT, `attachedClass` TEXT, `attachmentName` TEXT, `attachmentPath` TEXT, `isPinned` INTEGER NOT NULL, PRIMARY KEY(`naturalKey`))")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_lecture_notes_updatedAt` ON `lecture_notes` (`updatedAt`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `custom_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `dayOfWeek` TEXT NOT NULL, `startTime` TEXT NOT NULL, `endTime` TEXT NOT NULL, `location` TEXT NOT NULL, `notes` TEXT NOT NULL, `repeatWeekly` INTEGER NOT NULL, `alarmMinutes` INTEGER, `category` TEXT NOT NULL, `colorTag` TEXT NOT NULL)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_custom_events_dayOfWeek_startTime` ON `custom_events` (`dayOfWeek`, `startTime`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `assignments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `courseCode` TEXT NOT NULL, `dueDate` TEXT NOT NULL, `reminderMinutes` INTEGER, `priority` TEXT NOT NULL, `notes` TEXT NOT NULL, `completed` INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_assignments_completed` ON `assignments` (`completed`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_assignments_dueDate` ON `assignments` (`dueDate`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_assignments_courseCode` ON `assignments` (`courseCode`)")

                    db.execSQL("CREATE TABLE IF NOT EXISTS `timetable_metadata` (`id` INTEGER NOT NULL, `versionLabel` TEXT NOT NULL, `status` TEXT NOT NULL, `isFinal` INTEGER NOT NULL, `draftNumber` INTEGER, `academicYear` TEXT NOT NULL, `semester` TEXT NOT NULL, `releaseNotes` TEXT, `lastUpdated` INTEGER NOT NULL, `source` TEXT NOT NULL, `totalSessions` INTEGER NOT NULL, PRIMARY KEY(`id`))")

                    db.execSQL("INSERT INTO lecture_notes (naturalKey, content, updatedAt, title, isPinned) VALUES ('CSC1101|Note|Mon', 'Legacy body', 1700000000000, 'Legacy Note', 1)")
                    db.execSQL("INSERT INTO custom_events (title, dayOfWeek, startTime, endTime, location, notes, repeatWeekly, category, colorTag) VALUES ('Study Group', 'Monday', '14:00', '16:00', 'Library', 'Bring notes', 1, 'Study', '#2563EB')")
                    db.execSQL("INSERT INTO assignments (title, courseCode, dueDate, priority, notes, completed) VALUES ('Lab Report', 'CSC1101', 'Friday', 'High', 'Submit PDF', 0)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        helper.writableDatabase.close()
        helper.close()

        // Open with real Room builder from v10 -> v12
        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12)
            .allowMainThreadQueries()
            .build()

        try {
            val notes = roomDb.timetableDao().getAllNotesSync()
            assertEquals(1, notes.size)
            assertEquals("Legacy Note", notes.first().title)
            assertFalse(notes.first().isDeleted)
            assertEquals(null, notes.first().ownerId)

            val events = roomDb.customEventDao().getAllRawSync()
            assertEquals(1, events.size)
            assertTrue(events.first().clientUuid.isNotBlank())
            assertTrue(events.first().updatedAt > 0L)
            assertFalse(events.first().isDeleted)
            assertEquals(null, events.first().ownerId)

            val assignments = roomDb.assignmentDao().getAllRawSync()
            assertEquals(1, assignments.size)
            assertTrue(assignments.first().clientUuid.isNotBlank())
            assertTrue(assignments.first().updatedAt > 0L)
            assertFalse(assignments.first().isDeleted)
            assertEquals(null, assignments.first().ownerId)
        } finally {
            roomDb.close()
        }
    }

    @Test
    fun prepopulatedAssetDatabaseOpensCleanlyWithV12SchemaValidation() = runBlocking {
        val dbName = "phase2_prepopulated_verify.db"
        context.deleteDatabase(dbName)

        val roomDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .createFromAsset(AppDatabase.PREPOPULATED_DB_NAME)
            .addMigrations(
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8,
                AppDatabase.MIGRATION_8_9,
                AppDatabase.MIGRATION_9_10,
                AppDatabase.MIGRATION_10_11,
                AppDatabase.MIGRATION_11_12
            )
            .allowMainThreadQueries()
            .build()

        try {
            val count = roomDb.timetableDao().getEntryCount()
            assertTrue("Expected prepopulated database to have >= 1000 rows, got $count", count >= 1000)
            val meta = roomDb.timetableDao().getTimetableMetadataSync()
            assertNotNull("Expected prepopulated metadata row", meta)
        } finally {
            roomDb.close()
        }
    }

    @Test
    fun convertersHandleNullBlankAndCommaSeparatedTags() {
        val converters = Converters()
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList("   "))
        assertEquals(listOf("BCS I", "BIT I", "BSE I"), converters.toStringList("BCS I, , BIT I , BSE I"))
        assertEquals("BCS I,BIT I", converters.fromStringList(listOf("BCS I", "BIT I")))
        assertEquals(null, converters.fromStringList(null))
    }

    @Test
    fun onboardingStateStaysSynchronizedBetweenDataStoreETagStoreAndFallback() = runBlocking {
        val etagStore = ETagStore(context)
        val onboardingDataStore = OnboardingDataStore(context)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = TimetableRepository(
                dao = db.timetableDao(),
                eventDao = db.customEventDao(),
                assignmentDao = db.assignmentDao(),
                etagStore = etagStore,
                onboardingDataStore = onboardingDataStore
            )

            // 1. Legacy SharedPreferences value must NOT be deleted and must resolve true everywhere
            context.getSharedPreferences("mustime_settings", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("onboarding_completed", true)
                .commit()

            assertTrue("DataStore flow should read legacy flag", onboardingDataStore.isOnboardingCompleted.first())
            assertTrue("ETagStore must still keep onboarding_completed=true", etagStore.isOnboardingCompleted())
            assertTrue("Repository check must agree", repo.checkIsOnboardingCompleted())

            // 2. Resetting onboarding resets BOTH DataStore and ETagStore
            onboardingDataStore.resetOnboarding()
            assertFalse("DataStore must emit false after reset", onboardingDataStore.isOnboardingCompleted.first())
            assertFalse("ETagStore must also be false after reset", etagStore.isOnboardingCompleted())

            // 3. Repository without DataStore falls back properly to ETagStore
            val repoNoDataStore = TimetableRepository(
                dao = db.timetableDao(),
                eventDao = db.customEventDao(),
                assignmentDao = db.assignmentDao(),
                etagStore = etagStore,
                onboardingDataStore = null
            )
            etagStore.setOnboardingCompleted(true)
            assertTrue(
                "Fallback when onboardingDataStore == null must return ETagStore state",
                repoNoDataStore.checkIsOnboardingCompleted()
            )
        } finally {
            db.close()
        }
    }

    @Test
    fun deletedSampleWelcomeNoteDoesNotResurrectAcrossViewModelRebuilds() = runTest(testDispatcher) {
        val isolatedPrefs = "phase2_sample_seed_prefs_${System.nanoTime()}"
        context.getSharedPreferences(isolatedPrefs, Context.MODE_PRIVATE).edit().clear().commit()
        val etagStore = ETagStore(context, isolatedPrefs)
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = TimetableRepository(
                dao = db.timetableDao(),
                eventDao = db.customEventDao(),
                assignmentDao = db.assignmentDao(),
                etagStore = etagStore
            )
            val dataLoader = DataLoader(context, db, isolatedPrefs)
            dataLoader.loadInitialDataIfNeeded()

            // Verify sample note exists on first launch
            val initialNotes = db.timetableDao().getAllNotesSync()
            assertTrue("First launch should seed sample welcome note", initialNotes.isNotEmpty())

            // User soft-deletes the welcome note
            initialNotes.forEach { repo.deleteNote(it.naturalKey) }
            assertEquals(0, db.timetableDao().getAllNotesSync().size)

            // Re-run DataLoader, ensureInitialNotesIfEmpty, and recreate NotesViewModel
            dataLoader.loadInitialDataIfNeeded()
            repo.ensureInitialNotesIfEmpty()
            val vm = NotesViewModel(repo)
            advanceUntilIdle()

            assertEquals(
                "Deleted sample welcome note must never resurrect",
                0,
                vm.uiState.value.notes.size
            )

            // Also verify direct ensureInitialNotesIfEmpty after deleteAllNotes stays empty
            repo.deleteAllNotes()
            repo.ensureInitialNotesIfEmpty()
            assertEquals(0, db.timetableDao().getAllNotesSync().size)
        } finally {
            db.close()
        }
    }
}
