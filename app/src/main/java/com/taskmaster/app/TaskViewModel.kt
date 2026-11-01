package com.taskmaster.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.taskmaster.app.data.Priority
import com.taskmaster.app.data.Task
import com.taskmaster.app.data.TaskFilter
import com.taskmaster.app.data.TaskStore

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    // Shared with the floating bubble popup, so both always show the same tasks.
    private val store = TaskStore.get(app)

    val tasks: List<Task>
        get() = store.tasks

    var filter by mutableStateOf(TaskFilter.ALL)
        private set

    val visibleTasks: List<Task>
        get() = when (filter) {
            TaskFilter.ALL -> tasks
            TaskFilter.PENDING -> tasks.filter { !it.completed }
            TaskFilter.COMPLETED -> tasks.filter { it.completed }
        }

    fun setFilter(newFilter: TaskFilter) {
        filter = newFilter
    }

    /** Returns false when the title is blank (nothing is created). */
    fun addTask(title: String, desc: String, priority: Priority): Boolean =
        store.add(title, desc, priority)

    fun toggleTask(id: String) = store.toggle(id)

    fun deleteTask(id: String) = store.delete(id)
}
