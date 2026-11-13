package com.lectures

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
}
