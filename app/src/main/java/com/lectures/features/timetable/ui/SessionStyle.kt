package com.lectures.features.timetable.ui

import androidx.compose.ui.graphics.Color
import com.lectures.features.timetable.domain.TimetableEntry
import java.util.Locale

data class SessionStyle(
    val badgeBg: Color,
    val badgeText: Color,
    val accentBar: Color
)

fun sessionStyle(sessionType: String?, room: String, title: String): SessionStyle {
    val type = sessionType?.uppercase(Locale.ROOT) ?: ""
    val isAssociation = title.contains("MUCOSA", ignoreCase = true) ||
            title.contains("ASSOCIATION", ignoreCase = true) ||
            title.contains("GUILD", ignoreCase = true) ||
            title.contains("SOCIETY", ignoreCase = true) ||
            type.contains("ASSOCIATION")

    val isPractical = type == "PRACTICAL" || type == "LAB" || room.uppercase(Locale.ROOT).contains("LAB")
    val isClinical = type == "CLINICAL" || title.uppercase(Locale.ROOT).contains("CLINICAL") || title.uppercase(Locale.ROOT).contains("WARD")

    return when {
        isAssociation -> SessionStyle(Color(0xFF8B5CF6), Color.White, Color(0xFF7C3AED))
        isClinical -> SessionStyle(Color(0xFFF59E0B), Color.White, Color(0xFFD97706))
        isPractical -> SessionStyle(Color(0xFF22C55E), Color.White, Color(0xFF16A34A))
        type == "THEORY" -> SessionStyle(Color(0xFF3B82F6), Color.White, Color(0xFF2563EB))
        else -> SessionStyle(Color(0xFF3B82F6), Color.White, Color(0xFF2563EB))
    }
}

fun resolveSessionTypeLabel(entry: TimetableEntry): String {
    val code = entry.courseCode.trim().uppercase(Locale.ROOT)
    val title = entry.courseTitle.trim().uppercase(Locale.ROOT)
    if (code.contains("MUCOSA") || title.contains("MUCOSA") ||
        title.contains("ASSOCIATION") || title.contains("GUILD") ||
        code.contains("GUILD") || title.contains("SOCIETY")
    ) {
        return "Student Association"
    }

    val type = entry.sessionType?.trim()
    if (!type.isNullOrEmpty()) {
        return when (type.uppercase(Locale.ROOT)) {
            "THEORY" -> "Lecture"
            "LAB" -> "Lab"
            "PRACTICAL" -> "Practical"
            "CLINICAL" -> "Clinical"
            else -> type.lowercase(Locale.ROOT).replaceFirstChar { it.uppercase(Locale.ROOT) }
        }
    }

    if (entry.room?.uppercase(Locale.ROOT)?.contains("LAB") == true) return "Lab"
    if (title.contains("CLINICAL") || title.contains("WARD")) return "Clinical"
    return "Lecture"
}
