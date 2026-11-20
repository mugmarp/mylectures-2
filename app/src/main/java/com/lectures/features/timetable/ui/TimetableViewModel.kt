package com.lectures.features.timetable.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lectures.core.alarm.AlarmScheduler
import com.lectures.core.alarm.TaskAlarmScheduler
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.data.TimetableRepository
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TimetableUiState(
    val isLoading: Boolean = true,
    val selectedDay: String = TimeUtil.todayName(),
    val entries: List<TimetableEntry> = emptyList(),
    val customEvents: List<CustomEvent> = emptyList(),
    val assignments: List<Assignment> = emptyList(),
    val notes: Map<String, LectureNote> = emptyMap(),
    val enrolledProgramme: String = "",
    val viewingProgramme: String = "",
    val isPeeking: Boolean = false
) {
    val programme: String get() = if (viewingProgramme.isNotBlank()) viewingProgramme else enrolledProgramme
}

class TimetableViewModel(
    private val repository: TimetableRepository,
    private val alarmScheduler: TaskAlarmScheduler? = null,
    private val classAlarmScheduler: AlarmScheduler? = null,
    private val appContext: Context? = null
) : ViewModel() {

    private val initialProg = repository.getInitialProgramme()?.trim() ?: ""
    private val _uiState = MutableStateFlow(
        TimetableUiState(
            isLoading = initialProg.isNotEmpty(),
            enrolledProgramme = initialProg,
            viewingProgramme = initialProg,
            isPeeking = false
        )
    )
    val uiState: StateFlow<TimetableUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshTimetable() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val progToRefresh = _uiState.value.programme
                repository.refreshTimetable(progToRefresh)
                if (progToRefresh.isNotEmpty()) {
                    loadSchedule(progToRefresh)
                }
                kotlinx.coroutines.delay(500)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private var scheduleJob: Job? = null

    @Volatile
    private var lastArmedScheduleSignature: String? = null

    init {
        viewModelScope.launch {
            repository.programmePref.collect { pref ->
                val newEnrolled = pref?.trim() ?: ""
                val current = _uiState.value
                val wasPeeking = current.isPeeking
                
                if (newEnrolled != current.enrolledProgramme) {
                    if (!wasPeeking) {
                        _uiState.value = current.copy(
                            enrolledProgramme = newEnrolled,
                            viewingProgramme = newEnrolled,
                            isPeeking = false
                        )
                        if (newEnrolled.isNotEmpty()) {
                            loadSchedule(newEnrolled)
                        } else {
                            _uiState.value = _uiState.value.copy(entries = emptyList(), isLoading = false)
                        }
                    } else {
                        // User was peeking a friend's timetable; keep viewing friend's schedule but update enrolled anchor
                        _uiState.value = current.copy(enrolledProgramme = newEnrolled)
                    }
                } else if (_uiState.value.entries.isEmpty() && scheduleJob == null) {
                    val prog = current.programme
                    if (prog.isNotEmpty()) {
                        loadSchedule(prog)
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
        }
        viewModelScope.launch {
            repository.getAllNotes().collect { noteList ->
                _uiState.value = _uiState.value.copy(notes = noteList.associateBy { it.naturalKey })
            }
        }
        viewModelScope.launch {
            repository.getCustomEvents().collect { events ->
                _uiState.value = _uiState.value.copy(customEvents = events)
            }
        }
        viewModelScope.launch {
            repository.getAssignments().collect { assignmentList ->
                _uiState.value = _uiState.value.copy(assignments = assignmentList)
            }
        }
    }

    private fun loadSchedule(programme: String) {
        val progToLoad = programme.trim()
        if (progToLoad.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                entries = emptyList(),
                isLoading = false
            )
            return
        }
        scheduleJob?.cancel()
        scheduleJob = viewModelScope.launch {
            repository.getLocalSchedule(progToLoad).collect { entries ->
                _uiState.value = _uiState.value.copy(
                    entries = entries,
                    isLoading = false
                )
                // Reconcile enrolled class reminders off the main thread only when the active schedule,
                // lead preference, or note overrides actually change (avoids main-thread AlarmManager IPC churn).
                if (!_uiState.value.isPeeking && entries.isNotEmpty() && (appContext != null || classAlarmScheduler != null)) {
                    withContext(Dispatchers.IO) {
                        if (!repository.etagStore.isNotificationsEnabled()) return@withContext
                        val leadMinutes = repository.etagStore.getClassAlarmLeadMinutes()
                        val customNoteMap = repository.getNotesWithAlarms()
                            .mapNotNull { note -> note.alarmMinutes?.let { note.naturalKey to it } }
                            .toMap()
                        val signature = buildString {
                            append(progToLoad).append('|').append(leadMinutes).append('|')
                            entries.forEach {
                                append(it.naturalKey).append('@').append(it.dayOfWeek).append('@').append(it.startTime).append(';')
                            }
                            customNoteMap.entries.sortedBy { it.key }.forEach {
                                append(it.key).append('=').append(it.value).append(';')
                            }
                        }
                        if (signature != lastArmedScheduleSignature) {
                            if (appContext != null) {
                                com.lectures.core.alarm.AlarmSyncCoordinator.resyncAll(
                                    context = appContext,
                                    repository = repository,
                                    force = false,
                                    preserveImminentExisting = true,
                                    reason = "timetable_schedule_loaded"
                                )
                            } else {
                                classAlarmScheduler?.scheduleAllEnrolledClassAlarms(
                                    classes = entries,
                                    leadMinutes = leadMinutes,
                                    isAlarm = false,
                                    customAlarmNotes = customNoteMap,
                                    preserveImminentExisting = true
                                )
                            }
                            lastArmedScheduleSignature = signature
                        }
                    }
                }
            }
        }
    }

    fun selectDay(day: String) {
        _uiState.value = _uiState.value.copy(selectedDay = day)
    }

    /**
     * Peeks another cohort/friend's schedule transiently without touching the user's permanent
     * enrolled identity, background class alarms, or notification scheduling.
     */
    fun peekProgramme(targetCohort: String) {
        val cleanTarget = targetCohort.trim()
        val enrolled = _uiState.value.enrolledProgramme.trim()
        if (cleanTarget.isEmpty()) return
        if (cleanTarget.equals(enrolled, ignoreCase = true)) {
            resetToEnrolledProgramme()
            return
        }
        _uiState.value = _uiState.value.copy(
            viewingProgramme = cleanTarget,
            isPeeking = true,
            isLoading = true
        )
        loadSchedule(cleanTarget)
        _userMessage.value = "Peeking $cleanTarget timetable"
    }

    /**
     * Returns the user cleanly back to their enrolled academic timetable.
     */
    fun resetToEnrolledProgramme() {
        val enrolled = _uiState.value.enrolledProgramme.trim()
        _uiState.value = _uiState.value.copy(
            viewingProgramme = enrolled,
            isPeeking = false,
            isLoading = true
        )
        if (enrolled.isNotEmpty()) {
            loadSchedule(enrolled)
        } else {
            _uiState.value = _uiState.value.copy(entries = emptyList(), isLoading = false)
        }
        _userMessage.value = "Returned to your enrolled timetable"
    }

    /**
     * Permanently switches the user's enrolled academic programme (updates alarms, preferences, and Room cache).
     */
    fun setOfficialProgramme(newProgramme: String) {
        val clean = newProgramme.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            // Cancel previous programme alarms cleanly on IO thread
            val previousEntries = _uiState.value.entries
            withContext(Dispatchers.IO) {
                lastArmedScheduleSignature = null
                if (previousEntries.isNotEmpty()) {
                    classAlarmScheduler?.cancelAllEnrolledClassAlarms(previousEntries)
                }
            }
            repository.setProgrammePref(clean)
            if (appContext != null) {
                com.lectures.core.alarm.AlarmSyncCoordinator.resyncAll(
                    context = appContext,
                    repository = repository,
                    force = true,
                    preserveImminentExisting = false,
                    reason = "programme_switched"
                )
            }
            _uiState.value = _uiState.value.copy(
                enrolledProgramme = clean,
                viewingProgramme = clean,
                isPeeking = false,
                isLoading = true
            )
            loadSchedule(clean)
            _userMessage.value = "Academic profile updated to $clean"
        }
    }

    fun addCustomEvent(event: CustomEvent) {
        viewModelScope.launch {
            val savedId = repository.saveCustomEvent(event)
            val resolvedId = if (event.id > 0L) event.id else savedId
            val savedEvent = event.copy(id = resolvedId)
            val alarmMins = savedEvent.alarmMinutes
            if (alarmMins != null && resolvedId > 0L) {
                if (repository.etagStore.isNotificationsEnabled()) {
                    val ok = classAlarmScheduler?.scheduleCustomEventAlarm(savedEvent) ?: false
                    _userMessage.value = if (ok) {
                        "Activity saved • Alert set (${alarmMins}m before)"
                    } else {
                        "Activity saved"
                    }
                } else {
                    classAlarmScheduler?.cancelCustomEventAlarm(resolvedId)
                    _userMessage.value = "Activity saved (Quiet Mode active — alert paused)"
                }
            } else if (resolvedId > 0L) {
                classAlarmScheduler?.cancelCustomEventAlarm(resolvedId)
                _userMessage.value = if (event.id > 0L) "Activity updated" else "Activity added to schedule"
            }
        }
    }

    fun deleteCustomEvent(id: Long) {
        viewModelScope.launch {
            classAlarmScheduler?.cancelCustomEventAlarm(id)
            repository.deleteCustomEvent(id)
            _userMessage.value = "Activity removed"
        }
    }

    fun addTask(
        title: String,
        courseCode: String,
        dueDate: String,
        priority: String,
        reminderMinutes: Int?,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val task = Assignment(
                title = title,
                courseCode = courseCode,
                dueDate = dueDate,
                reminderMinutes = reminderMinutes,
                priority = priority,
                notes = notes,
                completed = false
            )
            val generatedId = repository.saveAssignment(task)
            if (reminderMinutes != null && generatedId > 0 && alarmScheduler != null && repository.etagStore.isNotificationsEnabled()) {
                alarmScheduler.scheduleTaskReminder(task.copy(id = generatedId))
            }
        }
    }

    fun toggleTaskComplete(task: Assignment) {
        val newCompleted = !task.completed
        // Instant optimistic update
        val updated = _uiState.value.assignments.map {
            if (it.id == task.id) it.copy(completed = newCompleted) else it
        }
        _uiState.value = _uiState.value.copy(assignments = updated)
        viewModelScope.launch {
            repository.updateAssignmentCompletion(task.id, newCompleted)
            if (newCompleted) {
                alarmScheduler?.cancelTaskReminder(task.id)
                _userMessage.value = "Task completed • Alarm cancelled"
            } else if (task.reminderMinutes != null) {
                if (repository.etagStore.isNotificationsEnabled()) {
                    alarmScheduler?.scheduleTaskReminder(task.copy(completed = false))
                    _userMessage.value = "Task reactivated • Alarm restored"
                } else {
                    _userMessage.value = "Task reactivated (Quiet Mode active — alert paused)"
                }
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            alarmScheduler?.cancelTaskReminder(id)
            repository.deleteAssignment(id)
            _userMessage.value = "Task deleted • Alarm cancelled"
        }
    }

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun saveNote(entry: TimetableEntry, noteText: String, reminderMinutes: Int?) {
        viewModelScope.launch {
            repository.saveNote(entry.naturalKey, noteText, reminderMinutes)
            if (reminderMinutes != null) {
                if (!repository.etagStore.isNotificationsEnabled()) {
                    classAlarmScheduler?.cancelClassAlarm(entry.naturalKey)
                    _userMessage.value = "Note saved (Quiet Mode active — alarm paused)"
                } else {
                    val ok = classAlarmScheduler?.scheduleClassAlarm(entry, reminderMinutes, isAlarm = true) ?: true
                    _userMessage.value = if (ok) "Alarm scheduled for ${entry.courseCode} (${reminderMinutes}m before)" else "Class alarm set"
                }
            } else {
                classAlarmScheduler?.cancelClassAlarm(entry.naturalKey)
                _userMessage.value = "Alarm turned off for ${entry.courseCode}"
            }
        }
    }

    fun triggerTestAlarm(entry: TimetableEntry) {
        classAlarmScheduler?.triggerTestAlarm(entry)
    }

    companion object {
        fun provideFactory(
            repository: TimetableRepository,
            alarmScheduler: TaskAlarmScheduler? = null,
            classAlarmScheduler: AlarmScheduler? = null,
            appContext: Context? = null
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TimetableViewModel(
                        repository = repository,
                        alarmScheduler = alarmScheduler,
                        classAlarmScheduler = classAlarmScheduler,
                        appContext = appContext?.applicationContext
                    ) as T
                }
            }
    }
}

