package com.lectures.core.alarm

import android.content.Context
import android.util.Log
import com.lectures.features.timetable.data.TimetableRepository
import kotlinx.coroutines.CoroutineDispatcher
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
        preserveImminentExisting: Boolean = false,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        reason: String = "manual"
    ): AlarmResyncSummary = withContext(dispatcher) {
        syncMutex.withLock {
            val enabled = repository.etagStore.isNotificationsEnabled()
            val dailyDigestEnabled = repository.etagStore.isDailyDigestEnabled()
            val prog = repository.etagStore.getSavedProgramme()?.trim()
                ?: repository.getInitialProgramme()?.trim()
                ?: ""
            val leadMinutes = repository.etagStore.getClassAlarmLeadMinutes()

            val enrolledClasses = if (prog.isNotEmpty()) {
                repository.getLocalScheduleSync(prog)
            } else {
                emptyList()
            }
            val notesWithAlarms = repository.getNotesWithAlarms()
            val customEvents = repository.getCustomEventsSync()
            val assignments = repository.getAssignmentsSync()

            val scheduleDigest = buildString {
                append(enabled).append('|')
                append(dailyDigestEnabled).append('|')
                append(prog).append('|')
                append(leadMinutes).append('|')
                append(enrolledClasses.size).append(':')
                append(enrolledClasses.hashCode()).append('|')
                append(notesWithAlarms.size).append(':')
                notesWithAlarms.forEach { append(it.naturalKey).append('=').append(it.alarmMinutes).append(',') }
                append('|')
                append(customEvents.size).append(':')
                customEvents.forEach {
                    append(it.id).append('@').append(it.updatedAt).append('=').append(it.alarmMinutes).append(',')
                }
                append('|')
                append(assignments.size).append(':')
                assignments.forEach {
                    append(it.id).append('@').append(it.completed).append('=').append(it.reminderMinutes).append(',')
                }
            }

            val nowNano = System.nanoTime()
            if (!force && scheduleDigest == lastSignature && (nowNano - lastCompletedNano) in 0L..COALESCE_WINDOW_NS) {
                Log.d(TAG, "Coalescing redundant alarm resync ($reason) within window")
                return@withLock AlarmResyncSummary(notificationsEnabled = enabled)
            }

            val appContext = context.applicationContext
            val reminderScheduler = ReminderScheduler(appContext)
            val classScheduler = AlarmScheduler(appContext)
            val taskScheduler = TaskAlarmScheduler(appContext)

            if (!enabled) {
                reminderScheduler.cancelDailyReminder()
                if (enrolledClasses.isNotEmpty()) {
                    classScheduler.cancelAllEnrolledClassAlarms(enrolledClasses)
                }
                notesWithAlarms.forEach { classScheduler.cancelClassAlarm(it.naturalKey) }
                classScheduler.getTrackedClassKeys().forEach { classScheduler.cancelClassAlarm(it) }

                classScheduler.cancelAllCustomEventAlarms(customEvents)
                classScheduler.getTrackedCustomEventIds().forEach { classScheduler.cancelCustomEventAlarm(it) }

                taskScheduler.cancelAllTaskReminders(assignments)
                taskScheduler.getTrackedTaskIds().forEach { taskScheduler.cancelTaskReminder(it) }

                lastSignature = scheduleDigest
                lastCompletedNano = System.nanoTime()
                Log.d(TAG, "Quiet Mode active ($reason): cancelled all local alarms")
                return@withLock AlarmResyncSummary(notificationsEnabled = false)
            }

            // 1. Daily Morning Digest
            if (dailyDigestEnabled) {
                reminderScheduler.scheduleDailyReminder()
            } else {
                reminderScheduler.cancelDailyReminder()
            }

            // 2. Academic Task Reminders (never trigger catch-up 5s spam on bulk resync) + orphan pruning
            var taskCount = 0
            val scheduledTaskIds = mutableSetOf<Long>()
            assignments.forEach { task ->
                if (!task.completed && !task.isDeleted && task.reminderMinutes != null) {
                    if (taskScheduler.scheduleTaskReminder(task, allowImmediateCatchup = false)) {
                        taskCount++
                        scheduledTaskIds.add(task.id)
                    } else {
                        taskScheduler.cancelTaskReminder(task.id)
                    }
                } else {
                    taskScheduler.cancelTaskReminder(task.id)
                }
            }
            (taskScheduler.getTrackedTaskIds() - scheduledTaskIds).forEach { staleTaskId ->
                taskScheduler.cancelTaskReminder(staleTaskId)
            }

            // 3. Enrolled Classes + Custom Note Alarms + orphan pruning
            val customNoteMap = notesWithAlarms
                .mapNotNull { note -> note.alarmMinutes?.let { note.naturalKey to it } }
                .toMap()
            val enrolledKeys = enrolledClasses.map { it.naturalKey }.toSet()
            val desiredClassKeys = enrolledKeys + customNoteMap.keys

            (classScheduler.getTrackedClassKeys() - desiredClassKeys).forEach { staleClassKey ->
                classScheduler.cancelClassAlarm(staleClassKey)
            }

            val enrolledCount = if (enrolledClasses.isNotEmpty()) {
                classScheduler.scheduleAllEnrolledClassAlarms(
                    classes = enrolledClasses,
                    leadMinutes = leadMinutes,
                    isAlarm = false,
                    customAlarmNotes = customNoteMap,
                    preserveImminentExisting = preserveImminentExisting
                )
            } else {
                0
            }

            notesWithAlarms.forEach { note ->
                if (note.naturalKey !in enrolledKeys) {
                    val mins = note.alarmMinutes ?: return@forEach
                    val entry = repository.getTimetableEntryByNaturalKey(note.naturalKey)
                    if (entry != null) {
                        classScheduler.scheduleClassAlarm(
                            entry = entry,
                            minutesBefore = mins,
                            isAlarm = true,
                            preserveImminentExisting = preserveImminentExisting
                        )
                    } else {
                        classScheduler.scheduleClassAlarmFromNote(
                            note = note,
                            minutesBefore = mins,
                            preserveImminentExisting = preserveImminentExisting
                        )
                    }
                }
            }

            // 4. Personal Activities (CustomEvents) + orphan pruning
            var eventCount = 0
            val scheduledEventIds = mutableSetOf<Long>()
            customEvents.forEach { event ->
                if (!event.isDeleted && event.alarmMinutes != null && event.id > 0L) {
                    if (classScheduler.scheduleCustomEventAlarm(
                            event = event,
                            preserveImminentExisting = preserveImminentExisting
                        )
                    ) {
                        eventCount++
                        scheduledEventIds.add(event.id)
                    }
                } else if (event.id > 0L) {
                    classScheduler.cancelCustomEventAlarm(event.id)
                }
            }
            (classScheduler.getTrackedCustomEventIds() - scheduledEventIds).forEach { staleEventId ->
                classScheduler.cancelCustomEventAlarm(staleEventId)
            }

            lastSignature = scheduleDigest
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
