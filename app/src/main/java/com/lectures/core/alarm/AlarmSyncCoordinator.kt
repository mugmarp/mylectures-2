package com.lectures.core.alarm

import android.content.Context
import android.util.Log
import com.lectures.features.timetable.data.TimetableRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class AlarmResyncSummary(
    val notificationsEnabled: Boolean,
    val enrolledClassCount: Int = 0,
    val taskCount: Int = 0,
    val customNoteCount: Int = 0,
    val customEventCount: Int = 0
)

/**
 * Serialized, single-source-of-truth coordinator for scheduling and cancelling
 * all local AlarmManager alarms (Daily Digest, Enrolled Classes, Custom Note Alarms,
 * Custom Activities, and Academic Tasks).
 */
object AlarmSyncCoordinator {

    private const val TAG = "AlarmSyncCoordinator"
    private const val COALESCE_WINDOW_NS = 2_500_000_000L // 2.5 seconds

    private val syncMutex = Mutex()

    @Volatile
    private var lastCompletedNano: Long = 0L

    @Volatile
    private var lastSignature: String? = null

    suspend fun resyncAll(
        context: Context,
        repository: TimetableRepository,
        force: Boolean = false,
        reason: String = "manual"
    ): AlarmResyncSummary = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val enabled = repository.etagStore.isNotificationsEnabled()
            val prog = repository.etagStore.getSavedProgramme()?.trim()
                ?: repository.getInitialProgramme()?.trim()
                ?: ""
            val leadMinutes = repository.etagStore.getClassAlarmLeadMinutes()
            val signature = "$enabled|$prog|$leadMinutes"

            val nowNano = System.nanoTime()
            if (!force && signature == lastSignature && (nowNano - lastCompletedNano) in 0L..COALESCE_WINDOW_NS) {
                Log.d(TAG, "Coalescing redundant alarm resync ($reason) within window")
                return@withLock AlarmResyncSummary(notificationsEnabled = enabled)
            }

            val appContext = context.applicationContext
            val reminderScheduler = ReminderScheduler(appContext)
            val classScheduler = AlarmScheduler(appContext)
            val taskScheduler = TaskAlarmScheduler(appContext)

            val enrolledClasses = if (prog.isNotEmpty()) {
                repository.getLocalScheduleSync(prog)
            } else {
                emptyList()
            }
            val notesWithAlarms = repository.getNotesWithAlarms()
            val customEvents = repository.getCustomEventsSync()
            val assignments = repository.getAssignmentsSync()

            if (!enabled) {
                reminderScheduler.cancelDailyReminder()
                if (enrolledClasses.isNotEmpty()) {
                    classScheduler.cancelAllEnrolledClassAlarms(enrolledClasses)
                }
                notesWithAlarms.forEach { classScheduler.cancelClassAlarm(it.naturalKey) }
                classScheduler.cancelAllCustomEventAlarms(customEvents)
                taskScheduler.cancelAllTaskReminders(assignments)

                lastSignature = signature
                lastCompletedNano = System.nanoTime()
                Log.d(TAG, "Quiet Mode active ($reason): cancelled all local alarms")
                return@withLock AlarmResyncSummary(notificationsEnabled = false)
            }

            // 1. Daily Morning Digest
            reminderScheduler.scheduleDailyReminder()

            // 2. Academic Task Reminders (never trigger catch-up 5s spam on bulk resync)
            var taskCount = 0
            assignments.forEach { task ->
                if (!task.completed && task.reminderMinutes != null) {
                    if (taskScheduler.scheduleTaskReminder(task, allowImmediateCatchup = false)) {
                        taskCount++
                    }
                } else {
                    taskScheduler.cancelTaskReminder(task.id)
                }
            }

            // 3. Enrolled Classes + Custom Note Alarms (avoid scheduling overlapping keys twice)
            val customNoteMap = notesWithAlarms
                .mapNotNull { note -> note.alarmMinutes?.let { note.naturalKey to it } }
                .toMap()
            val enrolledKeys = enrolledClasses.map { it.naturalKey }.toSet()

            val enrolledCount = if (enrolledClasses.isNotEmpty()) {
                classScheduler.scheduleAllEnrolledClassAlarms(
                    classes = enrolledClasses,
                    leadMinutes = leadMinutes,
                    isAlarm = false,
                    customAlarmNotes = customNoteMap
                )
            } else {
                0
            }

            notesWithAlarms.forEach { note ->
                if (note.naturalKey !in enrolledKeys) {
                    val mins = note.alarmMinutes ?: return@forEach
                    val entry = repository.getTimetableEntryByNaturalKey(note.naturalKey)
                    if (entry != null) {
                        classScheduler.scheduleClassAlarm(entry, mins, isAlarm = true)
                    } else {
                        classScheduler.scheduleClassAlarmFromNote(note, mins)
                    }
                }
            }

            // 4. Personal Activities (CustomEvents)
            var eventCount = 0
            customEvents.forEach { event ->
                if (event.alarmMinutes != null) {
                    if (classScheduler.scheduleCustomEventAlarm(event)) {
                        eventCount++
                    }
                } else {
                    classScheduler.cancelCustomEventAlarm(event.id)
                }
            }

            lastSignature = signature
            lastCompletedNano = System.nanoTime()
            Log.d(
                TAG,
                "Alarm resync complete ($reason): classes=$enrolledCount, tasks=$taskCount, notes=${customNoteMap.size}, events=$eventCount"
            )
            AlarmResyncSummary(
                notificationsEnabled = true,
                enrolledClassCount = enrolledCount,
                taskCount = taskCount,
                customNoteCount = customNoteMap.size,
                customEventCount = eventCount
            )
        }
    }
}
