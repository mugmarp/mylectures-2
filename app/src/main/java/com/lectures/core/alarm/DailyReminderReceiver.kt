package com.lectures.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lectures.TimetableApplication
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.util.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DailyReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_DAILY_DIGEST = "com.lectures.ACTION_TRIGGER_DAILY_DIGEST"
        private const val TAG = "DailyReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val isTest = intent.getBooleanExtra("IS_TEST", false)
        Log.d(TAG, "Daily reminder broadcast received (isTest=$isTest)")

        val app = context.applicationContext as? TimetableApplication ?: return
        val repository = app.repository
        val etagStore = app.etagStore

        val pendingResult = goAsync()
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        receiverScope.launch {
            try {
                withTimeout(8_500L) {
                    val notificationsEnabled = etagStore.isNotificationsEnabled()
                    val digestEnabled = etagStore.isDailyDigestEnabled()
                    if ((!notificationsEnabled || !digestEnabled) && !isTest) {
                        Log.d(TAG, "Daily digest disabled (notifications=$notificationsEnabled, digest=$digestEnabled); cancelling schedule")
                        ReminderScheduler(context).cancelDailyReminder()
                        return@withTimeout
                    }

                    // Re-arm for tomorrow morning only when notifications are enabled and this was an actual alarm trigger
                    if (!isTest) {
                        ReminderScheduler(context).scheduleDailyReminder()
                    }

                    // Determine today's day of week (always English/Locale.US to match stored timetable days)
                    val cal = Calendar.getInstance()
                    val dayFormat = SimpleDateFormat("EEEE", Locale.US)
                    val dayOfWeek = dayFormat.format(cal.time)
                    val todayYear = cal.get(Calendar.YEAR)
                    val todayMonth = cal.get(Calendar.MONTH)
                    val todayDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

                    val programme = etagStore.getSavedProgramme()?.trim()
                        ?: repository.getInitialProgramme()?.trim()
                        ?: ""

                    val allCohortClasses = if (programme.isNotEmpty()) {
                        repository.getLocalScheduleSync(programme)
                    } else {
                        emptyList()
                    }

                    val todayClasses = (if (programme.isNotEmpty()) {
                        allCohortClasses.filter { matchesDay(it.dayOfWeek, dayOfWeek) }
                    } else {
                        repository.getScheduleForDaySync(dayOfWeek)
                    }).sortedBy {
                        val mins = TimeUtil.toMinutes(it.startTime)
                        if (mins >= 0) mins else Int.MAX_VALUE
                    }

                    val todayCustomEvents = repository.getCustomEventsSync()
                        .filter { !it.isDeleted && matchesDay(it.dayOfWeek, dayOfWeek) }
                        .sortedBy {
                            val mins = TimeUtil.toMinutes(it.startTime)
                            if (mins >= 0) mins else Int.MAX_VALUE
                        }

                    val dueTodayTasks = repository.getAssignmentsSync()
                        .filter { task ->
                            if (task.completed || task.isDeleted) return@filter false
                            val dueMillis = TaskDateTimeParser.calculateDueMillis(task.dueDate) ?: return@filter false
                            val dueCal = Calendar.getInstance().apply { timeInMillis = dueMillis }
                            dueCal.get(Calendar.YEAR) == todayYear &&
                                dueCal.get(Calendar.MONTH) == todayMonth &&
                                dueCal.get(Calendar.DAY_OF_MONTH) == todayDayOfMonth
                        }

                    NotificationHelper.showDailyDigestNotification(
                        context = context,
                        dayOfWeek = dayOfWeek,
                        classes = todayClasses,
                        customEvents = todayCustomEvents,
                        dueTodayTasks = dueTodayTasks
                    )

                    // Self-healing morning safety net via centralized coordinator
                    if (!isTest) {
                        AlarmSyncCoordinator.resyncAll(
                            context = context,
                            repository = repository,
                            force = false,
                            preserveImminentExisting = true,
                            reason = "daily_digest_safety_net"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating daily schedule digest: ${e.message}", e)
            } finally {
                receiverScope.cancel()
                pendingResult?.finish()
            }
        }
    }

    private fun matchesDay(candidate: String, targetDay: String): Boolean {
        val clean = candidate.trim()
        return clean.equals(targetDay, ignoreCase = true) ||
            clean.startsWith(targetDay.take(3), ignoreCase = true)
    }
}
