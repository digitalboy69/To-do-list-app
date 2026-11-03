package com.taskmaster.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Where a brain-dump note belongs. [color] is an ARGB value used for its accent. */
enum class NoteCategory(val label: String, val emoji: String, val color: Long) {
    IDEA("Idea", "💡", 0xFFF59E0B),
    TODO("To do", "✅", 0xFF6366F1),
    WORK("Work", "💼", 0xFF3B82F6),
    PERSONAL("Personal", "🏠", 0xFFEC4899),
    BUY("Buy", "🛒", 0xFF10B981),
    LEARN("Learn", "📚", 0xFFA855F7),
    LINK("Link", "🔗", 0xFF06B6D4),
    RANDOM("Random", "🎲", 0xFF8A8A93);

    companion object {
        fun fromName(name: String?): NoteCategory =
            entries.firstOrNull { it.name == name } ?: RANDOM

        /** Cheap keyword guess so most notes land in the right place without any tapping. */
        fun guess(text: String): NoteCategory {
            val t = text.trim().lowercase()
            if (t.isEmpty()) return IDEA
            return when {
                "http://" in t || "https://" in t || "www." in t -> LINK
                t.startsWith("buy ") || t.startsWith("get ") || "grocer" in t ||
                    "shopping" in t -> BUY
                t.startsWith("todo") || t.startsWith("remember to") || t.startsWith("don't forget") ||
                    "need to " in t || "have to " in t -> TODO
                "meeting" in t || "client" in t || "deadline" in t || "email" in t ||
                    "report" in t || "boss" in t -> WORK
                t.startsWith("learn ") || t.startsWith("read ") || t.startsWith("watch ") ||
                    "course" in t || "book" in t -> LEARN
                "mom" in t || "dad" in t || "family" in t || "birthday" in t ||
                    "doctor" in t || "friend" in t -> PERSONAL
                else -> IDEA
            }
        }
    }
}

/** One captured thought. [createdAt] is epoch millis, set the moment it is saved. */
data class Note(
    val id: String,
    val text: String,
    val category: NoteCategory,
    val createdAt: Long,
    val pinned: Boolean = false,
    /** True once the note has been turned into a task. */
    val tasked: Boolean = false
)

object NoteJson {
    fun toJson(notes: List<Note>): String {
        val array = JSONArray()
        notes.forEach { n ->
            array.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("text", n.text)
                    put("category", n.category.name)
                    put("createdAt", n.createdAt)
                    put("pinned", n.pinned)
                    put("tasked", n.tasked)
                }
            )
        }
        return array.toString()
    }

    fun parse(raw: String): List<Note> {
        try {
            val array = JSONArray(raw)
            val result = ArrayList<Note>(array.length())
            for (i in 0 until array.length()) {
                val o = array.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                val text = o.optString("text", "")
                if (id.isEmpty() || text.isBlank()) continue
                result.add(
                    Note(
                        id = id,
                        text = text,
                        category = NoteCategory.fromName(o.optString("category", "")),
                        createdAt = o.optLong("createdAt", 0L),
                        pinned = o.optBoolean("pinned", false),
                        tasked = o.optBoolean("tasked", false)
                    )
                )
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }
}
