package com.lectures

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.AlarmSyncCoordinator
import com.lectures.core.alarm.BootReceiver
import com.lectures.core.alarm.ClassAlarmReceiver
import com.lectures.core.alarm.DailyReminderReceiver
import com.lectures.core.alarm.ReminderScheduler
import com.lectures.core.alarm.TaskActionReceiver
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.alarm.TaskDateTimeParser
import com.lectures.core.alarm.TaskReminderReceiver
import com.lectures.core.notification.NotificationHelper
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
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

    @Test
    fun customEventAlarmSchedulingCancellationAndReceiverValidation() = runBlocking {
        val eventId = app.database.customEventDao().upsert(
            CustomEvent(
                title = "AI Study Group",
                category = "Study",
                dayOfWeek = "Wednesday",
                startTime = "15:00",
                endTime = "16:30",
                location = "Library 204",
                alarmMinutes = 15,
                repeatWeekly = true
            )
        )
        val event = app.database.customEventDao().getById(eventId)!!
        val scheduler = AlarmScheduler(app)

        assertTrue(scheduler.scheduleCustomEventAlarm(event))
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        val armedIntent = shadowOf(shadowAlarmManager.scheduledAlarms.single().operation).savedIntent
        assertEquals(AlarmScheduler.ACTION_TRIGGER_CUSTOM_EVENT_ALARM, armedIntent.action)
        assertEquals(eventId, armedIntent.getLongExtra("CUSTOM_EVENT_ID", -1L))

        // Simulate receiver firing for active repeatWeekly event -> posts notification and re-arms next week
        shadowAlarmManager.scheduledAlarms.clear()
        ClassAlarmReceiver().onReceive(app, armedIntent)
        Thread.sleep(350)
        assertEquals(1, shadowNotificationManager.allNotifications.size)
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Mark event deleted in Room -> stale broadcast must suppress notification and cancel alarm
        notificationManager.cancelAll()
        app.database.customEventDao().softDelete(eventId)
        ClassAlarmReceiver().onReceive(app, armedIntent)
        Thread.sleep(350)
        assertTrue(
            "Deleted custom event alarm must not notify",
            shadowNotificationManager.allNotifications.isEmpty()
        )
        assertTrue(
            "Deleted custom event alarm must cancel itself",
            shadowAlarmManager.scheduledAlarms.isEmpty()
        )
    }

    @Test
    fun cancelClassAlarmAndCustomEventAlarmAlsoCancelPendingSnoozes() {
        val scheduler = AlarmScheduler(app)
        val entry = TimetableEntry(
            natural_key = "BCS I|CSC3301|Thursday|10:00|LT 1",
            program_group = "BCS I",
            day = "Thursday",
            time_slot = "10:00 - 12:00",
            start_time = "10:00",
            end_time = "12:00",
            course_code = "CSC3301",
            course_title = "Algorithms",
            session_type = "Lecture",
            lecturer = "Dr. Lin",
            room = "LT 1"
        )
        assertTrue(scheduler.scheduleClassAlarm(entry, 30))
        scheduler.scheduleSnoozeAlarm(
            courseCode = entry.courseCode,
            courseTitle = entry.courseTitle,
            room = entry.room ?: "LT 1",
            startTime = entry.startTime,
            snoozeMinutes = 10,
            naturalKey = entry.naturalKey,
            dayOfWeek = entry.dayOfWeek
        )
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
        scheduler.cancelClassAlarm(entry.naturalKey)
        assertTrue(
            "Cancelling class alarm must also cancel any pending snooze",
            shadowAlarmManager.scheduledAlarms.isEmpty()
        )

        val event = CustomEvent(
            id = 42L,
            title = "Team Sync",
            category = "Club",
            dayOfWeek = "Friday",
            startTime = "16:00",
            endTime = "17:00",
            location = "Room 5",
            alarmMinutes = 10
        )
        assertTrue(scheduler.scheduleCustomEventAlarm(event))
        scheduler.scheduleSnoozeAlarm(
            courseCode = event.category,
            courseTitle = event.title,
            room = event.location,
            startTime = event.startTime,
            snoozeMinutes = 10,
            naturalKey = "custom_event_${event.id}",
            dayOfWeek = event.dayOfWeek,
            isCustomEvent = true,
            customEventId = event.id
        )
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)
        scheduler.cancelCustomEventAlarm(event.id)
        assertTrue(
            "Cancelling custom event alarm must also cancel any pending snooze",
            shadowAlarmManager.scheduledAlarms.isEmpty()
        )
    }

    @Test
    fun taskDateTimeParserDoesNotTriggerImmediateCatchupOnDefaultResync() {
        val twoHoursFromNow = Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
        }
        val formattedDue = java.text.SimpleDateFormat("EEE, MMM d, yyyy HH:mm", java.util.Locale.US)
            .format(twoHoursFromNow.time)

        // Lead time 180m means trigger window was 1 hour ago, though due date is still in the future
        val defaultResyncTrigger = TaskDateTimeParser.calculateTriggerTimeMillis(
            dueDateStr = formattedDue,
            reminderMinutes = 180,
            allowImmediateCatchup = false
        )
        assertNull(
            "Bulk resync/boot must return null instead of firing 5s catch-up spam for passed reminder windows",
            defaultResyncTrigger
        )

        val now = System.currentTimeMillis()
        val interactiveSaveTrigger = TaskDateTimeParser.calculateTriggerTimeMillis(
            dueDateStr = formattedDue,
            reminderMinutes = 180,
            allowImmediateCatchup = true
        )
        assertNotNull(interactiveSaveTrigger)
        assertTrue(interactiveSaveTrigger!! in (now + 1_000L)..(now + 10_000L))
    }

    @Test
    fun taskReminderReceiverSuppressesStaleCompletedOrDeletedTasksInRoom() = runBlocking {
        val activeTask = Assignment(
            id = 941L,
            title = "Active Paper",
            courseCode = "CSC4102",
            dueDate = "Tomorrow • 18:00",
            reminderMinutes = 30,
            priority = "High",
            completed = false
        )
        val completedTask = Assignment(
            id = 942L,
            title = "Already Finished",
            courseCode = "CSC4102",
            dueDate = "Tomorrow • 18:00",
            reminderMinutes = 30,
            priority = "Low",
            completed = true
        )
        app.database.assignmentDao().upsert(activeTask)
        app.database.assignmentDao().upsert(completedTask)

        TaskAlarmScheduler(app).scheduleTaskReminder(activeTask)
        // Schedule manually for completed id using an active copy first so an alarm sits in AlarmManager
        TaskAlarmScheduler(app).scheduleTaskReminder(completedTask.copy(completed = false))
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        val receiver = TaskReminderReceiver()

        // Completed task broadcast must NOT notify and must cancel its leftover alarm
        receiver.onReceive(app, Intent(app, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, completedTask.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, completedTask.title)
            putExtra("IS_TEST", false)
        })
        Thread.sleep(350)
        assertTrue(shadowNotificationManager.allNotifications.isEmpty())
        assertEquals(1, shadowAlarmManager.scheduledAlarms.size)

        // Active task broadcast must notify normally
        receiver.onReceive(app, Intent(app, TaskReminderReceiver::class.java).apply {
            action = TaskReminderReceiver.ACTION_TRIGGER_TASK_REMINDER
            putExtra(TaskReminderReceiver.EXTRA_TASK_ID, activeTask.id)
            putExtra(TaskReminderReceiver.EXTRA_TASK_TITLE, activeTask.title)
            putExtra("IS_TEST", false)
        })
        Thread.sleep(350)
        assertEquals(1, shadowNotificationManager.allNotifications.size)
    }

    @Test
    fun bootReceiverHandlesTimezoneAndExactAlarmPermissionChanges() = runBlocking {
        app.etagStore.setProgrammePref("BCS I")
        app.etagStore.setNotificationsEnabled(true)
        app.database.customEventDao().upsert(
            CustomEvent(
                title = "Robotics Lab",
                category = "Lab",
                dayOfWeek = "Monday",
                startTime = "10:00",
                endTime = "12:00",
                location = "Eng 101",
                alarmMinutes = 15,
                repeatWeekly = true
            )
        )

        shadowAlarmManager.scheduledAlarms.clear()
        BootReceiver().onReceive(app, Intent(Intent.ACTION_TIMEZONE_CHANGED))
        Thread.sleep(350)
        assertTrue(
            "Timezone change must reschedule Daily Digest + active CustomEvent alarms",
            shadowAlarmManager.scheduledAlarms.size >= 2
        )

        shadowAlarmManager.scheduledAlarms.clear()
        BootReceiver().onReceive(app, Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))
        Thread.sleep(350)
        assertTrue(
            "Exact alarm permission grant change must trigger force resync",
            shadowAlarmManager.scheduledAlarms.size >= 2
        )
    }

    @Test
    fun dailyReminderReceiverIncludesSortedClassesCustomEventsAndDueTasks() = runBlocking {
        val todayDayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.US)
            .format(java.util.Date())
        app.etagStore.setProgrammePref("BCS III")
        app.etagStore.setNotificationsEnabled(true)

        // Seed afternoon class BEFORE morning class to verify chronological sorting
        app.database.timetableDao().upsertEntries(
            listOf(
                TimetableEntry(
                    natural_key = "BCS III|CSC301|${todayDayName}|14:00|LT2",
                    program_group = "BCS III",
                    day = todayDayName,
                    time_slot = "14:00 - 16:00",
                    start_time = "14:00",
                    end_time = "16:00",
                    course_code = "CSC301",
                    course_title = "Afternoon Graphics",
                    session_type = "Lecture",
                    lecturer = "Dr. Y",
                    room = "LT2"
                ),
                TimetableEntry(
                    natural_key = "BCS III|CSC302|${todayDayName}|08:00|LT1",
                    program_group = "BCS III",
                    day = todayDayName,
                    time_slot = "08:00 - 10:00",
                    start_time = "08:00",
                    end_time = "10:00",
                    course_code = "CSC302",
                    course_title = "Morning AI",
                    session_type = "Lecture",
                    lecturer = "Dr. X",
                    room = "LT1"
                )
            )
        )
        app.database.customEventDao().upsert(
            CustomEvent(
                title = "Mentor Meeting",
                category = "Activity",
                dayOfWeek = todayDayName,
                startTime = "11:00",
                endTime = "11:30",
                location = "Office 3"
            )
        )
        app.database.assignmentDao().upsert(
            Assignment(
                id = 990L,
                title = "Capstone Proposal",
                courseCode = "CSC399",
                dueDate = "Today • 17:00",
                priority = "High",
                completed = false
            )
        )

        DailyReminderReceiver().onReceive(
            app,
            Intent(app, DailyReminderReceiver::class.java).apply {
                action = DailyReminderReceiver.ACTION_TRIGGER_DAILY_DIGEST
                putExtra("IS_TEST", true)
            }
        )
        Thread.sleep(400)

        val notif = shadowNotificationManager.allNotifications.single()
        val bigText = notif.extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
            ?: notif.extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty()
        assertTrue("Digest should contain morning CSC302", bigText.contains("CSC302"))
        assertTrue("Digest should contain afternoon CSC301", bigText.contains("CSC301"))
        assertTrue("Morning 08:00 must sort before afternoon 14:00", bigText.indexOf("CSC302") < bigText.indexOf("CSC301"))
        assertTrue("Digest should include today's custom event", bigText.contains("Mentor Meeting"))
        assertTrue("Digest should include task due today", bigText.contains("Capstone Proposal"))
    }

    @Test
    fun imminentClassAlertIsPreservedDuringBackgroundResyncAndExactMinuteFiresToday() {
        val scheduler = AlarmScheduler(app)

        // 1. Deterministic 07:29:15 -> 08:00 (30m lead = 07:30:00): minLeadMs=0L schedules TODAY, minLeadMs=60_000L rolls +7d
        val tue072915 = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 6, 7, 29, 15)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayAt0730 = AlarmScheduler.calculateNextTriggerMillis(
            targetDow = Calendar.TUESDAY,
            hour = 8,
            minute = 0,
            minutesBefore = 30,
            nowMillis = tue072915,
            minLeadMs = 0L
        )
        assertEquals(45_000L, todayAt0730 - tue072915)

        val nextWeekAt0730 = AlarmScheduler.calculateNextTriggerMillis(
            targetDow = Calendar.TUESDAY,
            hour = 8,
            minute = 0,
            minutesBefore = 30,
            nowMillis = tue072915,
            minLeadMs = 60_000L
        )
        assertEquals(7L * 24 * 60 * 60_000L + 45_000L, nextWeekAt0730 - tue072915)

        // 2. Preserve an existing imminent alarm instead of shifting it +7d on foreground/digest resync
        val classStartCal = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayName = java.text.SimpleDateFormat("EEEE", java.util.Locale.US).format(classStartCal.time)
        val startTimeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(classStartCal.time)
        val entry = TimetableEntry(
            natural_key = "BCS I|CSC3501|$dayName|$startTimeStr|LT1",
            program_group = "BCS I",
            day = dayName,
            time_slot = "$startTimeStr - 10:00",
            start_time = startTimeStr,
            end_time = "10:00",
            course_code = "CSC3501",
            course_title = "Real-Time Systems",
            session_type = "Lecture",
            lecturer = "Dr. Grace",
            room = "LT1"
        )

        // Arm existing PendingIntent 1 minute ahead (29m lead) so today's 30m lead falls in [now-60s..now]
        assertTrue(scheduler.scheduleClassAlarm(entry, minutesBefore = 29, isAlarm = false))
        val initialTrigger = shadowAlarmManager.scheduledAlarms.single().triggerAtTime

        // Morning digest / foreground resync with preserveImminentExisting=true keeps initialTrigger intact
        assertTrue(
            scheduler.scheduleClassAlarm(
                entry = entry,
                minutesBefore = 30,
                isAlarm = false,
                preserveImminentExisting = true
            )
        )
        assertEquals(
            "Imminent pending alert must be preserved rather than jumped +7 days",
            initialTrigger,
            shadowAlarmManager.scheduledAlarms.single().triggerAtTime
        )
    }

    @Test
    fun urgentAlarmsUseSetAlarmClockWhenPermittedAndFallbackWhenDenied() {
        val scheduler = AlarmScheduler(app)
        val entry = TimetableEntry(
            natural_key = "BCS I|CSC3601|Friday|09:00|Lab 4",
            program_group = "BCS I",
            day = "Friday",
            time_slot = "09:00 - 11:00",
            start_time = "09:00",
            end_time = "11:00",
            course_code = "CSC3601",
            course_title = "Network Security",
            session_type = "Lecture",
            lecturer = "Dr. Paul",
            room = "Lab 4"
        )

        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        assertTrue(scheduler.scheduleClassAlarm(entry, minutesBefore = 15, isAlarm = true))
        assertNotNull("Urgent class alarm must register AlarmClockInfo", alarmManager.nextAlarmClock)

        shadowAlarmManager.scheduledAlarms.clear()
        val event = CustomEvent(
            id = 88L,
            title = "Capstone Demo",
            category = "Presentation",
            dayOfWeek = "Friday",
            startTime = "14:00",
            endTime = "15:00",
            location = "Auditoria",
            alarmMinutes = 15,
            repeatWeekly = true
        )
        assertTrue(scheduler.scheduleCustomEventAlarm(event))
        assertNotNull("Custom event alarm must register AlarmClockInfo", alarmManager.nextAlarmClock)

        shadowAlarmManager.scheduledAlarms.clear()
        scheduler.scheduleSnoozeAlarm(
            courseCode = "CSC3601",
            courseTitle = "Network Security",
            room = "Lab 4",
            startTime = "09:00",
            snoozeMinutes = 10,
            naturalKey = entry.naturalKey,
            dayOfWeek = entry.dayOfWeek
        )
        assertNotNull("Snooze alarm must register AlarmClockInfo", alarmManager.nextAlarmClock)

        // When exact alarm permission is revoked, fall back gracefully without crashing
        scheduler.cancelClassAlarm(entry.naturalKey)
        scheduler.cancelCustomEventAlarm(event.id)
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        try {
            shadowAlarmManager.scheduledAlarms.clear()
            assertTrue(scheduler.scheduleClassAlarm(entry, minutesBefore = 15, isAlarm = true))
            assertNull(alarmManager.nextAlarmClock)
            assertEquals(1, shadowAlarmManager.scheduledAlarms.size)
        } finally {
            ShadowAlarmManager.setCanScheduleExactAlarms(true)
        }
    }

    @Test
    fun programmeSwitchPrunesStaleTrackedClassAlarmsViaCoordinator() = runBlocking {
        val entryBcs1 = TimetableEntry(
            natural_key = "BCS I|CSC1101|Monday|08:00|Hall A",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "08:00 - 10:00",
            start_time = "08:00",
            end_time = "10:00",
            course_code = "CSC1101",
            course_title = "Computing Basics",
            session_type = "Lecture",
            lecturer = "Dr. A",
            room = "Hall A"
        )
        val entryBcs2 = TimetableEntry(
            natural_key = "BCS II|CSC2201|Tuesday|10:00|Hall B",
            program_group = "BCS II",
            day = "Tuesday",
            time_slot = "10:00 - 12:00",
            start_time = "10:00",
            end_time = "12:00",
            course_code = "CSC2201",
            course_title = "Data Structures",
            session_type = "Lecture",
            lecturer = "Dr. B",
            room = "Hall B"
        )
        app.database.timetableDao().upsertEntries(listOf(entryBcs1, entryBcs2))
        app.etagStore.setProgrammePref("BCS I")
        AlarmSyncCoordinator.resyncAll(app, app.repository, force = true, reason = "test_bcs1")

        val scheduler = AlarmScheduler(app)
        assertTrue(scheduler.getTrackedClassKeys().contains(entryBcs1.naturalKey))

        // Switch programme to BCS II -> BCS I alarm must be pruned from both AlarmManager and tracked set
        app.etagStore.setProgrammePref("BCS II")
        AlarmSyncCoordinator.resyncAll(app, app.repository, force = true, reason = "test_bcs2")

        val trackedAfter = scheduler.getTrackedClassKeys()
        assertFalse("Old programme alarm must be pruned", trackedAfter.contains(entryBcs1.naturalKey))
        assertTrue("New programme alarm must be tracked", trackedAfter.contains(entryBcs2.naturalKey))
        val armedNaturalKeys = shadowAlarmManager.scheduledAlarms.mapNotNull {
            shadowOf(it.operation).savedIntent.getStringExtra("NATURAL_KEY")
        }
        assertFalse(armedNaturalKeys.contains(entryBcs1.naturalKey))
        assertTrue(armedNaturalKeys.contains(entryBcs2.naturalKey))
    }

    @Test
    fun classAlarmReceiverSuppressesOrphanedCohortAlarmAndDisarmsOneTimeCustomEvent() = runBlocking {
        // 1. Orphaned class alarm after cohort change (and no custom LectureNote)
        val entry = TimetableEntry(
            natural_key = "BCS I|CSC1999|Monday|09:00|Hall C",
            program_group = "BCS I",
            day = "Monday",
            time_slot = "09:00 - 11:00",
            start_time = "09:00",
            end_time = "11:00",
            course_code = "CSC1999",
            course_title = "Orphaned Course",
            session_type = "Lecture",
            lecturer = "Dr. C",
            room = "Hall C"
        )
        app.database.timetableDao().upsertEntries(listOf(entry))
        app.etagStore.setProgrammePref("BCS II") // User switched away from BCS I

        val scheduler = AlarmScheduler(app)
        assertTrue(scheduler.scheduleClassAlarm(entry, 30, isAlarm = false))
        val orphanedIntent = shadowOf(shadowAlarmManager.scheduledAlarms.single().operation).savedIntent

        ClassAlarmReceiver().onReceive(app, orphanedIntent)
        Thread.sleep(350)

        assertTrue("Orphaned cohort alarm must be suppressed", shadowNotificationManager.allNotifications.isEmpty())
        assertTrue("Orphaned cohort alarm must cancel itself", shadowAlarmManager.scheduledAlarms.isEmpty())

        // 2. Non-repeating custom event fires once, clears alarmMinutes, and does NOT re-arm next week
        val oneTimeId = app.database.customEventDao().upsert(
            CustomEvent(
                title = "One-Time Seminar",
                category = "Study",
                dayOfWeek = "Wednesday",
                startTime = "15:00",
                endTime = "16:00",
                location = "Hall D",
                alarmMinutes = 15,
                repeatWeekly = false
            )
        )
        val oneTimeEvent = app.database.customEventDao().getById(oneTimeId)!!
        assertTrue(scheduler.scheduleCustomEventAlarm(oneTimeEvent))
        val oneTimeIntent = shadowOf(shadowAlarmManager.scheduledAlarms.single().operation).savedIntent

        shadowAlarmManager.scheduledAlarms.clear()
        ClassAlarmReceiver().onReceive(app, oneTimeIntent)
        Thread.sleep(350)

        assertEquals(1, shadowNotificationManager.allNotifications.size)
        assertTrue("Non-repeating custom event must NOT re-arm for next week", shadowAlarmManager.scheduledAlarms.isEmpty())
        assertNull(
            "One-time event alarmMinutes must be cleared in Room after firing",
            app.database.customEventDao().getById(oneTimeId)?.alarmMinutes
        )
    }

    @Test
    fun taskDateTimeParserDateOnlyAnchorsAreStrictlyMonotonic() {
        val triggers = listOf(720, 360, 120, 60, 30).map { mins ->
            mins to TaskDateTimeParser.calculateTriggerTimeMillis("Tomorrow", mins, allowImmediateCatchup = false)
        }
        triggers.forEach { (mins, value) ->
            assertNotNull("Trigger for ${mins}m before tomorrow must not be null", value)
        }
        val times = triggers.map { it.second!! }
        for (i in 0 until times.size - 1) {
            assertTrue(
                "Larger lead time must fire strictly earlier (${triggers[i].first}m=${times[i]} vs ${triggers[i + 1].first}m=${times[i + 1]})",
                times[i] < times[i + 1]
            )
        }
    }

    @Test
    fun blockedReminderChannelsRespectsDailyDigestToggle() = runBlocking {
        NotificationHelper.createNotificationChannels(app)
        val digestChannel = notificationManager.getNotificationChannel(NotificationHelper.CHANNEL_DAILY_DIGEST)!!
        val originalImportance = digestChannel.importance
        try {
            digestChannel.importance = NotificationManager.IMPORTANCE_NONE
            app.etagStore.setDailyDigestEnabled(true)
            assertTrue(
                "Muted Daily Digest channel must be flagged when digest toggle is enabled",
                com.lectures.core.util.NotificationPermissionHelper.getBlockedReminderChannels(app)
                    .contains(NotificationHelper.CHANNEL_DAILY_DIGEST)
            )

            app.etagStore.setDailyDigestEnabled(false)
            assertFalse(
                "Muted Daily Digest channel must NOT trigger blocked-channel banner when digest toggle is disabled",
                com.lectures.core.util.NotificationPermissionHelper.getBlockedReminderChannels(app)
                    .contains(NotificationHelper.CHANNEL_DAILY_DIGEST)
            )
        } finally {
            digestChannel.importance = originalImportance
            app.etagStore.setDailyDigestEnabled(true)
        }
    }
}
