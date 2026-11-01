package com.taskmaster.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/**
 * Single in-process source of truth for tasks. Both the app screen (via TaskViewModel) and the
 * floating bubble popup (OverlayService) read and write through this object, so a task ticked in
 * the popup shows up in the app immediately, and the other way round.
 *
 * Must only be used from the main thread (Activity / Service callbacks).
 */
class TaskStore private constructor(context: Context) {

    private val repository = TaskRepository(context)

    var tasks by mutableStateOf(repository.load())
        private set

    /** Returns false when the title is blank (nothing is created). */
    fun add(title: String, desc: String, priority: Priority, dueDate: Long? = null): Boolean {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return false
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            desc = desc.trim(),
            priority = priority,
            completed = false,
            createdAt = System.currentTimeMillis(),
            dueDate = dueDate
        )
        update(listOf(task) + tasks)
        return true
    }

    /** Returns false when the title is blank (nothing is changed). */
    fun edit(id: String, title: String, desc: String, priority: Priority, dueDate: Long?): Boolean {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return false
        update(
            tasks.map {
                if (it.id == id) {
                    it.copy(
                        title = cleanTitle,
                        desc = desc.trim(),
                        priority = priority,
                        dueDate = dueDate
                    )
                } else {
                    it
                }
            }
        )
        return true
    }

    /** Removes every finished task and returns how many were removed. */
    fun clearCompleted(): Int {
        val remaining = tasks.filterNot { it.completed }
        val removed = tasks.size - remaining.size
        if (removed > 0) update(remaining)
        return removed
    }

    fun toggle(id: String) {
        update(tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it })
    }

    fun delete(id: String) {
        update(tasks.filterNot { it.id == id })
    }

    private fun update(newTasks: List<Task>) {
        tasks = newTasks
        repository.save(newTasks)
    }

    companion object {
        @Volatile
        private var instance: TaskStore? = null

        fun get(context: Context): TaskStore =
            instance ?: synchronized(this) {
                instance ?: TaskStore(context.applicationContext).also { instance = it }
            }
    }
}
