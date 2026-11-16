package com.lectures

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.BootReceiver
import com.lectures.core.alarm.DailyReminderReceiver
import com.lectures.core.alarm.ReminderScheduler
import com.lectures.core.alarm.TaskActionReceiver
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.alarm.TaskReminderReceiver
import com.lectures.core.notification.NotificationHelper
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowNotificationManager
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase3AlarmAndNotificationTest {

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
        }
        shadowAlarmManager.scheduledAlarms.clear()
        notificationManager.cancelAll()
    }

    @After
    fun tearDown() {
        runBlocking {
            app.etagStore.setNotificationsEnabled(true)
        }
        shadowAlarmManager.scheduledAlarms.clear()
        notificationManager.cancelAll()
    }

    @Test
    fun mondayEarlyMorningClassCheckedOnLateSundaySchedulesStrictlyInFuture() {
        // Case 1: Checked on Sunday at 23:30 for Monday 00:15 with 30m lead -> Sunday 23:45 (15 mins ahead)
        val sun2330 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 4, 23, 30, 0) // Oct 4, 2026 is Sunday
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val upcomingSameNight = AlarmScheduler.calculateNextTriggerMillis(
            targetDow = Calendar.MONDAY,
            hour = 0,
            minute = 15,
            minutesBefore = 30,
            nowMillis = sun2330
        )
        assertTrue(
            "Trigger at Sun 23:45 must be strictly after Sun 23:30",
            upcomingSameNight > sun2330
        )
        assertEquals(15 * 60_000L, upcomingSameNight - sun2330)

        // Case 2: Checked on Sunday at 23:50 for Monday 00:15 with 30m lead (window Sun 23:45 missed)
        val sun2350 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 4, 23, 50, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val rolledToNextWeek = AlarmScheduler.calculateNextTriggerMillis(
            targetDow = Calendar.MONDAY,
            hour = 0,
            minute = 15,
            minutesBefore = 30,
            nowMillis = sun2350
        )
        assertTrue(
            "When lead trigger (Sun 23:45) has already passed at Sun 23:50, alarm must roll forward +7d",
            rolledToNextWeek > sun2350
        )
        val expectedNextSunday = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 11, 23, 45, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        assertEquals(expectedNextSunday, rolledToNextWeek)
    }

    @Test
    fun inexactAlarmFallbackSucceedsWhenExactAlarmPermissionRevoked() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        try {
            val classScheduler = AlarmScheduler(app)
            val entry = TimetableEntry(
                natural_key = "BCS I|CSC3101|Wednesday|10:00|Lab 2",
                program_group = "BCS I",
                day = "Wednesday",
                time_slot = "10:00 - 12:00",
                start_time = "10:00",
                end_time = "12:00",
                course_code = "CSC3101",
                course_title = "Compiler Design",
                session_type = "Lecture",
                lecturer = "Dr. Ada",
                room = "Lab 2"
            )
            assertTrue("Class alarm must fall back to inexact when exact denied", classScheduler.scheduleClassAlarm(entry, 15))
            assertTrue("Test alarm must fall back to inexact when exact denied", classScheduler.triggerTestAlarm(entry, 3))
            assertTrue("Daily digest must fall back to inexact when exact denied", ReminderScheduler(app).scheduleDailyReminder())

            val task = Assignment(
                id = 701L,
                title = "Read Chapter 4",
                courseCode = "CSC3101",
                dueDate = "Tomorrow • 16:00",
                reminderMinutes = 30,
                priority = "High",
                completed = false
            )
            assertTrue("Task alarm must fall back to inexact when exact denied", TaskAlarmScheduler(app).scheduleTaskReminder(task))
            assertEquals(4, shadowAlarmManager.scheduledAlarms.size)
        } finally {
            ShadowAlarmManager.setCanScheduleExactAlarms(true)
        }
    }

    @Test
    fun taskTestAndScheduledPendingIntentsUseDistinctUrisAndDoNotClobberEachOther() {
        val taskScheduler = TaskAlarmScheduler(app)
        val task = Assignment(
            id = 55L,
            title = "OS Kernel Project",
            courseCode = "CSC2204",
            dueDate = "Tomorrow • 14:00",
            reminderMinutes = 60,
            priority = "High",
            completed = false
        )

        assertTrue(taskScheduler.scheduleTaskReminder(task))
        taskScheduler.scheduleTestReminder(task, delaySeconds = 5)

        val alarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Test alarm must not clobber real task alarm", 2, alarms.size)

        val intent0 = shadowOf(alarms[0].operation).savedIntent
        val intent1 = shadowOf(alarms[1].operation).savedIntent
        assertNotEquals(intent0.data, intent1.data)

        // Cancelling the real task alarm leaves only the test alarm intact
        taskScheduler.cancelTaskReminder(task.id)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertTrue(shadowOf(shadowAlarmManager.scheduledAlarms.single().operation).savedIntent.getBooleanExtra("IS_TEST", false))
    }

    @Test
    fun bootReceiverRespectsQuietModeAcrossAllFourAlarmGroups() = runBlocking {
        // Seed DB items so boot has tasks & custom note alarms that COULD be scheduled
        app.database.assignmentDao().upsert(
            Assignment(
                id = 910L,
                title = "Boot Test Assignment",
                courseCode = "CSC1101",
                dueDate = "Tomorrow • 17:00",
                reminderMinutes = 30,
                priority = "High",
                completed = false
            )
        )
        app.database.timetableDao().upsertNote(
            LectureNote(
                naturalKey = "CSC1101|Intro|Monday|09:00|Hall 1",
                content = "Review",
                updatedAt = System.currentTimeMillis(),
                alarmMinutes = 15,
                title = "Intro Lecture"
            )
        )
        app.etagStore.setProgrammePref("BCS I")
        app.etagStore.setNotificationsEnabled(false)
        shadowAlarmManager.scheduledAlarms.clear()

        BootReceiver().onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))
        // Allow io dispatcher inside goAsync() brief window
        Thread.sleep(350)

        assertTrue(
            "Quiet Mode on boot must NOT schedule Daily Digest, Task Reminders, Custom Note Alarms, or Enrolled Class Alarms",
            shadowAlarmManager.scheduledAlarms.isEmpty()
        )
    }

    @Test
    fun dailyReminderReceiverInQuietModeNeitherPostsNorRearmsTomorrow() = runBlocking {
        app.etagStore.setNotificationsEnabled(false)
        ReminderScheduler(app).scheduleDailyReminder()
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        val broadcast = Intent(app, DailyReminderReceiver::class.java).apply {
            action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
            putExtra("IS_TEST", false)
        }
        DailyReminderReceiver().onReceive(app, broadcast)
        Thread.sleep(350)

        assertTrue(
            "Quiet Mode daily trigger must not post notification",
            shadowNotificationManager.allNotifications.isEmpty()
        )
        assertTrue(
            "Quiet Mode daily trigger must cancel daily alarm rather than re-arming tomorrow",
            shadowAlarmManager.scheduledAlarms.isEmpty()
        )
    }

    @Test
    fun taskReminderReceiverRespectsQuietModeUnlessExplicitTest() = runBlocking {
        app.etagStore.setNotificationsEnabled(false)

        val receiver = TaskReminderReceiver()
        val regularIntent = Intent(app, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, 301L)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, "Hidden in Quiet Mode")
            putExtra("IS_TEST", false)
        }
        receiver.onReceive(app, regularIntent)
        assertTrue("Task reminder should be suppressed in Quiet Mode", shadowNotificationManager.allNotifications.isEmpty())

        val testIntent = Intent(app, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, 302L)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, "Explicit Test")
            putExtra("IS_TEST", true)
        }
        receiver.onReceive(app, testIntent)
        assertEquals("Explicit test reminder should still display", 1, shadowNotificationManager.allNotifications.size)
    }

    @Test
    fun taskActionReceiverMarksTaskCompleteAndCancelsScheduledAlarm() = runBlocking {
        val taskId = 812L
        val task = Assignment(
            id = taskId,
            title = "Submit Network Lab",
            courseCode = "CSC2102",
            dueDate = "Tomorrow • 18:00",
            reminderMinutes = 30,
            priority = "Medium",
            completed = false
        )
        app.database.assignmentDao().upsert(task)
        assertTrue(TaskAlarmScheduler(app).scheduleTaskReminder(task))
        NotificationHelper.showTaskReminderNotification(app, taskId, task.title, task.courseCode, task.dueDate, task.priority)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        assertFalse(shadowNotificationManager.allNotifications.isEmpty())

        val doneIntent = Intent(app, TaskActionReceiver::class.java).apply {
            action = TaskActionReceiver.ACTION_MARK_TASK_DONE
            putExtra(TaskActionReceiver.EXTRA_TASK_ID, taskId)
        }
        TaskActionReceiver().onReceive(app, doneIntent)
        Thread.sleep(350)

        val updated = app.database.assignmentDao().getAllRawSync().firstOrNull { it.id == taskId }
        assertNotNull(updated)
        assertTrue("Task must be marked completed in DB", updated!!.completed)
        assertTrue("Notification must be cancelled", shadowNotificationManager.allNotifications.isEmpty())
        assertTrue("Scheduled task alarm must be cancelled", shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun notificationHelperActionsCarryDistinctUris() {
        NotificationHelper.showTaskReminderNotification(app, 99L, "Quiz Prep", "MTH1101", "Tomorrow", "High")
        val notif = shadowNotificationManager.allNotifications.single()
        val markDoneIntent = shadowOf(notif.actions[0].actionIntent).savedIntent
        val dismissIntent = shadowOf(notif.actions[1].actionIntent).savedIntent
        val viewIntent = shadowOf(notif.contentIntent).savedIntent

        assertNotEquals(markDoneIntent.data, dismissIntent.data)
        assertNotEquals(viewIntent.data, markDoneIntent.data)
        assertEquals("lectures", viewIntent.data?.scheme)
    }

    @Test
    fun taskDateTimeParserPreservesExplicitTimeOnDedicatedPickerFormatAndNextWeek() {
        val fixedDueMillis = com.lectures.core.alarm.TaskDateTimeParser.calculateDueMillis("Mon, Nov 16, 2026 17:00")
        assertNotNull(fixedDueMillis)
        val cal = Calendar.getInstance().apply { timeInMillis = fixedDueMillis!! }
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, cal.get(Calendar.MONTH))
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(17, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))

        // Dynamic +10d future test: 17:00 - 60m lead MUST trigger at 16:00 (not 22:59 end-of-day)
        val futureDateCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 10) }
        val futureDateStr = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US).format(futureDateCal.time)
        val triggerMillis = com.lectures.core.alarm.TaskDateTimeParser.calculateTriggerTimeMillis("$futureDateStr 17:00", 60)
        assertNotNull(triggerMillis)
        val trigCal = Calendar.getInstance().apply { timeInMillis = triggerMillis!! }
        assertEquals(16, trigCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, trigCal.get(Calendar.MINUTE))

        val nextWeekMillis = com.lectures.core.alarm.TaskDateTimeParser.calculateDueMillis("Next Week")
        assertNotNull(nextWeekMillis)
        val diffDays = (nextWeekMillis!! - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)
        assertTrue("Next Week should resolve roughly 6..8 days from now (was $diffDays)", diffDays in 6..8)
    }

    @Test
    fun scheduleAllEnrolledClassAlarmsPreservesCustomNotesAndSeparatesChannels() {
        val scheduler = AlarmScheduler(app)
        val entryRegular = TimetableEntry(
            natural_key = "BCS I|CSC1101|Monday|09:00|Hall 1",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "09:00 - 11:00",
            start_time = "09:00",
            end_time = "11:00",
            course_code = "CSC1101",
            course_title = "Programming I",
            session_type = "Lecture",
            lecturer = "Dr. A",
            room = "Hall 1"
        )
        val entryWithCustomNote = TimetableEntry(
            natural_key = "BCS I|CSC1102|Tuesday|14:00|Lab 2",
            program_group = "BCS I",
            day = "Tuesday",
            time_slot = "14:00 - 16:00",
            start_time = "14:00",
            end_time = "16:00",
            course_code = "CSC1102",
            course_title = "Web Dev",
            session_type = "Lab",
            lecturer = "Dr. B",
            room = "Lab 2"
        )

        val count = scheduler.scheduleAllEnrolledClassAlarms(
            classes = listOf(entryRegular, entryWithCustomNote),
            leadMinutes = 30,
            isAlarm = false,
            customAlarmNotes = mapOf(entryWithCustomNote.naturalKey to 15)
        )
        assertEquals(2, count)
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        val intents = shadowAlarmManager.scheduledAlarms.map { shadowOf(it.operation).savedIntent }
        val regularIntent = intents.first { it.getStringExtra("COURSE_CODE") == "CSC1101" }
        val customIntent = intents.first { it.getStringExtra("COURSE_CODE") == "CSC1102" }

        assertEquals(30, regularIntent.getIntExtra("MINUTES_BEFORE", -1))
        assertFalse("Routine enrolled class should use reminder channel (isAlarm=false)", regularIntent.getBooleanExtra("IS_ALARM", true))

        assertEquals(15, customIntent.getIntExtra("MINUTES_BEFORE", -1))
        assertTrue("Class with custom note alert should preserve isAlarm=true", customIntent.getBooleanExtra("IS_ALARM", false))
    }

    @Test
    fun notificationHelperHonorsVibrationSettingAndSkipsInAppHistoryWhenBlocked() = runBlocking {
        val store = com.lectures.core.notification.InAppNotificationStore.getInstance(app)
        store.clearAll()

        app.etagStore.setAlarmVibration(false)
        try {
            val posted = NotificationHelper.showClassAlarmNotification(
                context = app,
                courseCode = "CSC4101",
                courseTitle = "Distributed Systems",
                room = "Lab 3",
                startTime = "11:00"
            )
            assertTrue(posted)
            assertEquals(1, store.notifications.value.size)

            notificationManager.cancelAll()
            store.clearAll()

            // Revoke permission -> must return false and not record phantom history
            shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
            val postedWhenDenied = NotificationHelper.showTaskReminderNotification(
                context = app,
                taskId = 505L,
                title = "Blocked Task",
                courseCode = "CSC4101",
                dueDate = "Tomorrow",
                priority = "High"
            )
            assertFalse(postedWhenDenied)
            assertTrue("Blocked notification must not be recorded in InAppNotificationStore", store.notifications.value.isEmpty())
        } finally {
            app.etagStore.setAlarmVibration(true)
            shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
