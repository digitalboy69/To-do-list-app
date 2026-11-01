package com.taskmaster.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.taskmaster.app.data.Priority
import com.taskmaster.app.data.Task
import com.taskmaster.app.data.TaskFilter
import com.taskmaster.app.data.TaskRepository
import java.util.UUID

class TaskViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = TaskRepository(app)

    var tasks by mutableStateOf(repository.load())
        private set

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
    fun addTask(title: String, desc: String, priority: Priority): Boolean {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return false
        val task = Task(
            id = UUID.randomUUID().toString(),
            title = cleanTitle,
            desc = desc.trim(),
            priority = priority,
            completed = false,
            createdAt = System.currentTimeMillis()
        )
        update(listOf(task) + tasks)
        return true
    }

    fun toggleTask(id: String) {
        update(tasks.map { if (it.id == id) it.copy(completed = !it.completed) else it })
    }

    fun deleteTask(id: String) {
        update(tasks.filterNot { it.id == id })
    }

    private fun update(newTasks: List<Task>) {
        tasks = newTasks
        repository.save(newTasks)
    }
}
