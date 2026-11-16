package com.lectures.features.timetable.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Design Tokens & Theme Colors from Specification
val SurfaceBaseLight = Color(0xFFFAF8FF)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF2F3FF)
val SurfaceContainerHighLight = Color(0xFFE8EBFA)
val SurfaceDimLight = Color(0xFFD2D9F4)

val DarkSurfaceBase = Color(0xFF101216)
val DarkSurfaceCard = Color(0xFF181B22)
val DarkSurfaceContainer = Color(0xFF20242D)
val DarkBorderSubtle = Color(0xFF2B303C)

val PrimaryBlue = Color(0xFF2563EB)
val PrimaryBlueDark = Color(0xFF1D4ED8)
val PrimaryBlueLight = Color(0xFF3B82F6)
val IndigoAccent = Color(0xFF4F46E5)

val HeroGradientStart = Color(0xFF2563EB)
val HeroGradientMid = Color(0xFF3B82F6)
val HeroGradientEnd = Color(0xFF4F46E5)

val ProgressTrackBackground = Color(0xFFFFFFFF).copy(alpha = 0.25f)
val ProgressIndicatorActive = Color(0xFF34D399)
val ProgressGradientStart = Color(0xFF60A5FA)
val ProgressGradientEnd = Color(0xFF34D399)

val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF475569)
val TextMutedLight = Color(0xFF64748B)
val TextTertiaryLight = Color(0xFF94A3B8)

val StatusGreenLive = Color(0xFF10B981)
val StatusTheoryBlue = Color(0xFF2563EB)
val StatusLabPurple = Color(0xFF7C3AED)
val StatusLectureGreen = Color(0xFF059669)

val BorderSubtleLight = Color(0xFFE2E8F0)

val DayCapsuleShape = RoundedCornerShape(22.dp)
val HeroCardShape = RoundedCornerShape(24.dp)
val UpNextBarShape = RoundedCornerShape(18.dp)
val TimelineCardShape = RoundedCornerShape(16.dp)
val ProgressTrackShape = RoundedCornerShape(10.dp)
val PillBadgeShape = RoundedCornerShape(12.dp)
val ActionButtonShape = RoundedCornerShape(14.dp)
val FabShape = RoundedCornerShape(28.dp)

enum class SpecSessionType(val label: String, val tagColor: Color) {
    LECTURE("Lecture", StatusLectureGreen),
    THEORY("Lecture", StatusTheoryBlue),
    LAB("Lab Session", StatusLabPurple),
    PRACTICAL("Practical", Color(0xFF16A34A)),
    CLINICAL("Clinical", Color(0xFFD97706)),
    ASSOCIATION("Student Association", Color(0xFF8B5CF6))
}

data class SpecClassSession(
    val id: String,
    val courseCode: String,
    val courseName: String,
    val department: String,
    val topicDescription: String?,
    val startTime: String,
    val endTime: String,
    val type: SpecSessionType,
    val venue: String?,
    val floor: Int? = null,
    val lecturer: String?,
    val startsInText: String? = null,
    val attachedNotesCount: Int = 0,
    val attachedNotePreview: String? = null,
    val isAlarmSet: Boolean = false,
    val alarmMinutes: Int? = null,
    val isHappeningNow: Boolean = false,
    val hasConflict: Boolean = false,
    val progress: Float = 0.70f,
    val elapsedText: String = "1h 45m elapsed",
    val remainingText: String = "35 mins left · Ends 11:00",
    val sharedWith: List<String> = emptyList()
)
