package com.lectures.features.settings

import com.lectures.BuildConfig
import com.lectures.features.timetable.domain.Assignment
import com.lectures.features.timetable.domain.CustomEvent
import com.lectures.features.timetable.domain.LectureNote
import org.json.JSONArray
import org.json.JSONObject

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
                        put("title", e.title)
                        put("category", e.category)
                        put("dayOfWeek", e.dayOfWeek)
                        put("startTime", e.startTime)
                        put("endTime", e.endTime)
                        put("location", e.location)
                    })
                }
            })
            put("assignments", JSONArray().apply {
                tasks.forEach { t ->
                    put(JSONObject().apply {
                        put("id", t.id)
                        put("title", t.title)
                        put("courseCode", t.courseCode)
                        put("dueDate", t.dueDate)
                        put("priority", t.priority)
                        put("completed", t.completed)
                    })
                }
            })
        }
        return root.toString(2)
    }
}
