package com.taskmaster.app.ui

import com.taskmaster.app.data.Note
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
private val SAME_YEAR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())
private val OTHER_YEAR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

private fun dayLabel(date: LocalDate, today: LocalDate): String = when {
    date == today -> "Today"
    date == today.minusDays(1) -> "Yesterday"
    date.year == today.year -> date.format(SAME_YEAR_FORMAT)
    else -> date.format(OTHER_YEAR_FORMAT)
}

/** Exact stamp shown on a note, e.g. "Today · 1:47 PM" or "Mon 2 Nov · 9:15 AM". */
internal fun noteStamp(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    return "${dayLabel(time.toLocalDate(), LocalDate.now(zone))} · ${time.format(TIME_FORMAT)}"
}

/** "just now", "12 min ago", "3 h ago"; null once the note is a day old or more. */
internal fun noteAgo(epochMillis: Long, now: Long = System.currentTimeMillis()): String? {
    val minutes = (now - epochMillis) / 60_000
    return when {
        minutes < 0 -> null
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 24 * 60 -> "${minutes / 60} h ago"
        else -> null
    }
}

/** Live "Tuesday, 3 November · 1:47 PM" shown above the capture box. */
internal fun nowStamp(): String {
    val zone = ZoneId.systemDefault()
    val time = Instant.now().atZone(zone)
    val date = time.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    return "$date · ${time.format(TIME_FORMAT)}"
}

/** A row in the notes list: either a day heading or a note. */
internal sealed interface DumpItem {
    val key: String

    data class Header(val label: String) : DumpItem {
        override val key: String get() = "h:$label"
    }

    data class Entry(val note: Note) : DumpItem {
        override val key: String get() = "n:${note.id}"
    }
}

/** Pinned notes first under "Pinned", then the rest grouped by day, newest first. */
internal fun groupNotes(notes: List<Note>): List<DumpItem> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val result = ArrayList<DumpItem>()

    val pinned = notes.filter { it.pinned }.sortedByDescending { it.createdAt }
    if (pinned.isNotEmpty()) {
        result.add(DumpItem.Header("📌 Pinned"))
        pinned.forEach { result.add(DumpItem.Entry(it)) }
    }

    var lastLabel: String? = null
    notes.filter { !it.pinned }.sortedByDescending { it.createdAt }.forEach { note ->
        val label = dayLabel(Instant.ofEpochMilli(note.createdAt).atZone(zone).toLocalDate(), today)
        if (label != lastLabel) {
            result.add(DumpItem.Header(label))
            lastLabel = label
        }
        result.add(DumpItem.Entry(note))
    }
    return result
}

/** Plain-text export, grouped by category, each note with its exact date and time. */
internal fun exportNotesText(notes: List<Note>): String {
    val out = StringBuilder("TaskMaster brain dump\n")
    notes.groupBy { it.category }.toSortedMap().forEach { (category, list) ->
        out.append("\n").append(category.emoji).append(" ").append(category.label).append("\n")
        list.sortedByDescending { it.createdAt }.forEach { n ->
            out.append("- [").append(noteStamp(n.createdAt)).append("] ")
                .append(n.text.trim().replace("\n", "\n  ")).append("\n")
        }
    }
    return out.toString()
}
