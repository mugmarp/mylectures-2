package com.mustime.core.notification

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
enum class NotificationItemType {
    CLASS_ALARM,
    CLASS_REMINDER,
    TASK_REMINDER,
    DAILY_DIGEST,
    SYSTEM
}

@Serializable
data class InAppNotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val type: NotificationItemType,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val relatedKey: String? = null,
    val navTab: Int? = null
)

class InAppNotificationStore(private val context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("mustime_inapp_notifications", Context.MODE_PRIVATE)

    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableStateFlow<List<InAppNotificationItem>>(loadSavedNotifications())
    val notifications = _notifications.asStateFlow()

    val unreadCount: Flow<Int> = _notifications.map { list -> list.count { !it.isRead } }

    private fun loadSavedNotifications(): List<InAppNotificationItem> {
        val raw = prefs.getString(KEY_NOTIFICATIONS, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<InAppNotificationItem>>(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persist(list: List<InAppNotificationItem>) {
        val limited = list.take(100) // Keep latest 100 entries
        _notifications.value = limited
        try {
            val encoded = json.encodeToString(limited)
            prefs.edit().putString(KEY_NOTIFICATIONS, encoded).apply()
        } catch (_: Exception) {}
    }

    fun recordNotification(
        type: NotificationItemType,
        title: String,
        message: String,
        relatedKey: String? = null,
        navTab: Int? = null
    ): InAppNotificationItem {
        val item = InAppNotificationItem(
            type = type,
            title = title,
            message = message,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            relatedKey = relatedKey,
            navTab = navTab
        )
        val current = _notifications.value.toMutableList()
        current.add(0, item)
        persist(current)

        // Also emit for real-time in-app foreground banner
        _foregroundAlertFlow.tryEmit(item)
        return item
    }

    fun markAsRead(id: String) {
        val updated = _notifications.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        persist(updated)
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        persist(updated)
    }

    fun deleteNotification(id: String) {
        val updated = _notifications.value.filter { it.id != id }
        persist(updated)
    }

    fun clearAll() {
        persist(emptyList())
    }

    companion object {
        private const val KEY_NOTIFICATIONS = "notifications_json"

        // Shared across instances for instant in-app heads-up alerts
        private val _foregroundAlertFlow = MutableSharedFlow<InAppNotificationItem>(
            replay = 0,
            extraBufferCapacity = 5
        )
        val foregroundAlertFlow = _foregroundAlertFlow.asSharedFlow()
    }
}
