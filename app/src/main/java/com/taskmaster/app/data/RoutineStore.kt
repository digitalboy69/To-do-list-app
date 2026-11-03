package com.taskmaster.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

/** Saved routines (templates), kept in SharedPreferences. Main thread only. */
class RoutineStore private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("taskmaster_prefs", Context.MODE_PRIVATE)

    var routines by mutableStateOf(load())
        private set

    private fun load(): List<Routine> {
        val raw = prefs.getString(KEY_ROUTINES, null) ?: return emptyList()
        return RoutineJson.parse(raw, keepIds = true)
    }

    /**
     * Creates a routine, or updates the one with [id]. Returns false (and changes nothing) when
     * the name is blank or there is not a single non-blank step.
     */
    fun save(id: String?, name: String, emoji: String, steps: List<String>): Boolean {
        val cleanName = name.trim().take(RoutineJson.MAX_NAME)
        val cleanSteps = steps
            .map { it.trim().take(RoutineJson.MAX_STEP) }
            .filter { it.isNotEmpty() }
            .take(RoutineJson.MAX_STEPS)
        if (cleanName.isEmpty() || cleanSteps.isEmpty()) return false
        val cleanEmoji = emoji.trim().ifEmpty { RoutineJson.DEFAULT_EMOJI }

        if (id == null) {
            val routine = Routine(UUID.randomUUID().toString(), cleanName, cleanEmoji, cleanSteps)
            update(routines + routine)
        } else {
            update(
                routines.map {
                    if (it.id == id) it.copy(name = cleanName, emoji = cleanEmoji, steps = cleanSteps)
                    else it
                }
            )
        }
        return true
    }

    fun delete(id: String) {
        update(routines.filterNot { it.id == id })
    }

    /** Adds the routines found in a shared file. Returns how many were added. */
    fun importFrom(text: String): Int {
        val room = RoutineJson.MAX_ROUTINES - routines.size
        val found = RoutineJson.parse(text, keepIds = false).take(maxOf(room, 0))
        if (found.isNotEmpty()) update(routines + found)
        return found.size
    }

    /** Adds the starter routines that are not there yet. Returns how many were added. */
    fun addExamples(): Int {
        val have = routines.map { it.name.lowercase() }.toSet()
        val fresh = EXAMPLES
            .filter { it.name.lowercase() !in have }
            .map { it.copy(id = UUID.randomUUID().toString()) }
        if (fresh.isNotEmpty()) update(routines + fresh)
        return fresh.size
    }

    private fun update(newRoutines: List<Routine>) {
        routines = newRoutines
        prefs.edit().putString(KEY_ROUTINES, RoutineJson.toJson(newRoutines)).apply()
    }

    companion object {
        private const val KEY_ROUTINES = "routines"

        private val EXAMPLES = listOf(
            Routine(
                "", "Take a shower", "🚿",
                listOf(
                    "Stand up",
                    "Find clean clothes",
                    "Put them where you will change",
                    "Get everything ready (towel, soap, shampoo)",
                    "Go to the shower"
                )
            ),
            Routine(
                "", "Morning start", "🌅",
                listOf(
                    "Sit up and put your feet on the floor",
                    "Drink a glass of water",
                    "Brush your teeth",
                    "Get dressed",
                    "Eat something small"
                )
            ),
            Routine(
                "", "Leave the house", "🚪",
                listOf(
                    "Phone",
                    "Keys",
                    "Wallet",
                    "Charger and headphones",
                    "Lights off, door locked"
                )
            ),
            Routine(
                "", "Quick tidy", "🧹",
                listOf(
                    "Set a 5 minute timer",
                    "Put the trash in the bin",
                    "Dishes into the sink",
                    "Clothes into the basket",
                    "Wipe one surface"
                )
            )
        )

        @Volatile
        private var instance: RoutineStore? = null

        fun get(context: Context): RoutineStore =
            instance ?: synchronized(this) {
                instance ?: RoutineStore(context.applicationContext).also { instance = it }
            }
    }
}
