package com.lectures.features.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lectures.BuildConfig
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.AlarmSyncCoordinator
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.auth.AuthRepository
import com.lectures.core.notification.NotificationHelper
import com.lectures.core.storage.AttachmentStore
import com.lectures.core.sync.SyncMergeStats
import com.lectures.core.sync.SyncState
import com.lectures.core.update.AppUpdateInfo
import com.lectures.core.update.InAppUpdateManager
import com.lectures.core.util.NotificationPermissionHelper
import com.lectures.features.timetable.data.DataLoader
import com.lectures.features.timetable.data.ResetSyncStatus
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.TimetableMetadata
import com.lectures.ui.ThemeMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SettingsUiState(
    val currentProgramme: String = "",
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val classAlarmLeadMinutes: Int = 30,
    val taskReminderLeadHours: Int = 2,
    val alarmVibration: Boolean = true,
    val dailyDigestEnabled: Boolean = true,
    val selectedAccent: Int = 0,
    val lastSyncTime: String = "Up to date",
    val timetableVersion: String = "Final Timetable",
    val timetableStatus: String = "Official Final",
    val isFinal: Boolean = true,
    val metadata: TimetableMetadata? = null,
    val isSyncing: Boolean = false,
    val isResetting: Boolean = false,
    val isCheckingForAppUpdate: Boolean = false,
    val availableUpdateInfo: AppUpdateInfo? = null,
    val showAppUpdateDialog: Boolean = false,
    val userNotesCount: Int = 0,
    val userEventsCount: Int = 0,
    val userAssignmentsCount: Int = 0,
    val isBackingUpUserData: Boolean = false,
    val userDataBackupStatus: String = "Local storage active (Guest mode)",
    val userAccountEmail: String? = null,
    val isCloudSyncActive: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val repository: TimetableRepository,
    private val dataLoader: DataLoader? = null,
    private val alarmScheduler: TaskAlarmScheduler? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val application: Application,
    private val classAlarmScheduler: AlarmScheduler? = null
) : AndroidViewModel(application) {

    private val initialProg = repository.getInitialProgramme() ?: ""
    private val _uiState = MutableStateFlow(SettingsUiState(currentProgramme = initialProg))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.programmePref.collect { pref ->
                if (!pref.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(currentProgramme = pref)
                }
            }
        }

        viewModelScope.launch {
            repository.themeModePref.collect { modeStr ->
                val mode = try {
                    ThemeMode.valueOf(modeStr.uppercase(Locale.ROOT))
                } catch (_: Exception) {
                    ThemeMode.LIGHT
                }
                _uiState.value = _uiState.value.copy(
                    themeMode = mode,
                    darkMode = mode == ThemeMode.DARK
                )
            }
        }

        viewModelScope.launch {
            repository.themeDarkPref.collect { dark ->
                if (_uiState.value.themeMode != ThemeMode.SYSTEM) {
                    _uiState.value = _uiState.value.copy(darkMode = dark)
                }
            }
        }

        viewModelScope.launch {
            repository.themeAccentPref.collect { accent ->
                _uiState.value = _uiState.value.copy(selectedAccent = accent)
            }
        }

        viewModelScope.launch {
            repository.notificationsEnabledPref.collect { enabled ->
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)
            }
        }

        viewModelScope.launch {
            repository.classAlarmLeadPref.collect { lead ->
                _uiState.value = _uiState.value.copy(classAlarmLeadMinutes = lead)
            }
        }

        viewModelScope.launch {
            repository.taskReminderLeadPref.collect { lead ->
                _uiState.value = _uiState.value.copy(taskReminderLeadHours = lead)
            }
        }

        viewModelScope.launch {
            repository.alarmVibrationPref.collect { vib ->
                _uiState.value = _uiState.value.copy(alarmVibration = vib)
            }
        }

        viewModelScope.launch {
            repository.dailyDigestEnabledPref.collect { digest ->
                _uiState.value = _uiState.value.copy(dailyDigestEnabled = digest)
            }
        }

        viewModelScope.launch {
            repository.lastSyncPref.collect { syncTime ->
                if (!syncTime.isNullOrBlank()) {
                    _uiState.value = _uiState.value.copy(lastSyncTime = syncTime)
                }
            }
        }

        viewModelScope.launch {
            repository.timetableMetadata.collect { meta ->
                if (meta != null) {
                    _uiState.value = _uiState.value.copy(
                        timetableVersion = meta.versionLabel,
                        timetableStatus = meta.status,
                        isFinal = meta.isFinal,
                        metadata = meta
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        timetableVersion = repository.getTimetableVersion(),
                        timetableStatus = repository.getTimetableStatus()
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(userNotesCount = notes.size)
            }
        }
        viewModelScope.launch {
            repository.getCustomEvents().collect { events ->
                _uiState.value = _uiState.value.copy(userEventsCount = events.size)
            }
        }
        viewModelScope.launch {
            repository.getAssignments().collect { assignments ->
                _uiState.value = _uiState.value.copy(userAssignmentsCount = assignments.size)
            }
        }

        viewModelScope.launch {
            AuthRepository.getInstance().currentUser.collect { user ->
                if (user != null) {
                    _uiState.value = _uiState.value.copy(
                        userAccountEmail = user.email ?: user.displayName ?: user.uid,
                        isCloudSyncActive = true,
                        userDataBackupStatus = "Signed in as ${user.displayName ?: user.email ?: "Student"}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        userAccountEmail = null,
                        isCloudSyncActive = false,
                        userDataBackupStatus = "Guest mode · Local storage active"
                    )
                }
            }
        }
    }

    fun syncUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUpUserData = true, errorMessage = null, successMessage = null)
            try {
                val currentUid = try {
                    AuthRepository.getInstance().currentUser.value?.uid
                } catch (_: Throwable) { null }

                val stats = withContext(ioDispatcher) {
                    if (currentUid != null) {
                        repository.performFullUserSync(currentUid)
                    } else {
                        val notes = repository.dao.getAllNotesSync()
                        val events = repository.getCustomEvents().firstOrNull() ?: emptyList()
                        val assignments = repository.getAssignments().firstOrNull() ?: emptyList()
                        SyncMergeStats(
                            notesCount = notes.size,
                            eventsCount = events.size,
                            assignmentsCount = assignments.size
                        )
                    }
                }

                val format = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
                val timeString = format.format(Date())

                val syncFailed = currentUid != null && (
                    stats == null ||
                        stats.status == SyncState.ERROR ||
                        stats.failedCount > 0
                    )

                _uiState.value = if (syncFailed) {
                    _uiState.value.copy(
                        isBackingUpUserData = false,
                        userDataBackupStatus = "Sync issue at $timeString",
                        errorMessage = stats?.errorMessage ?: "Cloud sync could not be completed. Local data remains safe."
                    )
                } else {
                    _uiState.value.copy(
                        isBackingUpUserData = false,
                        userDataBackupStatus = if (currentUid != null) "Synced with Cloud at $timeString" else "Preserved locally · Verified at $timeString",
                        successMessage = if (stats != null && stats.pushedCount > 0) {
                            "Cloud sync completed: ${stats.pushedCount} records synced, ${stats.conflictsResolvedCount} conflicts safely resolved."
                        } else {
                            "Data secure: ${stats?.notesCount ?: 0} notes, ${stats?.eventsCount ?: 0} events, ${stats?.assignmentsCount ?: 0} coursework tasks ready to sync."
                        }
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isBackingUpUserData = false,
                    errorMessage = "User sync error: ${e.localizedMessage ?: "Unknown error"}"
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            try {
                repository.setThemeMode(mode.name)
                _uiState.value = _uiState.value.copy(
                    themeMode = mode,
                    darkMode = mode == ThemeMode.DARK
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update appearance: ${e.message}")
            }
        }
    }

    fun setDarkMode(dark: Boolean) {
        viewModelScope.launch {
            try {
                repository.setThemeDark(dark)
                _uiState.value = _uiState.value.copy(
                    darkMode = dark,
                    themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update appearance: ${e.message}")
            }
        }
    }

    fun setSelectedAccent(accent: Int) {
        viewModelScope.launch {
            try {
                repository.setThemeAccent(accent)
                _uiState.value = _uiState.value.copy(selectedAccent = accent)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update accent color: ${e.message}")
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setNotificationsEnabled(enabled)
                _uiState.value = _uiState.value.copy(notificationsEnabled = enabled)

                val summary = AlarmSyncCoordinator.resyncAll(
                    context = application,
                    repository = repository,
                    force = true,
                    dispatcher = ioDispatcher,
                    reason = "settings_toggle_$enabled"
                )

                _uiState.value = _uiState.value.copy(
                    successMessage = if (summary.notificationsEnabled) {
                        val eventPart = if (summary.customEventCount > 0) ", ${summary.customEventCount} activity alarms" else ""
                        val digestPart = if (_uiState.value.dailyDigestEnabled) ", and daily digest enabled" else ""
                        "Reminders active. ${summary.enrolledClassCount} class reminders, ${summary.taskCount} task alerts, ${summary.customNoteCount} custom alarms$eventPart$digestPart."
                    } else {
                        "All alarms and reminders paused (quiet mode)."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update notification settings: ${e.message}")
            }
        }
    }

    fun setClassAlarmLeadMinutes(minutes: Int) {
        viewModelScope.launch {
            try {
                repository.setClassAlarmLeadMinutes(minutes)
                val summary = AlarmSyncCoordinator.resyncAll(
                    context = application,
                    repository = repository,
                    force = true,
                    preserveImminentExisting = false,
                    dispatcher = ioDispatcher,
                    reason = "settings_class_lead_$minutes"
                )
                val reArmedCount = summary.enrolledClassCount
                _uiState.value = _uiState.value.copy(
                    classAlarmLeadMinutes = minutes,
                    successMessage = when {
                        !summary.notificationsEnabled -> "Default class reminder set to ${minutes}m before (Quiet Mode active — alerts paused)."
                        reArmedCount > 0 -> "Lecture reminders updated to $minutes minutes before ($reArmedCount classes armed)."
                        else -> "Lecture reminder updated to $minutes minutes before."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update class reminder: ${e.message}")
            }
        }
    }

    fun setTaskReminderLeadHours(hours: Int) {
        viewModelScope.launch {
            try {
                repository.setTaskReminderLeadHours(hours)
                val label = if (hours >= 24) "${hours / 24} day before" else "${hours}h before"
                _uiState.value = _uiState.value.copy(
                    taskReminderLeadHours = hours,
                    successMessage = if (!_uiState.value.notificationsEnabled) {
                        "Default task reminder set to $label (Quiet Mode active — alerts paused)."
                    } else {
                        "Default task reminder set to $label for new tasks."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update task reminder: ${e.message}")
            }
        }
    }

    fun setAlarmVibration(vibrate: Boolean) {
        viewModelScope.launch {
            try {
                repository.setAlarmVibration(vibrate)
                _uiState.value = _uiState.value.copy(
                    alarmVibration = vibrate,
                    successMessage = if (vibrate) {
                        "Alarm vibration enabled for app alerts."
                    } else {
                        "Alarm vibration turned off for app alerts."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update vibration setting: ${e.message}")
            }
        }
    }

    fun setDailyDigestEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.setDailyDigestEnabled(enabled)
                _uiState.value = _uiState.value.copy(dailyDigestEnabled = enabled)
                val summary = AlarmSyncCoordinator.resyncAll(
                    context = application,
                    repository = repository,
                    force = true,
                    preserveImminentExisting = true,
                    dispatcher = ioDispatcher,
                    reason = "settings_daily_digest_$enabled"
                )
                _uiState.value = _uiState.value.copy(
                    successMessage = when {
                        !summary.notificationsEnabled -> "Morning digest preference saved (Quiet Mode active)."
                        enabled -> "Morning schedule digest active (7:30 AM daily)."
                        else -> "Morning schedule digest turned off."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to update morning digest: ${e.message}")
            }
        }
    }

    fun sendTestNotification(context: Context) {
        try {
            if (!NotificationPermissionHelper.areNotificationsEnabled(context)) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Notifications are disabled in Android Settings. Enable permissions first."
                )
                return
            }
            if (NotificationPermissionHelper.isChannelBlocked(context, NotificationHelper.CHANNEL_CLASSES)) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "The 'Upcoming Class Reminders' channel is muted in OS Channels."
                )
                return
            }
            val posted = NotificationHelper.showClassReminderNotification(
                context = context,
                courseCode = "BCS2101",
                courseTitle = "Data Structures & Algorithms",
                room = "Comp Lab 2 (Main Campus)",
                startTime = "09:00 AM",
                minutesBefore = _uiState.value.classAlarmLeadMinutes,
                lecturer = "Dr. Mugisha",
                naturalKey = "test_class_reminder"
            )
            if (posted) {
                _uiState.value = _uiState.value.copy(
                    successMessage = if (_uiState.value.notificationsEnabled) {
                        "Test class reminder posted! Check your status bar."
                    } else {
                        "Test reminder posted! (Quiet Mode active — scheduled alarms remain paused.)"
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Test notification was blocked by Android system settings."
                )
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Failed to dispatch test notification: ${e.message}"
            )
        }
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            try {
                val summary = AlarmSyncCoordinator.resyncAll(
                    context = application,
                    repository = repository,
                    force = true,
                    dispatcher = ioDispatcher,
                    reason = "settings_rearm_all"
                )

                _uiState.value = _uiState.value.copy(
                    successMessage = if (!summary.notificationsEnabled) {
                        "Quiet Mode is active — enable 'Class & Task Reminders' first to arm alarms."
                    } else {
                        val eventPart = if (summary.customEventCount > 0) ", ${summary.customEventCount} activity alarms" else ""
                        "Rescheduled ${summary.enrolledClassCount} class reminders, ${summary.taskCount} task alerts, ${summary.customNoteCount} custom alarms$eventPart, and morning digest."
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to reschedule alarms: ${e.message}")
            }
        }
    }

    fun checkForAppUpdate() {
        if (_uiState.value.isCheckingForAppUpdate) return
        _uiState.value = _uiState.value.copy(isCheckingForAppUpdate = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            val result = InAppUpdateManager.checkForUpdate(getApplication(), forceRefresh = true)
            result.fold(
                onSuccess = { update ->
                    if (update != null) {
                        _uiState.value = _uiState.value.copy(
                            isCheckingForAppUpdate = false,
                            availableUpdateInfo = update,
                            showAppUpdateDialog = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isCheckingForAppUpdate = false,
                            availableUpdateInfo = null,
                            successMessage = "Lectures is up to date (v${BuildConfig.VERSION_NAME})."
                        )
                    }
                },
                onFailure = { error ->
                    val msg = if (error is UnknownHostException || error is SocketTimeoutException) {
                        "Unable to check for updates. Please check your internet connection."
                    } else {
                        "Check for updates failed: ${error.localizedMessage ?: "Network error"}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isCheckingForAppUpdate = false,
                        errorMessage = msg
                    )
                }
            )
        }
    }

    fun openUpdateDialog() {
        if (_uiState.value.availableUpdateInfo != null) {
            _uiState.value = _uiState.value.copy(showAppUpdateDialog = true)
        }
    }

    fun dismissUpdateDialog() {
        _uiState.value = _uiState.value.copy(showAppUpdateDialog = false)
    }

    fun exportUserDataBackup(targetUri: Uri) {
        viewModelScope.launch {
            try {
                val (notesCount, eventsCount, tasksCount) = withContext(ioDispatcher) {
                    val notes = repository.dao.getAllNotesSync()
                    val events = repository.getCustomEvents().firstOrNull() ?: emptyList()
                    val tasks = repository.getAssignments().firstOrNull() ?: emptyList()
                    val jsonPayload = SettingsBackupExporter.buildBackupJson(
                        programme = _uiState.value.currentProgramme,
                        notes = notes,
                        events = events,
                        tasks = tasks
                    )
                    getApplication<Application>().contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Could not open destination file")

                    Triple(notes.size, events.size, tasks.size)
                }

                _uiState.value = _uiState.value.copy(
                    successMessage = "Backup exported ($notesCount notes, $eventsCount events, $tasksCount tasks)."
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Backup export failed: ${e.localizedMessage ?: "Storage error"}"
                )
            }
        }
    }

    fun importUserDataBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUpUserData = true, errorMessage = null, successMessage = null)
            try {
                val rawJson = withContext(ioDispatcher) {
                    val stream = getApplication<Application>().contentResolver.openInputStream(sourceUri)
                        ?: throw IllegalStateException("Could not open selected backup file")
                    stream.use { input ->
                        val maxBytes = 10 * 1024 * 1024 // 10 MB safety cap
                        val buffer = ByteArray(8192)
                        val out = java.io.ByteArrayOutputStream()
                        var total = 0
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            total += read
                            if (total > maxBytes) {
                                throw IllegalArgumentException("Backup file exceeds the 10 MB size limit.")
                            }
                            out.write(buffer, 0, read)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }
                }
                restoreBackupFromJsonInternal(rawJson)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isBackingUpUserData = false,
                    errorMessage = "Backup import failed: ${e.localizedMessage ?: "Invalid file"}"
                )
            }
        }
    }

    internal suspend fun importUserDataFromJson(rawJson: String): Result<Triple<Int, Int, Int>> {
        _uiState.value = _uiState.value.copy(isBackingUpUserData = true, errorMessage = null, successMessage = null)
        return restoreBackupFromJsonInternal(rawJson)
    }

    private suspend fun restoreBackupFromJsonInternal(rawJson: String): Result<Triple<Int, Int, Int>> {
        return try {
            val parsed = SettingsBackupExporter.parseBackupJson(rawJson).getOrElse { err ->
                _uiState.value = _uiState.value.copy(
                    isBackingUpUserData = false,
                    errorMessage = err.message ?: "Backup import failed."
                )
                return Result.failure(err)
            }

            if (parsed.notes.isEmpty() && parsed.events.isEmpty() && parsed.assignments.isEmpty()) {
                val emptyErr = IllegalArgumentException("The selected backup file contains no notes, tasks, or custom events.")
                _uiState.value = _uiState.value.copy(
                    isBackingUpUserData = false,
                    errorMessage = emptyErr.message
                )
                return Result.failure(emptyErr)
            }

            val counts = withContext(ioDispatcher) {
                repository.importBackupData(
                    notes = parsed.notes,
                    events = parsed.events,
                    assignments = parsed.assignments
                )
            }

            AlarmSyncCoordinator.resyncAll(
                context = getApplication(),
                repository = repository,
                force = true,
                preserveImminentExisting = true,
                dispatcher = ioDispatcher,
                reason = "settings_import_backup"
            )

            val currentUid = runCatching {
                AuthRepository.getInstance().currentUser.value?.uid
            }.getOrNull()?.takeIf { it.isNotBlank() }

            val cloudSyncStatus = if (currentUid != null) {
                withContext(ioDispatcher) { repository.performFullUserSync(currentUid) }
            } else {
                null
            }

            val (notesCount, eventsCount, tasksCount) = counts
            val cloudSuffix = when {
                currentUid == null -> ""
                cloudSyncStatus?.status == SyncState.ERROR -> " Saved locally; cloud sync queued."
                else -> " Synced to Cloud."
            }
            _uiState.value = _uiState.value.copy(
                isBackingUpUserData = false,
                successMessage = "Backup restored ($notesCount notes, $eventsCount events, $tasksCount tasks).$cloudSuffix"
            )
            Result.success(counts)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            _uiState.value = _uiState.value.copy(
                isBackingUpUserData = false,
                errorMessage = "Backup import failed: ${e.localizedMessage ?: "Unexpected error"}"
            )
            Result.failure(e)
        }
    }

    fun syncTimetable() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, errorMessage = null, successMessage = null)
            try {
                var remoteSynced = false
                var assetFallbackLoaded = false
                var totalEntries = 0
                withContext(ioDispatcher) {
                    val existingCount = repository.dao.getEntryCount()
                    val remoteMeta = repository.syncRepository?.fetchRemoteTimetableMetadata()
                    if (remoteMeta != null) {
                        val syncedEntries = repository.syncRepository.syncRemoteTimetableEntries()
                        val resolvedMeta = if (remoteMeta.totalSessions > 0) {
                            remoteMeta.copy(lastUpdated = System.currentTimeMillis())
                        } else if (syncedEntries > 0) {
                            remoteMeta.copy(lastUpdated = System.currentTimeMillis(), totalSessions = syncedEntries)
                        } else {
                            remoteMeta.copy(lastUpdated = System.currentTimeMillis())
                        }
                        repository.updateTimetableMetadata(resolvedMeta)
                        if (syncedEntries > 0) {
                            AlarmSyncCoordinator.resyncAll(
                                context = getApplication(),
                                repository = repository,
                                force = true,
                                preserveImminentExisting = true,
                                dispatcher = ioDispatcher,
                                reason = "manual_settings_sync"
                            )
                        }
                        remoteSynced = true
                    } else if (dataLoader != null && (repository.syncRepository == null || existingCount == 0)) {
                        val entries = dataLoader.loadFromAssets()
                        if (entries.isNotEmpty()) {
                            dataLoader.replaceTimetableEntries(entries)
                        }
                        val meta = dataLoader.loadMetadataFromAssets()
                        if (meta != null) {
                            repository.updateTimetableMetadata(meta.copy(lastUpdated = System.currentTimeMillis()))
                        }
                        assetFallbackLoaded = true
                    }
                    totalEntries = repository.dao.getEntryCount()
                }
                val format = SimpleDateFormat("h:mm a, MMM d", Locale.getDefault())
                val timeString = "Synced at ${format.format(Date())}"
                repository.setLastSyncTime(timeString)
                val syncStatus = when {
                    remoteSynced -> "Cloud release verified · $totalEntries lectures"
                    assetFallbackLoaded -> "$totalEntries lectures loaded"
                    else -> "Offline mode · $totalEntries cached lectures preserved"
                }
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    lastSyncTime = timeString,
                    successMessage = "Timetable synchronized ($syncStatus)"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    errorMessage = "Synchronization failed: ${e.localizedMessage ?: "Unknown network/database error"}"
                )
            }
        }
    }

    fun resetAllNotes(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResetting = true, errorMessage = null)
            try {
                val syncStatus = withContext(ioDispatcher) {
                    val notesWithAlarms = repository.getNotesWithAlarms()
                    notesWithAlarms.forEach { note ->
                        classAlarmScheduler?.cancelClassAlarm(note.naturalKey)
                    }
                    AttachmentStore.deleteAll(getApplication())
                    repository.resetAllNotesCloudAware()
                }
                AlarmSyncCoordinator.resyncAll(
                    context = getApplication(),
                    repository = repository,
                    force = true,
                    preserveImminentExisting = true,
                    dispatcher = ioDispatcher,
                    reason = "settings_reset_notes"
                )
                val msg = when (syncStatus) {
                    ResetSyncStatus.CLOUD_SYNCED -> "All lecture notes, attachments, and note alerts reset locally and in Cloud Sync."
                    ResetSyncStatus.QUEUED_OFFLINE -> "All lecture notes and attachments cleared locally. Cloud reset queued until next sync."
                    ResetSyncStatus.LOCAL_ONLY -> "All lecture notes, attachments, and note alerts have been reset."
                }
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    successMessage = msg
                )
                onComplete()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    errorMessage = "Failed to reset notes: ${e.message}"
                )
            }
        }
    }

    fun resetAllTasks(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResetting = true, errorMessage = null)
            try {
                val syncStatus = withContext(ioDispatcher) {
                    val tasks = repository.getAssignments().firstOrNull() ?: emptyList()
                    alarmScheduler?.cancelAllTaskReminders(tasks)
                    repository.resetAllAssignmentsCloudAware()
                }
                AlarmSyncCoordinator.resyncAll(
                    context = getApplication(),
                    repository = repository,
                    force = true,
                    preserveImminentExisting = true,
                    dispatcher = ioDispatcher,
                    reason = "settings_reset_tasks"
                )
                val msg = when (syncStatus) {
                    ResetSyncStatus.CLOUD_SYNCED -> "All academic tasks and alarms cleared locally and in Cloud Sync."
                    ResetSyncStatus.QUEUED_OFFLINE -> "All academic tasks and alarms cleared locally. Cloud reset queued until next sync."
                    ResetSyncStatus.LOCAL_ONLY -> "All academic tasks and alarms have been cleared."
                }
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    successMessage = msg
                )
                onComplete()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    errorMessage = "Failed to reset tasks: ${e.message}"
                )
            }
        }
    }

    fun resetAllCustomEvents(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResetting = true, errorMessage = null)
            try {
                val syncStatus = withContext(ioDispatcher) {
                    val events = repository.getCustomEvents().firstOrNull() ?: emptyList()
                    classAlarmScheduler?.cancelAllCustomEventAlarms(events)
                    repository.resetAllCustomEventsCloudAware()
                }
                AlarmSyncCoordinator.resyncAll(
                    context = getApplication(),
                    repository = repository,
                    force = true,
                    preserveImminentExisting = true,
                    dispatcher = ioDispatcher,
                    reason = "settings_reset_events"
                )
                val msg = when (syncStatus) {
                    ResetSyncStatus.CLOUD_SYNCED -> "All personal activities and event alarms cleared locally and in Cloud Sync."
                    ResetSyncStatus.QUEUED_OFFLINE -> "All personal activities and event alarms cleared locally. Cloud reset queued until next sync."
                    ResetSyncStatus.LOCAL_ONLY -> "All personal activities and event alarms have been cleared."
                }
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    successMessage = msg
                )
                onComplete()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.value = _uiState.value.copy(
                    isResetting = false,
                    errorMessage = "Failed to reset custom events: ${e.message}"
                )
            }
        }
    }

    fun clearFeedbackMessages() {
        _uiState.value = _uiState.value.copy(successMessage = null, errorMessage = null)
    }

    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            dataLoader: DataLoader? = null,
            alarmScheduler: TaskAlarmScheduler? = null,
            classAlarmScheduler: AlarmScheduler? = null,
            ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
            application: Application
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(
                        repository = repository,
                        dataLoader = dataLoader,
                        alarmScheduler = alarmScheduler,
                        ioDispatcher = ioDispatcher,
                        application = application,
                        classAlarmScheduler = classAlarmScheduler
                    ) as T
                }
            }
    }
}
