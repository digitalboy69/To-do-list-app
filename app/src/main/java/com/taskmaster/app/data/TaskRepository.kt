package com.taskmaster.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Persists tasks as JSON in SharedPreferences (the native equivalent of localStorage). */
class TaskRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<Task> {
        val raw = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Task(
                    id = o.getString("id"),
                    title = o.getString("title"),
                    desc = o.optString("desc", ""),
                    priority = Priority.fromName(o.optString("priority", "medium")),
                    completed = o.optBoolean("completed", false),
                    createdAt = o.optLong("createdAt", 0L)
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { t ->
            array.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("desc", t.desc)
                    put("priority", t.priority.name.lowercase())
                    put("completed", t.completed)
                    put("createdAt", t.createdAt)
                }
            )
        }
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    private companion object {
        const val PREFS_NAME = "taskmaster_prefs"
        const val KEY_TASKS = "taskMaster_dark_tasks"
    }
}
