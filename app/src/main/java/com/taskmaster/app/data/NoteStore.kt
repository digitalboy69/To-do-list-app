package com.taskmaster.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/**
 * Brain-dump notes, shared by the app screen and the floating popup (same pattern as TaskStore).
 * Also keeps the half-typed [draft], so closing the popup mid-thought never loses anything.
 * Main thread only.
 */
class NoteStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("taskmaster_prefs", Context.MODE_PRIVATE)

    var notes by mutableStateOf(load())
        private set

    var draft by mutableStateOf(prefs.getString(KEY_DRAFT, "") ?: "")
        private set

    private fun load(): List<Note> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return NoteJson.parse(raw)
    }

    fun updateDraft(text: String) {
        draft = text.take(MAX_LENGTH)
        prefs.edit().putString(KEY_DRAFT, draft).apply()
    }

    /** Returns false when the text is blank (nothing is saved). The time is stamped now. */
    fun add(text: String, category: NoteCategory): Boolean {
        val clean = text.trim().take(MAX_LENGTH)
        if (clean.isEmpty()) return false
        val note = Note(
            id = UUID.randomUUID().toString(),
            text = clean,
            category = category,
            createdAt = System.currentTimeMillis()
        )
        update(listOf(note) + notes)
        return true
    }

    /** Returns false when the new text is blank (nothing changes). Keeps the original time. */
    fun edit(id: String, text: String, category: NoteCategory): Boolean {
        val clean = text.trim().take(MAX_LENGTH)
        if (clean.isEmpty()) return false
        update(notes.map { if (it.id == id) it.copy(text = clean, category = category) else it })
        return true
    }

    fun togglePin(id: String) {
        update(notes.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })
    }

    fun delete(id: String) {
        update(notes.filterNot { it.id == id })
    }

    /** Creates a task from the note (first line is the title) and marks the note as done. */
    fun toTask(id: String, tasks: TaskStore): Boolean {
        val note = notes.firstOrNull { it.id == id } ?: return false
        val firstLine = note.text.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
            ?: return false
        val title = firstLine.take(80)
        val desc = if (note.text.trim().length > title.length) note.text.trim() else ""
        if (!tasks.add(title, desc, Priority.MEDIUM)) return false
        update(notes.map { if (it.id == id) it.copy(tasked = true) else it })
        return true
    }

    private fun update(newNotes: List<Note>) {
        notes = newNotes
        prefs.edit().putString(KEY_NOTES, NoteJson.toJson(newNotes)).apply()
    }

    companion object {
        private const val KEY_NOTES = "notes"
        private const val KEY_DRAFT = "note_draft"
        const val MAX_LENGTH = 5000

        @Volatile
        private var instance: NoteStore? = null

        fun get(context: Context): NoteStore =
            instance ?: synchronized(this) {
                instance ?: NoteStore(context.applicationContext).also { instance = it }
            }
    }
}
