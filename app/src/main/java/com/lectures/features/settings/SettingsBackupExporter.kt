package com.lectures.features.settings

import com.lectures.BuildConfig
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.UUID

internal data class ParsedBackupData(
    val appVersion: String,
    val exportedAt: Long,
    val programme: String,
    val notes: List<LectureNote>,
    val events: List<CustomEvent>,
    val assignments: List<Assignment>
)

internal object SettingsBackupExporter {

    fun buildBackupJson(
        programme: String,
        notes: List<LectureNote>,
        events: List<CustomEvent>,
        tasks: List<Assignment>,
        exportedAtMillis: Long = System.currentTimeMillis()
    ): String {
        val root = JSONObject().apply {
            put("appVersion", BuildConfig.VERSION_NAME)
            put("exportedAt", exportedAtMillis)
            put("programme", programme)
            put("notes", JSONArray().apply {
                notes.forEach { n ->
                    put(JSONObject().apply {
                        put("naturalKey", n.naturalKey)
                        put("title", n.title ?: "")
                        put("content", n.content)
                        put("tag", n.tag ?: "")
                        put("attachedClass", n.attachedClass ?: "")
                        put("colourTag", n.colourTag ?: "")
                        put("alarmMinutes", n.alarmMinutes ?: JSONObject.NULL)
                        put("isPinned", n.isPinned)
                        put("updatedAt", n.updatedAt)
                    })
                }
            })
            put("customEvents", JSONArray().apply {
                events.forEach { e ->
                    put(JSONObject().apply {
                        put("id", e.id)
                        put("clientUuid", e.clientUuid)
                        put("title", e.title)
                        put("category", e.category)
                        put("dayOfWeek", e.dayOfWeek)
                        put("startTime", e.startTime)
                        put("endTime", e.endTime)
                        put("location", e.location)
                        put("notes", e.notes)
                        put("repeatWeekly", e.repeatWeekly)
                        put("alarmMinutes", e.alarmMinutes ?: JSONObject.NULL)
                        put("colorTag", e.colorTag)
                        put("updatedAt", e.updatedAt)
                    })
                }
            })
            put("assignments", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("clientUuid", t.clientUuid)
                        put("title", t.title)
                        put("courseCode", t.courseCode)
                        put("dueDate", t.dueDate)
                        put("priority", t.priority)
                        put("notes", t.notes)
                        put("reminderMinutes", t.reminderMinutes ?: JSONObject.NULL)
                        put("completed", t.completed)
                        put("updatedAt", t.updatedAt)
                    })
                }
            })
        }
        return root.toString(2)
    }

    fun parseBackupJson(rawJson: String): Result<ParsedBackupData> {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Backup file is empty."))
        }
        return try {
            val root = JSONObject(trimmed)
            val hasNotes = root.has("notes") && root.optJSONArray("notes") != null
            val hasEvents = root.has("customEvents") && root.optJSONArray("customEvents") != null
            val hasAssignments = root.has("assignments") && root.optJSONArray("assignments") != null
            if (!hasNotes && !hasEvents && !hasAssignments) {
                return Result.failure(
                    IllegalArgumentException("Unrecognized backup file format. Expected Lectures JSON backup data.")
                )
            }

            val fallbackTime = root.optLong("exportedAt", System.currentTimeMillis())
                .takeIf { it > 0L } ?: System.currentTimeMillis()
            val appVersion = root.optString("appVersion", "")
            val programme = root.optString("programme", "")

            val notes = mutableListOf<LectureNote>()
            val notesArray = root.optJSONArray("notes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val obj = notesArray.optJSONObject(i) ?: continue
                    val key = obj.optString("naturalKey", "").trim()
                    if (key.isBlank()) continue
                    val alarmMinutes = if (obj.isNull("alarmMinutes") || !obj.has("alarmMinutes")) {
                        null
                    } else {
                        obj.optInt("alarmMinutes", -1).takeIf { it >= 0 }
                    }
                    notes.add(
                        LectureNote(
                            naturalKey = key,
                            title = obj.optString("title", "").ifBlank { null },
                            content = obj.optString("content", ""),
                            updatedAt = obj.optLong("updatedAt", fallbackTime).takeIf { it > 0L } ?: fallbackTime,
                            colourTag = obj.optString("colourTag", "").ifBlank { null },
                            alarmMinutes = alarmMinutes,
                            tag = obj.optString("tag", "").ifBlank { null },
                            attachedClass = obj.optString("attachedClass", "").ifBlank { null },
                            isPinned = obj.optBoolean("isPinned", false),
                            isDeleted = false
                        )
                    )
                }
            }

            val events = mutableListOf<CustomEvent>()
            val eventsArray = root.optJSONArray("customEvents")
            if (eventsArray != null) {
                for (i in 0 until eventsArray.length()) {
                    val obj = eventsArray.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "").trim()
                    if (title.isBlank()) continue
                    val dayOfWeek = obj.optString("dayOfWeek", "Monday").ifBlank { "Monday" }
                    val startTime = obj.optString("startTime", "08:00").ifBlank { "08:00" }
                    val endTime = obj.optString("endTime", "09:00").ifBlank { "09:00" }
                    val rawUuid = obj.optString("clientUuid", "").trim()
                    val resolvedUuid = rawUuid.ifBlank {
                        UUID.nameUUIDFromBytes(
                            "event|$title|$dayOfWeek|$startTime|$endTime".toByteArray(StandardCharsets.UTF_8)
                        ).toString()
                    }
                    val alarmMinutes = if (obj.isNull("alarmMinutes") || !obj.has("alarmMinutes")) {
                        null
                    } else {
                        obj.optInt("alarmMinutes", -1).takeIf { it >= 0 }
                    }
                    events.add(
                        CustomEvent(
                            id = 0L,
                            clientUuid = resolvedUuid,
                            title = title,
                            dayOfWeek = dayOfWeek,
                            startTime = startTime,
                            endTime = endTime,
                            location = obj.optString("location", ""),
                            notes = obj.optString("notes", ""),
                            repeatWeekly = obj.optBoolean("repeatWeekly", true),
                            alarmMinutes = alarmMinutes,
                            category = obj.optString("category", "Study").ifBlank { "Study" },
                            colorTag = obj.optString("colorTag", "#2563EB").ifBlank { "#2563EB" },
                            updatedAt = obj.optLong("updatedAt", fallbackTime).takeIf { it > 0L } ?: fallbackTime,
                            isDeleted = false
                        )
                    )
                }
            }

            val assignments = mutableListOf<Assignment>()
            val tasksArray = root.optJSONArray("assignments")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.optJSONObject(i) ?: continue
                    val title = obj.optString("title", "").trim()
                    if (title.isBlank()) continue
                    val courseCode = obj.optString("courseCode", "")
                    val dueDate = obj.optString("dueDate", "")
                    val rawUuid = obj.optString("clientUuid", "").trim()
                    val resolvedUuid = rawUuid.ifBlank {
                        UUID.nameUUIDFromBytes(
                            "assignment|$title|$courseCode|$dueDate".toByteArray(StandardCharsets.UTF_8)
                        ).toString()
                    }
                    val reminderMinutes = if (obj.isNull("reminderMinutes") || !obj.has("reminderMinutes")) {
                        null
                    } else {
                        obj.optInt("reminderMinutes", -1).takeIf { it >= 0 }
                    }
                    assignments.add(
                        Assignment(
                            id = 0L,
                            clientUuid = resolvedUuid,
                            title = title,
                            courseCode = courseCode,
                            dueDate = dueDate,
                            reminderMinutes = reminderMinutes,
                            priority = obj.optString("priority", "Medium").ifBlank { "Medium" },
                            notes = obj.optString("notes", ""),
                            completed = obj.optBoolean("completed", false),
                            updatedAt = obj.optLong("updatedAt", fallbackTime).takeIf { it > 0L } ?: fallbackTime,
                            isDeleted = false
                        )
                    )
                }
            }

            Result.success(
                ParsedBackupData(
                    appVersion = appVersion,
                    exportedAt = fallbackTime,
                    programme = programme,
                    notes = notes,
                    events = events,
                    assignments = assignments
                )
            )
        } catch (e: Exception) {
            Result.failure(
                IllegalArgumentException("Invalid JSON backup file: ${e.localizedMessage ?: "Parse error"}")
            )
        }
    }
}
