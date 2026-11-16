package com.lectures.core.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lectures.TimetableApplication
import com.lectures.core.notification.NotificationHelper
import com.lectures.features.timetable.data.ETagStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TRIGGER_TASK_REMINDER = "com.lectures.ACTION_TRIGGER_TASK_REMINDER"
        const val EXTRA_TASK_ID = "EXTRA_TASK_ID"
        const val EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE"
        const val EXTRA_TASK_COURSE = "EXTRA_TASK_COURSE"
        const val EXTRA_TASK_DUE = "EXTRA_TASK_DUE"
        const val EXTRA_TASK_PRIORITY = "EXTRA_TASK_PRIORITY"

        private const val TAG = "TaskReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_TASK_TITLE) ?: "Academic Task"
        val course = intent.getStringExtra(EXTRA_TASK_COURSE) ?: "Coursework"
        val due = intent.getStringExtra(EXTRA_TASK_DUE) ?: "Due Soon"
        val priority = intent.getStringExtra(EXTRA_TASK_PRIORITY) ?: "Medium"
        val isTest = intent.getBooleanExtra("IS_TEST", false)

        val app = context.applicationContext as? TimetableApplication
        val etagStore = app?.etagStore ?: ETagStore(context)
        if (!etagStore.isNotificationsEnabled() && !isTest) {
            Log.d(TAG, "Notifications disabled in Quiet Mode; suppressing task reminder for Task #$taskId")
            return
        }

        if (taskId == -1L) return

        // Test alerts display immediately without requiring a persisted Room row
        if (isTest || app == null) {
            NotificationHelper.showTaskReminderNotification(
                context = context,
                taskId = taskId,
                title = title,
                courseCode = course,
                dueDate = due,
                priority = priority
            )
            return
        }

        val pendingResult = goAsync()
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        receiverScope.launch {
            try {
                withTimeout(5_000L) {
                    val latestTask = app.repository.getAssignmentByIdSync(taskId)
                    if (latestTask == null || latestTask.isDeleted || latestTask.completed || latestTask.reminderMinutes == null) {
                        Log.d(TAG, "Suppressing stale/completed/deleted reminder for Task #$taskId")
                        TaskAlarmScheduler(context).cancelTaskReminder(taskId)
                        return@withTimeout
                    }
                    Log.d(TAG, "AlarmManager triggered reminder for Task #$taskId: ${latestTask.title} (${latestTask.courseCode})")
                    NotificationHelper.showTaskReminderNotification(
                        context = context,
                        taskId = taskId,
                        title = latestTask.title,
                        courseCode = latestTask.courseCode.ifBlank { course },
                        dueDate = latestTask.dueDate.ifBlank { due },
                        priority = latestTask.priority.ifBlank { priority }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed validating task reminder #$taskId: ${e.message}", e)
            } finally {
                receiverScope.cancel()
                pendingResult?.finish()
            }
        }
    }
}
