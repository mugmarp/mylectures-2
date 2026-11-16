package com.lectures.core.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.TimetableEntry
import java.util.Calendar

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val TAG = "AlarmScheduler"

    companion object {
        const val ACTION_TRIGGER_CLASS_ALARM = "com.lectures.ACTION_TRIGGER_CLASS_ALARM"
        const val ACTION_TRIGGER_CUSTOM_EVENT_ALARM = "com.lectures.ACTION_TRIGGER_CUSTOM_EVENT_ALARM"
        const val ACTION_TRIGGER_TEST_CLASS_ALARM = "com.lectures.ACTION_TRIGGER_TEST_CLASS_ALARM"
        const val ACTION_TRIGGER_SNOOZE_CLASS_ALARM = "com.lectures.ACTION_TRIGGER_SNOOZE_CLASS_ALARM"

        fun classAlarmUri(naturalKey: String): Uri =
            Uri.parse("lectures://alarm/class/${Uri.encode(naturalKey)}")

        fun customEventAlarmUri(eventId: Long): Uri =
            Uri.parse("lectures://alarm/event/$eventId")

        fun testClassAlarmUri(naturalKey: String): Uri =
            Uri.parse("lectures://alarm/class_test/${Uri.encode(naturalKey)}")

        fun snoozeClassAlarmUri(key: String): Uri =
            Uri.parse("lectures://alarm/class_snooze/${Uri.encode(key)}")

        internal fun stableRequestCode(key: String): Int {
            val h = key.hashCode()
            val positive = if (h == Int.MIN_VALUE) 1 else kotlin.math.abs(h)
            return 100_000 + (positive % 1_000_000_000)
        }

        /**
         * Computes the next future trigger timestamp in epoch millis for a weekly class occurrence.
         * Advances to the target day-of-week FIRST before subtracting [minutesBefore], ensuring
         * early-morning classes whose lead time crosses midnight (e.g. Monday 00:15 - 30m = Sunday 23:45)
         * advance strictly into the future instead of firing in the past.
         */
        fun calculateNextTriggerMillis(
            targetDow: Int,
            hour: Int,
            minute: Int,
            minutesBefore: Int,
            nowMillis: Long = System.currentTimeMillis()
        ): Long {
            val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val currentDow = nowCal.get(Calendar.DAY_OF_WEEK)
            val daysDiff = (targetDow - currentDow + 7) % 7

            val triggerCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, daysDiff)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MINUTE, -minutesBefore)
            }

            while (triggerCal.timeInMillis <= nowMillis + 60_000L) {
                triggerCal.add(Calendar.DAY_OF_YEAR, 7)
            }
            return triggerCal.timeInMillis
        }
    }

    private fun parseStartTime(rawTime: String): Pair<Int, Int>? {
        val clean = rawTime.trim().uppercase()
        val isPm = clean.endsWith("PM")
        val isAm = clean.endsWith("AM")
        val stripped = clean.removeSuffix("PM").removeSuffix("AM").trim()
        val parts = stripped.split(":")
        if (parts.size != 2) return null
        var hour = parts[0].trim().toIntOrNull() ?: return null
        val minute = parts[1].trim().toIntOrNull() ?: return null
        if (isPm && hour < 12) hour += 12
        if (isAm && hour == 12) hour = 0
        if (hour !in 0..23 || minute !in 0..59) return null
        return Pair(hour, minute)
    }

    fun scheduleClassAlarm(entry: TimetableEntry, minutesBefore: Int, isAlarm: Boolean = true): Boolean {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available")
            return false
        }

        val targetDow = when (entry.dayOfWeek.trim().lowercase().take(3)) {
            "mon" -> Calendar.MONDAY
            "tue" -> Calendar.TUESDAY
            "wed" -> Calendar.WEDNESDAY
            "thu" -> Calendar.THURSDAY
            "fri" -> Calendar.FRIDAY
            "sat" -> Calendar.SATURDAY
            "sun" -> Calendar.SUNDAY
            else -> null
        }
        if (targetDow == null) {
            Log.e(TAG, "Cannot schedule alarm: unrecognized dayOfWeek '${entry.dayOfWeek}' for ${entry.courseCode}")
            return false
        }

        val time = parseStartTime(entry.startTime)
        if (time == null) {
            Log.e(TAG, "Cannot schedule alarm: invalid startTime '${entry.startTime}' for ${entry.courseCode}")
            return false
        }
        val (hour, minute) = time

        val triggerMillis = calculateNextTriggerMillis(
            targetDow = targetDow,
            hour = hour,
            minute = minute,
            minutesBefore = minutesBefore
        )
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CLASS_ALARM
            data = classAlarmUri(entry.naturalKey)
            putExtra("NATURAL_KEY", entry.naturalKey)
            putExtra("DAY_OF_WEEK", entry.dayOfWeek)
            putExtra("COURSE_CODE", entry.courseCode)
            putExtra("COURSE_TITLE", entry.courseTitle)
            putExtra("ROOM", entry.room ?: "TBD")
            putExtra("START_TIME", entry.startTime)
            putExtra("LECTURER", entry.lecturer ?: "")
            putExtra("MINUTES_BEFORE", minutesBefore)
            putExtra("IS_ALARM", isAlarm)
            putExtra("IS_RECURRING", true)
            putExtra("IS_TEST", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode("class_${entry.naturalKey}"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "class alarm for ${entry.courseCode}"
        )
    }

    private fun scheduleRtcWakeupWithFallback(
        triggerMillis: Long,
        pendingIntent: PendingIntent,
        label: String
    ): Boolean {
        val manager = alarmManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (manager.canScheduleExactAlarms()) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact $label at $triggerMillis")
                } else {
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact $label at $triggerMillis")
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled M exact $label at $triggerMillis")
            } else {
                manager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact $label, falling back to inexact: ${e.message}")
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    manager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
                true
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback inexact scheduling failed for $label: ${fallbackEx.message}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed scheduling $label: ${e.message}")
            false
        }
    }

    /**
     * Schedules a class alarm directly from a saved LectureNote metadata.
     * Supports both timetable-keyed notes (`CODE|Title|Day|Time|Room`) and
     * standalone notes that have an `attachedClass` (`CODE: Title · Day, HH:mm – HH:mm`).
     */
    fun scheduleClassAlarmFromNote(note: com.lectures.features.timetable.domain.LectureNote, minutesBefore: Int): Boolean {
        val parts = note.naturalKey.split("|")
        val code = parts.getOrNull(0)?.trim()?.ifBlank { null } ?: (note.title?.take(10) ?: "Lecture")
        val title = note.title?.ifBlank { null } ?: parts.getOrNull(1)?.trim() ?: code

        val keyDay = parts.getOrNull(2)?.trim()
        val keyTime = parts.getOrNull(3)?.trim()
        val keyRoom = parts.getOrNull(4)?.trim()

        val hasValidKeySchedule = keyDay != null && parseDayPrefix(keyDay) != null &&
            keyTime != null && parseStartTime(keyTime) != null

        var day = if (hasValidKeySchedule) keyDay!! else ""
        var time = if (hasValidKeySchedule) keyTime!! else ""
        val room = keyRoom?.ifBlank { null } ?: "Lecture Room"

        if (!hasValidKeySchedule && !note.attachedClass.isNullOrBlank()) {
            // Parse format: "SWE2107: Systems Analysis · Monday, 11:00 – 13:00"
            val schedulePart = note.attachedClass.substringAfter("·", "").trim()
            if (schedulePart.isNotBlank()) {
                val tokens = schedulePart.split(",", limit = 2).map { it.trim() }
                val candidateDay = tokens.getOrNull(0).orEmpty()
                val timeRange = tokens.getOrNull(1).orEmpty()
                val candidateStart = timeRange
                    .split("–", "-", "to")
                    .firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (parseDayPrefix(candidateDay) != null && parseStartTime(candidateStart) != null) {
                    day = candidateDay
                    time = candidateStart
                }
            }
        }

        if (day.isBlank() || time.isBlank()) {
            return false
        }

        val syntheticEntry = TimetableEntry(
            natural_key = note.naturalKey,
            program_group = "",
            day = day,
            time_slot = time,
            start_time = time,
            end_time = null,
            course_code = code,
            course_title = title,
            session_type = null,
            lecturer = null,
            room = room
        )
        return scheduleClassAlarm(syntheticEntry, minutesBefore)
    }

    private fun parseDayPrefix(rawDay: String): Int? = when (rawDay.trim().lowercase().take(3)) {
        "mon" -> Calendar.MONDAY
        "tue" -> Calendar.TUESDAY
        "wed" -> Calendar.WEDNESDAY
        "thu" -> Calendar.THURSDAY
        "fri" -> Calendar.FRIDAY
        "sat" -> Calendar.SATURDAY
        "sun" -> Calendar.SUNDAY
        else -> null
    }

    fun triggerTestAlarm(entry: TimetableEntry, delaySeconds: Int = 3): Boolean {
        if (alarmManager == null) return false
        val triggerMillis = System.currentTimeMillis() + (delaySeconds * 1000L)
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_TEST_CLASS_ALARM
            data = testClassAlarmUri(entry.naturalKey)
            putExtra("NATURAL_KEY", entry.naturalKey)
            putExtra("DAY_OF_WEEK", entry.dayOfWeek)
            putExtra("COURSE_CODE", entry.courseCode)
            putExtra("COURSE_TITLE", entry.courseTitle)
            putExtra("ROOM", entry.room ?: "TBD")
            putExtra("START_TIME", entry.startTime)
            putExtra("LECTURER", entry.lecturer ?: "")
            putExtra("MINUTES_BEFORE", 0)
            putExtra("IS_ALARM", true)
            putExtra("IS_RECURRING", false)
            putExtra("IS_TEST", true)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode("test_${entry.naturalKey}"),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "test alarm for ${entry.courseCode}"
        )
    }

    fun scheduleCustomEventAlarm(event: CustomEvent): Boolean {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager service not available")
            return false
        }
        val minutesBefore = event.alarmMinutes
        if (event.id <= 0L || event.isDeleted || minutesBefore == null) {
            if (event.id > 0L) cancelCustomEventAlarm(event.id)
            return false
        }

        val targetDow = parseDayPrefix(event.dayOfWeek) ?: run {
            Log.e(TAG, "Cannot schedule custom event alarm: invalid dayOfWeek '${event.dayOfWeek}' for ${event.title}")
            return false
        }
        val (hour, minute) = parseStartTime(event.startTime) ?: run {
            Log.e(TAG, "Cannot schedule custom event alarm: invalid startTime '${event.startTime}' for ${event.title}")
            return false
        }

        val triggerMillis = calculateNextTriggerMillis(
            targetDow = targetDow,
            hour = hour,
            minute = minute,
            minutesBefore = minutesBefore
        )

        val naturalKey = "custom_event_${event.id}"
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CUSTOM_EVENT_ALARM
            data = customEventAlarmUri(event.id)
            putExtra("NATURAL_KEY", naturalKey)
            putExtra("CUSTOM_EVENT_ID", event.id)
            putExtra("DAY_OF_WEEK", event.dayOfWeek)
            putExtra("COURSE_CODE", event.category.ifBlank { "Activity" })
            putExtra("COURSE_TITLE", event.title)
            putExtra("ROOM", event.location.ifBlank { "Personal Activity" })
            putExtra("START_TIME", event.startTime)
            putExtra("LECTURER", "")
            putExtra("MINUTES_BEFORE", minutesBefore)
            putExtra("IS_ALARM", true)
            putExtra("IS_RECURRING", event.repeatWeekly)
            putExtra("IS_CUSTOM_EVENT", true)
            putExtra("IS_TEST", false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode(naturalKey),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "custom activity alarm for ${event.title}"
        )
    }

    fun cancelCustomEventAlarm(eventId: Long) {
        if (eventId <= 0L) return
        val manager = alarmManager ?: return
        val naturalKey = "custom_event_$eventId"
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CUSTOM_EVENT_ALARM
            data = customEventAlarmUri(eventId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode(naturalKey),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled custom event alarm for $eventId")
        }
        cancelSnoozeAlarm(naturalKey)
    }

    fun cancelAllCustomEventAlarms(events: List<CustomEvent>) {
        if (alarmManager == null || events.isEmpty()) return
        events.forEach { cancelCustomEventAlarm(it.id) }
    }

    fun cancelSnoozeAlarm(snoozeKey: String) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SNOOZE_CLASS_ALARM
            data = snoozeClassAlarmUri(snoozeKey)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode("snooze_$snoozeKey"),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled snooze alarm for $snoozeKey")
        }
    }

    fun cancelClassAlarm(naturalKey: String) {
        val manager = alarmManager ?: return
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_CLASS_ALARM
            data = classAlarmUri(naturalKey)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            stableRequestCode("class_$naturalKey"),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled class alarm for $naturalKey")
        }
        cancelSnoozeAlarm(naturalKey)
    }

    /**
     * Automatically arms recurring class reminders/alarms for all lectures
     * in the student's enrolled academic programme while preserving any
     * custom per-note alarm lead times.
     */
    fun scheduleAllEnrolledClassAlarms(
        classes: List<TimetableEntry>,
        leadMinutes: Int,
        isAlarm: Boolean = false,
        customAlarmNotes: Map<String, Int> = emptyMap()
    ): Int {
        if (alarmManager == null || classes.isEmpty()) return 0
        val uniqueClasses = classes.distinctBy {
            if (it.naturalKey.isNotBlank()) it.naturalKey else "${it.courseCode}|${it.dayOfWeek}|${it.startTime}"
        }
        var scheduledCount = 0
        for (entry in uniqueClasses) {
            val customLead = customAlarmNotes[entry.naturalKey]
            val effectiveLead = customLead ?: leadMinutes
            val effectiveIsAlarm = if (customLead != null) true else isAlarm
            val ok = scheduleClassAlarm(entry, effectiveLead, effectiveIsAlarm)
            if (ok) scheduledCount++
        }
        Log.d(TAG, "Scheduled $scheduledCount enrolled class alerts (lead = ${leadMinutes}m, preservedNotes = ${customAlarmNotes.size}) out of ${uniqueClasses.size} unique lectures")
        return scheduledCount
    }

    /**
     * Cancels alarms for all given timetable entries (e.g. on programme switch or quiet mode).
     */
    fun cancelAllEnrolledClassAlarms(classes: List<TimetableEntry>) {
        if (alarmManager == null || classes.isEmpty()) return
        classes.distinctBy {
            if (it.naturalKey.isNotBlank()) it.naturalKey else "${it.courseCode}|${it.dayOfWeek}|${it.startTime}"
        }.forEach {
            cancelClassAlarm(it.naturalKey)
        }
        Log.d(TAG, "Cancelled alarms for ${classes.size} enrolled class entries")
    }

    fun scheduleSnoozeAlarm(
        courseCode: String,
        courseTitle: String,
        room: String,
        startTime: String,
        snoozeMinutes: Int = 10,
        minutesBefore: Int = 30,
        lecturer: String = "",
        naturalKey: String? = null,
        dayOfWeek: String? = null,
        isCustomEvent: Boolean = false,
        customEventId: Long = -1L
    ) {
        if (alarmManager == null) return
        val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val snoozeKey = naturalKey ?: "$courseCode|$room|$startTime|$dayOfWeek"
        val intent = Intent(context, ClassAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_SNOOZE_CLASS_ALARM
            data = snoozeClassAlarmUri(snoozeKey)
            putExtra("COURSE_CODE", courseCode)
            putExtra("COURSE_TITLE", courseTitle)
            putExtra("ROOM", room)
            putExtra("START_TIME", startTime)
            putExtra("MINUTES_BEFORE", minutesBefore)
            putExtra("LECTURER", lecturer)
            putExtra("IS_CUSTOM_EVENT", isCustomEvent)
            if (customEventId > 0L) putExtra("CUSTOM_EVENT_ID", customEventId)
            naturalKey?.let { putExtra("NATURAL_KEY", it) }
            dayOfWeek?.let { putExtra("DAY_OF_WEEK", it) }
            putExtra("IS_ALARM", true)
            putExtra("IS_RECURRING", false)
            putExtra("IS_TEST", false)
            putExtra("IS_SNOOZED", true)
        }
        val requestCode = stableRequestCode("snooze_$snoozeKey")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        scheduleRtcWakeupWithFallback(
            triggerMillis = triggerMillis,
            pendingIntent = pendingIntent,
            label = "snooze alarm for $courseCode"
        )
    }
}
