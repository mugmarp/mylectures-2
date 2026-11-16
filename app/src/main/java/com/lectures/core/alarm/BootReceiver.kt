package com.lectures.core.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.lectures.TimetableApplication
import com.lectures.core.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"

        private val BOOT_OR_PACKAGE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON
        )

        private val CLOCK_OR_PERMISSION_ACTIONS = setOf(
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isBootOrPackage = action in BOOT_OR_PACKAGE_ACTIONS
        val isClockOrPermission = action in CLOCK_OR_PERMISSION_ACTIONS
        if (!isBootOrPackage && !isClockOrPermission) {
            return
        }

        Log.d(TAG, "System broadcast '$action' detected. Reconciling alarms.")

        val app = context.applicationContext as? TimetableApplication ?: return
        if (isBootOrPackage) {
            NotificationHelper.createNotificationChannels(context)
        }

        val repository = app.repository
        val pendingResult = goAsync()
        val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        receiverScope.launch {
            try {
                withTimeout(8_500L) {
                    AlarmSyncCoordinator.resyncAll(
                        context = context,
                        repository = repository,
                        force = isClockOrPermission,
                        reason = "broadcast_$action"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms on '$action': ${e.message}", e)
            } finally {
                receiverScope.cancel()
                pendingResult?.finish()
            }
        }
    }
}
