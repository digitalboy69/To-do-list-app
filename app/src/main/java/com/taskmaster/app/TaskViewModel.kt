package com.taskmaster.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.taskmaster.app.data.Priority
import com.taskmaster.app.data.SortOrder
import com.taskmaster.app.data.Task
import com.taskmaster.app.data.TaskFilter
import com.taskmaster.app.data.TaskStore

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    // Shared with the floating bubble popup, so both always show the same tasks.
    private val store = TaskStore.get(app)
    private val prefs = app.getSharedPreferences("taskmaster_prefs", Context.MODE_PRIVATE)

    val tasks: List<Task>
        get() = store.tasks

    var filter by mutableStateOf(TaskFilter.ALL)
        private set

    var query by mutableStateOf("")
        private set

    var sortOrder by mutableStateOf(SortOrder.fromName(prefs.getString(KEY_SORT, null)))
        private set

    /** Filter tab, then search text, then sort order. Finished tasks always sink to the bottom. */
    val visibleTasks: List<Task>
        get() {
            val byFilter = when (filter) {
                TaskFilter.ALL -> tasks
                TaskFilter.PENDING -> tasks.filter { !it.completed }
                TaskFilter.COMPLETED -> tasks.filter { it.completed }
            }
            val text = query.trim()
            val searched = if (text.isEmpty()) {
                byFilter
            } else {
                byFilter.filter {
                    it.title.contains(text, ignoreCase = true) ||
                        it.desc.contains(text, ignoreCase = true)
                }
            }
            val order: Comparator<Task> = when (sortOrder) {
                SortOrder.NEWEST -> compareByDescending<Task> { it.createdAt }
                SortOrder.PRIORITY -> compareBy<Task> { it.priority.ordinal }
                    .thenByDescending { it.createdAt }
                SortOrder.DUE -> compareBy<Task> { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            }
            return searched.sortedWith(compareBy<Task> { it.completed }.then(order))
        }

    fun selectFilter(newFilter: TaskFilter) {
        filter = newFilter
    }

    fun updateQuery(text: String) {
        query = text
    }

    fun selectSort(order: SortOrder) {
        sortOrder = order
        prefs.edit().putString(KEY_SORT, order.name).apply()
    }

    /** Returns false when the title is blank (nothing is created). */
    fun addTask(title: String, desc: String, priority: Priority, dueDate: Long?): Boolean =
        store.add(title, desc, priority, dueDate)

    /** Returns false when the title is blank (nothing is changed). */
    fun editTask(
        id: String,
        title: String,
        desc: String,
        priority: Priority,
        dueDate: Long?
    ): Boolean = store.edit(id, title, desc, priority, dueDate)

    fun toggleTask(id: String) = store.toggle(id)

    fun deleteTask(id: String) = store.delete(id)

    fun clearCompleted(): Int = store.clearCompleted()

    private companion object {
        const val KEY_SORT = "sort_order"
    }
}
