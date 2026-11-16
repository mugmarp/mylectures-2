package com.lectures.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lectures.TimetableApplication
import com.lectures.core.notification.NotificationHelper
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class ClassAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_DISMISS_CLASS_ALARM = "com.lectures.ACTION_DISMISS_CLASS_ALARM"
        const val ACTION_SNOOZE_CLASS_ALARM = "com.lectures.ACTION_SNOOZE_CLASS_ALARM"
        const val EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == ACTION_DISMISS_CLASS_ALARM) {
            val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notifId != -1) {
                androidx.core.app.NotificationManagerCompat.from(context).cancel(notifId)
                Log.d("ClassAlarmReceiver", "Dismissed notification $notifId via action button")
            }
            return
        }

        if (action == ACTION_SNOOZE_CLASS_ALARM) {
            val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
            if (notifId != -1) {
                androidx.core.app.NotificationManagerCompat.from(context).cancel(notifId)
            }
            val courseCode = intent.getStringExtra("COURSE_CODE") ?: "Lecture"
            val courseTitle = intent.getStringExtra("COURSE_TITLE") ?: "Class"
            val room = intent.getStringExtra("ROOM") ?: "Classroom"
            val startTime = intent.getStringExtra("START_TIME") ?: "Soon"
            val minutesBefore = intent.getIntExtra("MINUTES_BEFORE", 30)
            val lecturer = intent.getStringExtra("LECTURER") ?: ""
            val naturalKey = intent.getStringExtra("NATURAL_KEY")
            val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK")
            val isCustomEvent = intent.getBooleanExtra("IS_CUSTOM_EVENT", false)
            val customEventId = intent.getLongExtra("CUSTOM_EVENT_ID", -1L)

            AlarmScheduler(context).scheduleSnoozeAlarm(
                courseCode = courseCode,
                courseTitle = courseTitle,
                room = room,
                startTime = startTime,
                snoozeMinutes = 10,
                minutesBefore = minutesBefore,
                lecturer = lecturer,
                naturalKey = naturalKey,
                dayOfWeek = dayOfWeek,
                isCustomEvent = isCustomEvent,
                customEventId = customEventId
            )
            Log.d("ClassAlarmReceiver", "Snoozed alert for $courseCode for 10 minutes (custom=$isCustomEvent)")
            return
        }

        val courseCode = intent.getStringExtra("COURSE_CODE") ?: "Lecture"
        val courseTitle = intent.getStringExtra("COURSE_TITLE") ?: "Class"
        val room = intent.getStringExtra("ROOM") ?: "Classroom"
        val startTime = intent.getStringExtra("START_TIME") ?: "Soon"
        val lecturer = intent.getStringExtra("LECTURER") ?: ""
        val isAlarm = intent.getBooleanExtra("IS_ALARM", false)
        val minutesBefore = intent.getIntExtra("MINUTES_BEFORE", 30)
        val isRecurring = intent.getBooleanExtra("IS_RECURRING", true)
        val isTest = intent.getBooleanExtra("IS_TEST", false)
        val isSnoozed = intent.getBooleanExtra("IS_SNOOZED", false)
        val isCustomEvent = intent.getBooleanExtra("IS_CUSTOM_EVENT", false) ||
            action == AlarmScheduler.ACTION_TRIGGER_CUSTOM_EVENT_ALARM
        val customEventId = intent.getLongExtra("CUSTOM_EVENT_ID", -1L)
        val naturalKey = intent.getStringExtra("NATURAL_KEY")
        val dayOfWeek = intent.getStringExtra("DAY_OF_WEEK")

        val app = context.applicationContext as? TimetableApplication
        val etagStore = app?.etagStore ?: com.lectures.features.timetable.data.ETagStore(context)
        if (!etagStore.isNotificationsEnabled() && !isTest) {
            Log.d("ClassAlarmReceiver", "Notifications disabled in settings; suppressing alert for $courseCode")
            return
        }

        // Custom Events (Personal Activities): validate current Room state & reschedule weekly if repeatWeekly
        if (isCustomEvent && !isTest && customEventId > 0L && app != null) {
            val pendingResult = goAsync()
            val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            receiverScope.launch {
                try {
                    withTimeout(5_000L) {
                        val event = app.repository.getCustomEventByIdSync(customEventId)
                        if (event == null || event.isDeleted || (!isSnoozed && event.alarmMinutes == null)) {
                            Log.d("ClassAlarmReceiver", "Suppressing stale/deleted custom event alert #$customEventId")
                            AlarmScheduler(context).cancelCustomEventAlarm(customEventId)
                            return@withTimeout
                        }
                        val lead = event.alarmMinutes ?: minutesBefore
                        NotificationHelper.showClassAlarmNotification(
                            context = context,
                            courseCode = event.category.ifBlank { courseCode },
                            courseTitle = event.title,
                            room = event.location.ifBlank { "Personal Activity" },
                            startTime = event.startTime,
                            minutesBefore = lead,
                            lecturer = "",
                            naturalKey = "custom_event_${event.id}",
                            dayOfWeek = event.dayOfWeek,
                            isSnoozed = isSnoozed,
                            isCustomEvent = true,
                            customEventId = event.id
                        )
                        if (isRecurring && !isSnoozed && event.repeatWeekly) {
                            AlarmScheduler(context).scheduleCustomEventAlarm(event)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ClassAlarmReceiver", "Error handling custom event alarm #$customEventId: ${e.message}", e)
                } finally {
                    receiverScope.cancel()
                    pendingResult?.finish()
                }
            }
            return
        }

        Log.d("ClassAlarmReceiver", "Class alert fired (isAlarm=$isAlarm, isRecurring=$isRecurring, isSnoozed=$isSnoozed) for $courseCode in $room at $startTime")

        if (isAlarm) {
            NotificationHelper.showClassAlarmNotification(
                context = context,
                courseCode = courseCode,
                courseTitle = courseTitle,
                room = room,
                startTime = startTime,
                minutesBefore = minutesBefore,
                lecturer = lecturer,
                naturalKey = naturalKey,
                dayOfWeek = dayOfWeek,
                isSnoozed = isSnoozed,
                isCustomEvent = isCustomEvent,
                customEventId = customEventId
            )
        } else {
            NotificationHelper.showClassReminderNotification(
                context = context,
                courseCode = courseCode,
                courseTitle = courseTitle,
                room = room,
                startTime = startTime,
                minutesBefore = minutesBefore,
                lecturer = lecturer,
                naturalKey = naturalKey,
                dayOfWeek = dayOfWeek
            )
        }

        // Reschedule recurring alarm/reminder for next week (+7 days)
        if (isRecurring && !isTest && !isSnoozed && !naturalKey.isNullOrBlank() && !dayOfWeek.isNullOrBlank()) {
            try {
                val syntheticEntry = TimetableEntry(
                    natural_key = naturalKey,
                    program_group = "",
                    day = dayOfWeek,
                    time_slot = startTime,
                    start_time = startTime,
                    end_time = null,
                    course_code = courseCode,
                    course_title = courseTitle,
                    session_type = null,
                    lecturer = lecturer,
                    room = room
                )
                AlarmScheduler(context).scheduleClassAlarm(syntheticEntry, minutesBefore, isAlarm)
                Log.d("ClassAlarmReceiver", "Successfully re-armed weekly class alert for $courseCode on next $dayOfWeek (isAlarm=$isAlarm)")
            } catch (e: Exception) {
                Log.e("ClassAlarmReceiver", "Failed to reschedule weekly class alarm: ${e.message}")
            }
        }
    }
}
