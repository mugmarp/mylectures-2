package com.lectures

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import com.lectures.core.notification.InAppNotificationStore
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.notification.NotificationItemType
import com.lectures.core.sync.TimetableSyncWorker
import com.lectures.core.sync.UserSettingsSyncManager
import com.lectures.core.sync.UserSettingsSyncPayload
import com.lectures.features.timetable.domain.TimetableVersionResolver
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.io.ByteArrayInputStream
import java.net.InetAddress
import java.net.ServerSocket
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase2PerformanceAndSyncTest {

    private lateinit var app: TimetableApplication
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)

        runBlocking {
            app.etagStore.setNotificationsEnabled(true)
            app.etagStore.setProgrammePref("BCS I")
        }
        shadowAlarmManager.scheduledAlarms.clear()
        notificationManager.cancelAll()
        app.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        app.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE).edit().clear().commit()
        shadowAlarmManager.scheduledAlarms.clear()
        notificationManager.cancelAll()
    }

    @Test
    fun timetableSyncWorkerVersionCheckIgnoresSyntheticTimestampSpam() {
        val local = TimetableVersionResolver.resolve(
            rawVersion = "Final Timetable",
            isExplicitFinal = true,
            academicYear = "2026/2027",
            semester = "Semester 1",
            totalEntries = 1304,
            lastUpdatedMillis = 1_700_000_000_000L
        )

        // Same release JSON without server-provided last_updated must NOT trigger an update
        val sameRemoteJson = """
            {
              "version": "Final Timetable",
              "is_final": true,
              "academic_year": "2026/2027",
              "semester": "Semester 1",
              "total_sessions": 1304
            }
        """.trimIndent()
        val parsedSame = TimetableSyncWorker.parseMetadataJson(sameRemoteJson)
        assertNotNull(parsedSame)
        assertEquals(0L, parsedSame!!.lastUpdated)
        assertFalse(
            "Identical remote metadata without timestamp must not trigger repeat update notifications",
            TimetableSyncWorker.isNewerVersion(local, parsedSame)
        )

        // Newer release label or total_sessions or server timestamp DOES trigger update
        val newerDraftRemote = TimetableVersionResolver.resolve(
            rawVersion = "Draft 4",
            isExplicitFinal = false,
            totalEntries = 1310,
            lastUpdatedMillis = 0L
        )
        assertTrue(TimetableSyncWorker.isNewerVersion(local, newerDraftRemote))

        val sameWithNewerServerTimestamp = TimetableSyncWorker.parseMetadataJson(
            """
            {
              "version": "Final Timetable",
              "is_final": true,
              "academic_year": "2026/2027",
              "semester": "Semester 1",
              "total_sessions": 1304,
              "last_updated": 1800000000000
            }
            """.trimIndent()
        )!!
        assertTrue(TimetableSyncWorker.isNewerVersion(local, sameWithNewerServerTimestamp))
    }

    @Test
    fun timetableSyncWorkerDoesNotPostUpdateNotificationWhenEntriesUnavailableAndUpdatesAllWhenPresent() = runBlocking {
        val initialMeta = TimetableVersionResolver.resolve(
            rawVersion = "Draft 2",
            isExplicitFinal = false,
            totalEntries = 5,
            lastUpdatedMillis = 1_700_000_000_000L
        )
        app.database.timetableDao().upsertTimetableMetadata(initialMeta)

        val serveEntries = java.util.concurrent.atomic.AtomicBoolean(false)
        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val running = java.util.concurrent.atomic.AtomicBoolean(true)
        val serverThread = thread(isDaemon = true) {
            while (running.get()) {
                try {
                    val socket = server.accept()
                    socket.use { s ->
                        val reader = s.getInputStream().bufferedReader()
                        val requestLine = reader.readLine() ?: return@use
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                        }
                        val out = s.getOutputStream()
                        when {
                            requestLine.contains("timetable_metadata.json") -> {
                                val body = """
                                    {
                                      "version_label": "Draft 5",
                                      "is_final": false,
                                      "academic_year": "2026/2027",
                                      "semester": "Semester 1",
                                      "release_notes": "Updated LT venues",
                                      "total_sessions": 1
                                    }
                                """.trimIndent()
                                val bytes = body.toByteArray()
                                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n\r\n".toByteArray())
                                out.write(bytes)
                            }
                            requestLine.contains("timetable_export.json") && serveEntries.get() -> {
                                val body = """
                                    [
                                      {
                                        "natural_key": "BCS I|CSC4999|Monday|11:00|Innovation Lab",
                                        "program_group": "BCS I",
                                        "day": "Monday",
                                        "time_slot": "11:00 - 13:00",
                                        "start_time": "11:00",
                                        "end_time": "13:00",
                                        "course_code": "CSC4999",
                                        "course_title": "Distributed Cloud Systems",
                                        "session_type": "Lecture",
                                        "lecturer": "Dr. Grace",
                                        "room": "Innovation Lab"
                                      }
                                    ]
                                """.trimIndent()
                                val bytes = body.toByteArray()
                                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n\r\n".toByteArray())
                                out.write(bytes)
                            }
                            else -> {
                                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                            }
                        }
                        out.flush()
                    }
                } catch (_: Exception) {
                }
            }
        }

        try {
            app.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE).edit()
                .putString(TimetableSyncWorker.PREF_REMOTE_METADATA_URL, "http://127.0.0.1:$port/timetable_metadata.json")
                .commit()

            // Case 1: Metadata says Draft 5, but entries endpoint returns 404 -> MUST NOT lie or bump metadata
            serveEntries.set(false)
            val res1 = TimetableSyncWorker.performSync(app, checkAppUpdate = false)
            assertEquals(ListenableWorker.Result.success(), res1)
            assertEquals(
                "Metadata must not advance past un-synced entries",
                "Draft 2",
                app.database.timetableDao().getTimetableMetadataSync()?.versionLabel
            )
            assertTrue(
                "Must NOT post false Timetable Updated notification when entries failed to sync",
                shadowNotificationManager.allNotifications.isEmpty()
            )

            // Case 2: Entries endpoint succeeds -> replaces Room entries, bumps metadata, re-arms alarms, and notifies
            serveEntries.set(true)
            shadowAlarmManager.scheduledAlarms.clear()
            val res2 = TimetableSyncWorker.performSync(app, checkAppUpdate = false)
            assertEquals(ListenableWorker.Result.success(), res2)

            val entries = app.database.timetableDao().getAllEntriesSync()
            assertEquals(1, entries.size)
            assertEquals("CSC4999", entries.single().courseCode)

            val updatedMeta = app.database.timetableDao().getTimetableMetadataSync()
            assertNotNull(updatedMeta)
            assertEquals("Draft 5", updatedMeta!!.versionLabel)
            assertEquals(1, updatedMeta.totalSessions)
            assertEquals(1, shadowNotificationManager.allNotifications.size)
            assertTrue(
                "Alarms must be re-armed for synced BCS I schedule",
                shadowAlarmManager.scheduledAlarms.isNotEmpty()
            )
        } finally {
            running.set(false)
            runCatching { server.close() }
            serverThread.join(500)
        }
    }

    @Test
    fun inAppNotificationStoreGetInstanceSharesSingletonAndChannelsStayValid() {
        val storeA = InAppNotificationStore.getInstance(app)
        val storeB = InAppNotificationStore.getInstance(app.createAttributionContext("test"))
        assertSame(storeA, storeB)
        storeA.clearAll()

        NotificationHelper.showTimetableUpdatedNotification(app, "Draft 6", "Room updates")
        assertEquals(1, storeB.notifications.value.size)
        assertEquals(NotificationItemType.SYSTEM, storeB.notifications.value.single().type)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            assertNotNull(notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_CLASS_ALARMS))
            assertNotNull(notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_TIMETABLE_UPDATES))
        }
    }

    @Test
    fun dataLoaderParseEntriesFromStreamParsesValidEntriesAndOptionalFields() {
        val json = """
            [
              {
                "natural_key": "BCS II|CSC2201|Tuesday|08:00|LT1",
                "program_group": "BCS II",
                "day": "Tuesday",
                "time_slot": "08:00 - 10:00",
                "start_time": "08:00",
                "end_time": null,
                "course_code": "CSC2201",
                "course_title": "Operating Systems",
                "session_type": "Lecture",
                "lecturer": null,
                "room": "LT1",
                "shared_with": ["BSE II"],
                "unknown_future_field": "ignored"
              }
            ]
        """.trimIndent()

        val parsed = app.dataLoader.parseEntriesFromStream(ByteArrayInputStream(json.toByteArray()))
        assertEquals(1, parsed.size)
        val entry = parsed.single()
        assertEquals("BCS II|CSC2201|Tuesday|08:00|LT1", entry.naturalKey)
        assertEquals("CSC2201", entry.courseCode)
        assertEquals(listOf("BSE II"), entry.shared_with)
        assertEquals("LT1", entry.room)
    }

    @Test
    fun applicationDefersListenersAndLaunchAnalyticsUntilActivityForegrounds() {
        assertFalse("Cold non-UI launch must not mark app foregrounded", app.isAppInForeground.value)
        assertFalse("Cold non-UI launch must not attach Firestore timetable listeners", app.syncRepository.hasActiveTimetableListeners())
        assertFalse("Cold non-UI launch must not attach Firestore user listeners", app.syncRepository.hasActiveUserListeners())
        assertEquals("Cold non-UI launch must not inflate app_launch analytics", 0, app.analyticsManager.getFeatureCount("app_launch"))

        val controller = org.robolectric.Robolectric.buildActivity(ComponentActivity::class.java).create().start()
        Thread.sleep(200)
        assertTrue("Activity start must transition app to foreground", app.isAppInForeground.value)
        assertTrue("Foreground transition must record launch analytics once", app.analyticsManager.getFeatureCount("app_launch") >= 1)

        controller.stop()
        Thread.sleep(200)
        assertFalse("Activity stop must transition app to background", app.isAppInForeground.value)
        assertFalse("Backgrounding must detach timetable listeners", app.syncRepository.hasActiveTimetableListeners())
    }

    @Test
    fun timetableSyncWorkerPreservesTotalSessionsToAvoidRepeatUpdateNotifications() = runBlocking {
        val initialMeta = TimetableVersionResolver.resolve(
            rawVersion = "Draft 1",
            isExplicitFinal = false,
            totalEntries = 5,
            lastUpdatedMillis = 1_700_000_000_000L
        )
        app.database.timetableDao().upsertTimetableMetadata(initialMeta)

        val server = ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))
        val port = server.localPort
        val running = java.util.concurrent.atomic.AtomicBoolean(true)
        val serverThread = thread(isDaemon = true) {
            while (running.get()) {
                try {
                    val socket = server.accept()
                    socket.use { s ->
                        val reader = s.getInputStream().bufferedReader()
                        val requestLine = reader.readLine() ?: return@use
                        while (true) {
                            val line = reader.readLine() ?: break
                            if (line.isEmpty()) break
                        }
                        val out = s.getOutputStream()
                        when {
                            requestLine.contains("timetable_metadata.json") -> {
                                val body = """
                                    {
                                      "version_label": "Final 2026",
                                      "is_final": true,
                                      "academic_year": "2026/2027",
                                      "semester": "Semester 1",
                                      "total_sessions": 1304
                                    }
                                """.trimIndent()
                                val bytes = body.toByteArray()
                                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n\r\n".toByteArray())
                                out.write(bytes)
                            }
                            requestLine.contains("timetable_export.json") -> {
                                val body = """
                                    [
                                      {
                                        "natural_key": "BCS I|CSC4998|Monday|11:00|LT1",
                                        "program_group": "BCS I",
                                        "day": "Monday",
                                        "time_slot": "11:00 - 13:00",
                                        "start_time": "11:00",
                                        "end_time": "13:00",
                                        "course_code": "CSC4998",
                                        "course_title": "Systems",
                                        "session_type": "Lecture",
                                        "lecturer": "Dr. Grace",
                                        "room": "LT1"
                                      }
                                    ]
                                """.trimIndent()
                                val bytes = body.toByteArray()
                                out.write("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: ${bytes.size}\r\n\r\n".toByteArray())
                                out.write(bytes)
                            }
                            else -> out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".toByteArray())
                        }
                        out.flush()
                    }
                } catch (_: Exception) {}
            }
        }

        try {
            app.getSharedPreferences("mustime_sync_prefs", Context.MODE_PRIVATE).edit()
                .putString(TimetableSyncWorker.PREF_REMOTE_METADATA_URL, "http://127.0.0.1:$port/timetable_metadata.json")
                .commit()

            val res1 = TimetableSyncWorker.performSync(app, checkAppUpdate = false)
            assertEquals(ListenableWorker.Result.success(), res1)
            assertEquals(1304, app.database.timetableDao().getTimetableMetadataSync()?.totalSessions)
            assertEquals(1, shadowNotificationManager.allNotifications.size)

            notificationManager.cancelAll()
            val res2 = TimetableSyncWorker.performSync(app, checkAppUpdate = false)
            assertEquals(ListenableWorker.Result.success(), res2)
            assertTrue("Second sync with identical server metadata must NOT spam Timetable Updated", shadowNotificationManager.allNotifications.isEmpty())
        } finally {
            running.set(false)
            runCatching { server.close() }
            serverThread.join(500)
        }
    }

    @Test
    fun notificationIntentsAreSingleTopAndStoreSynchronizesConcurrentWritesWithoutDuplicateBanners() = runBlocking {
        val store = InAppNotificationStore.getInstance(app)
        store.clearAll()

        val emittedBanners = java.util.Collections.synchronizedList(mutableListOf<String>())
        val collectJob = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            InAppNotificationStore.foregroundAlertFlow.collect { item ->
                emittedBanners.add(item.title)
            }
        }

        try {
            // OS notification posts carry FLAG_ACTIVITY_SINGLE_TOP and suppress duplicate in-app toast banner
            assertTrue(
                NotificationHelper.showClassReminderNotification(
                    context = app,
                    courseCode = "CSC2101",
                    courseTitle = "OS",
                    room = "LT 1",
                    startTime = "09:00"
                )
            )
            val postedIntent = shadowOf(shadowNotificationManager.allNotifications.single().contentIntent).savedIntent
            assertTrue(
                "Notification deep-link intent must include FLAG_ACTIVITY_SINGLE_TOP",
                (postedIntent.flags and Intent.FLAG_ACTIVITY_SINGLE_TOP) != 0
            )
            assertTrue(
                "OS notification path must set emitForegroundBanner=false",
                emittedBanners.isEmpty()
            )

            // Explicit emitForegroundBanner=true emits foreground alert
            store.recordNotification(
                title = "Foreground Alert",
                message = "Active session",
                type = NotificationItemType.SYSTEM,
                emitForegroundBanner = true
            )
            assertTrue(emittedBanners.contains("Foreground Alert"))

            // Concurrent writes under storeLock retain every distinct item
            store.clearAll()
            val threads = (0 until 4).map { t ->
                thread {
                    repeat(5) { i ->
                        store.recordNotification(
                            title = "Item $t-$i",
                            message = "Body",
                            type = NotificationItemType.SYSTEM,
                            emitForegroundBanner = false
                        )
                    }
                }
            }
            threads.forEach { it.join() }
            assertEquals(20, store.notifications.value.size)
        } finally {
            collectJob.cancel()
        }
    }

    @Test
    fun userSettingsSyncManagerPreservesTimestampAndDailyDigest() = runBlocking {
        app.getSharedPreferences("mustime_settings", Context.MODE_PRIVATE)
            .edit()
            .remove("settings_updated_at")
            .commit()
        val manager = UserSettingsSyncManager(app.etagStore, app.onboardingDataStore)
        val initial = manager.exportCurrentSettings()
        assertEquals("Fresh device settings updatedAt must start at 0L", 0L, initial.updatedAt)

        val remote = UserSettingsSyncPayload(
            programme = "BCS II",
            themeMode = "DARK",
            themeAccent = 2,
            notificationsEnabled = true,
            classAlarmLeadMinutes = 15,
            taskReminderLeadHours = 6,
            alarmVibration = false,
            dailyDigestEnabled = false,
            academicYear = "2026/2027",
            semester = "Semester 2",
            updatedAt = 1_750_000_000_000L
        )
        manager.applySettings(remote)

        assertFalse(app.etagStore.isDailyDigestEnabled())
        assertEquals(15, app.etagStore.getClassAlarmLeadMinutes())
        assertEquals(1_750_000_000_000L, app.etagStore.getSettingsUpdatedAt())

        // Local mutation bumps settings timestamp strictly above the remote timestamp
        app.etagStore.setDailyDigestEnabled(true)
        assertTrue(app.etagStore.getSettingsUpdatedAt() > 1_750_000_000_000L)
        assertTrue(app.etagStore.dailyDigestEnabledPref.first())
    }
}
