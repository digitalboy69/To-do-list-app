package com.taskmaster.app.data

enum class Priority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromName(name: String?): Priority =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: MEDIUM
    }
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    PENDING("Pending"),
    COMPLETED("Completed")
}

enum class SortOrder(val label: String) {
    NEWEST("Newest"),
    PRIORITY("Priority"),
    DUE("Due date");

    companion object {
        fun fromName(name: String?): SortOrder =
            entries.firstOrNull { it.name == name } ?: NEWEST
    }
}

data class Task(
    val id: String,
    val title: String,
    val desc: String,
    val priority: Priority,
    val completed: Boolean,
    val createdAt: Long,
    /** Due day as LocalDate epoch day, or null when the task has no due date. */
    val dueDate: Long? = null
)
