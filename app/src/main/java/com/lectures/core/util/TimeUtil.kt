package com.lectures.core.util

import java.util.Calendar

data class SessionProgress(
    val isOngoing: Boolean,
    val progress: Float, // 0.0f to 1.0f
    val elapsedMinutes: Int,
    val remainingMinutes: Int,
    val totalMinutes: Int
)

object TimeUtil {
    val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    /**
     * MUST 2026/2027 Academic Year Semester I commenced around late August / early September.
     * Calculates current active semester week (e.g. "Week 6 • Semester 1").
     */
    fun currentSemesterWeekLabel(): String = semesterWeekLabelFor(Calendar.getInstance())

    fun semesterWeekLabelFor(now: Calendar): String {
        val month = now.get(Calendar.MONTH)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        if ((month == Calendar.AUGUST && dayOfMonth < 24) || month == Calendar.JUNE || month == Calendar.JULY) {
            return "Academic Recess"
        }

        val weekLabel = academicWeekFor(now)
        if (weekLabel == "Recess") return "Academic Recess"
        val semesterNumber = if (month >= Calendar.AUGUST || month <= Calendar.JANUARY) 1 else 2
        return "$weekLabel • Sem $semesterNumber"
    }

    /**
     * Returns strictly the week identifier (e.g. "Week 7", "Recess / Exams", or "Recess")
     * without repeating the semester number.
     */
    fun currentAcademicWeek(): String = academicWeekFor(Calendar.getInstance())

    fun academicWeekFor(now: Calendar): String {
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val dayOfMonth = now.get(Calendar.DAY_OF_MONTH)

        if ((month == Calendar.AUGUST && dayOfMonth < 24) || month == Calendar.JUNE || month == Calendar.JULY) {
            return "Recess"
        }

        val semStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (month >= Calendar.AUGUST) {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.AUGUST)
                set(Calendar.DAY_OF_MONTH, 24)
            } else if (month <= Calendar.JANUARY) {
                set(Calendar.YEAR, year - 1)
                set(Calendar.MONTH, Calendar.AUGUST)
                set(Calendar.DAY_OF_MONTH, 24)
            } else {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, Calendar.FEBRUARY)
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }

        val diffMillis = now.timeInMillis - semStart.timeInMillis
        val diffDays = (diffMillis / (1000L * 60L * 60L * 24L)).toInt()
        val weekNum = (diffDays / 7) + 1

        return when {
            weekNum <= 0 -> "Recess"
            weekNum in 1..17 -> "Week $weekNum"
            else -> "Recess / Exams"
        }
    }

    fun todayName(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "Monday"
            Calendar.TUESDAY -> "Tuesday"
            Calendar.WEDNESDAY -> "Wednesday"
            Calendar.THURSDAY -> "Thursday"
            Calendar.FRIDAY -> "Friday"
            Calendar.SATURDAY -> "Saturday"
            Calendar.SUNDAY -> "Sunday"
            else -> "Monday"
        }
    }

    fun toMinutes(timeString: String): Int {
        if (timeString.isBlank() || !timeString.contains(":")) return 0
        val clean = timeString.trim().uppercase()
        val isPm = clean.endsWith("PM")
        val isAm = clean.endsWith("AM")
        val stripped = clean.removeSuffix("PM").removeSuffix("AM").trim()
        val parts = stripped.split(":")
        if (parts.size != 2) return 0
        var hours = parts[0].trim().toIntOrNull() ?: return 0
        val minutes = parts[1].trim().toIntOrNull() ?: return 0
        if (isPm && hours < 12) hours += 12
        if (isAm && hours == 12) hours = 0
        if (hours !in 0..23 || minutes !in 0..59) return 0
        return hours * 60 + minutes
    }

    fun rangesOverlap(start1: Int, end1: Int, start2: Int, end2: Int): Boolean {
        return start1 < end2 && start2 < end1
    }

    fun calculateProgress(startTime: String, endTime: String?, dayOfWeek: String): SessionProgress {
        if (endTime.isNullOrBlank()) {
            return SessionProgress(false, 0f, 0, 0, 0)
        }

        val today = todayName()
        val matchesDay = dayOfWeek.equals(today, ignoreCase = true) ||
                dayOfWeek.startsWith(today.take(3), ignoreCase = true) ||
                today.startsWith(dayOfWeek.take(3), ignoreCase = true)
        if (!matchesDay) {
            return SessionProgress(false, 0f, 0, 0, 0)
        }

        val cal = Calendar.getInstance()
        val now = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val start = toMinutes(startTime)
        val end = toMinutes(endTime)

        if (end <= start) {
            return SessionProgress(false, 0f, 0, 0, 0)
        }

        if (now in start until end) {
            val elapsed = now - start
            val total = end - start
            val remaining = end - now
            val fraction = (elapsed.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            return SessionProgress(
                isOngoing = true,
                progress = fraction,
                elapsedMinutes = elapsed,
                remainingMinutes = remaining,
                totalMinutes = total
            )
        }

        return SessionProgress(false, 0f, 0, 0, 0)
    }
}
