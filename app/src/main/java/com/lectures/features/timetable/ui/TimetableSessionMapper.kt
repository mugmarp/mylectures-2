package com.lectures.features.timetable.ui

import com.lectures.core.util.SessionProgress
import com.lectures.core.util.TimeUtil
import com.lectures.features.timetable.domain.LectureNote
import com.lectures.features.timetable.domain.TimetableEntry
import com.lectures.features.timetable.domain.TimetableMatcher
import java.util.Calendar
import java.util.Locale

internal fun TimetableEntry.toSpecClassSession(
    currentProgramme: String = "",
    note: LectureNote? = null,
    isLive: Boolean = false,
    progress: SessionProgress? = null,
    startsInText: String? = null,
    hasConflict: Boolean = false
): SpecClassSession {
    val codeUpper = courseCode.trim().uppercase(Locale.ROOT)
    val titleUpper = courseTitle.trim().uppercase(Locale.ROOT)
    val typeUpper = sessionType?.trim()?.uppercase(Locale.ROOT) ?: ""

    val specType = when {
        codeUpper.contains("MUCOSA") || titleUpper.contains("MUCOSA") ||
            titleUpper.contains("ASSOCIATION") || titleUpper.contains("GUILD") ||
            codeUpper.contains("GUILD") || titleUpper.contains("SOCIETY") -> SpecSessionType.ASSOCIATION
        typeUpper.contains("LAB") || room?.uppercase(Locale.ROOT)?.contains("LAB") == true -> SpecSessionType.LAB
        typeUpper.contains("PRACTICAL") -> SpecSessionType.PRACTICAL
        typeUpper.contains("CLINICAL") || titleUpper.contains("CLINICAL") || titleUpper.contains("WARD") -> SpecSessionType.CLINICAL
        else -> SpecSessionType.LECTURE
    }

    val elapsedMins = progress?.elapsedMinutes ?: 0
    val remMins = progress?.remainingMinutes ?: 0
    val elapsedStr = if (elapsedMins >= 60) "${elapsedMins / 60}h ${elapsedMins % 60}m elapsed" else "${elapsedMins}m elapsed"
    val remStr = if (remMins >= 60) {
        val h = remMins / 60
        val m = remMins % 60
        if (m > 0) "${h}h ${m}m left · Ends $endTime" else "${h}h left · Ends $endTime"
    } else if (remMins > 0) {
        "$remMins mins left · Ends $endTime"
    } else {
        "Ending soon · Ends $endTime"
    }

    val cleanVenue = room?.trim()?.takeIf {
        it.isNotBlank() && it.uppercase(Locale.ROOT) !in listOf("TBA", "TBD", "NONE", "N/A", "NULL")
    }
    val cleanLecturer = lecturer?.trim()?.takeIf {
        it.isNotBlank() && !it.equals("Staff", ignoreCase = true) && !it.equals("TBD", ignoreCase = true) && !it.equals("None", ignoreCase = true)
    }
    val cleanNotePreview = note?.content?.trim()?.takeIf { it.isNotBlank() }?.take(40)

    val isAssociation = specType == SpecSessionType.ASSOCIATION ||
        codeUpper.contains("MUCOSA") || titleUpper.contains("MUCOSA") ||
        titleUpper.contains("ASSOCIATION") || titleUpper.contains("GUILD")

    val userParsed = TimetableMatcher.parseGroupParts(currentProgramme)
    val userCanon = TimetableMatcher.canonicalCohort(currentProgramme)

    val allCohorts = (listOf(programmeGroup) + shared_with)
        .map { TimetableMatcher.canonicalCohort(it.trim()) }
        .filter { it.isNotBlank() }
        .distinct()

    val otherCohorts = if (isAssociation) emptyList() else allCohorts.filter { cohort ->
        if (cohort.equals(currentProgramme.trim(), ignoreCase = true)) return@filter false
        if (userCanon.isNotBlank() && cohort.equals(userCanon, ignoreCase = true)) return@filter false
        if (userParsed != null) {
            val cParsed = TimetableMatcher.parseGroupParts(cohort)
            if (cParsed != null && cParsed.code == userParsed.code && cParsed.year == userParsed.year) {
                if (userParsed.subject.isEmpty() || cParsed.subject.isEmpty() ||
                    cParsed.subject.equals(userParsed.subject, ignoreCase = true) ||
                    (userParsed.subject.startsWith("BIOLOG") && cParsed.subject.startsWith("BIOLOG"))
                ) {
                    return@filter false
                }
            }
        }
        true
    }

    val cohortsText = if (isAssociation) "" else otherCohorts.joinToString(", ")
    val effectiveShared = if (isAssociation) emptyList() else otherCohorts

    return SpecClassSession(
        id = naturalKey,
        courseCode = courseCode,
        courseName = courseTitle,
        department = cohortsText,
        topicDescription = null,
        startTime = startTime,
        endTime = endTime,
        type = specType,
        venue = cleanVenue,
        floor = this.floor,
        lecturer = cleanLecturer,
        startsInText = startsInText,
        attachedNotesCount = if (cleanNotePreview != null) 1 else 0,
        attachedNotePreview = cleanNotePreview,
        isAlarmSet = note?.alarmMinutes != null,
        alarmMinutes = note?.alarmMinutes,
        isHappeningNow = isLive,
        hasConflict = hasConflict,
        progress = progress?.progress ?: 0.5f,
        elapsedText = elapsedStr,
        remainingText = remStr,
        sharedWith = effectiveShared
    )
}

internal fun findActualNextUp(
    entries: List<TimetableEntry>,
    todayName: String
): GlobalNextUp? {
    if (entries.isEmpty()) return null

    val now = Calendar.getInstance()
    val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val todayEntries = entries.filter { it.dayOfWeek.equals(todayName, ignoreCase = true) }

    for (entry in todayEntries) {
        val prog = TimeUtil.calculateProgress(entry.startTime, entry.endTime, entry.dayOfWeek)
        if (prog.isOngoing) {
            return GlobalNextUp(
                entry = entry,
                isLive = true,
                progress = prog,
                minutesUntil = 0,
                dayLabel = "Today"
            )
        }
    }

    val upcomingToday = todayEntries
        .mapNotNull { entry ->
            val start = TimeUtil.toMinutes(entry.startTime)
            if (start > currentMinutes) {
                Pair(entry, start - currentMinutes)
            } else null
        }
        .minByOrNull { it.second }

    if (upcomingToday != null) {
        return GlobalNextUp(
            entry = upcomingToday.first,
            isLive = false,
            progress = null,
            minutesUntil = upcomingToday.second,
            dayLabel = "Today"
        )
    }

    val days = TimeUtil.DAYS
    val todayIdx = days.indexOfFirst { it.equals(todayName, ignoreCase = true) }.coerceAtLeast(0)
    for (offset in 1..6) {
        val nextDayIdx = (todayIdx + offset) % 7
        val nextDayName = days[nextDayIdx]
        val nextDayEntries = entries
            .filter { it.dayOfWeek.equals(nextDayName, ignoreCase = true) }
            .sortedBy { TimeUtil.toMinutes(it.startTime) }

        val earliest = nextDayEntries.firstOrNull()
        if (earliest != null) {
            val startMin = TimeUtil.toMinutes(earliest.startTime)
            val minutesUntil = (offset * 24 * 60) - currentMinutes + startMin
            val label = if (offset == 1) "Tomorrow" else nextDayName
            return GlobalNextUp(
                entry = earliest,
                isLive = false,
                progress = null,
                minutesUntil = minutesUntil.coerceAtLeast(1),
                dayLabel = label
            )
        }
    }

    return null
}
