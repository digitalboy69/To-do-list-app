package com.taskmaster.app.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A reusable template: a big task broken into a short sequence of small steps
 * ("Take a shower" -> stand up, find clothes, ...).
 */
data class Routine(
    val id: String,
    val name: String,
    val emoji: String,
    val steps: List<String>
)

/**
 * Reads and writes routines as JSON. The same format is used for on-device storage and for the
 * `.taskmaster` files people share with each other:
 *
 * ```
 * { "format": "taskmaster-routines", "version": 1,
 *   "routines": [ { "name": "...", "emoji": "...", "steps": ["...", "..."] } ] }
 * ```
 */
object RoutineJson {
    const val FORMAT = "taskmaster-routines"
    const val FILE_EXTENSION = "taskmaster"

    // Limits keep imported files sane; shared files come from other people.
    const val MAX_ROUTINES = 100
    const val MAX_STEPS = 100
    const val MAX_NAME = 60
    const val MAX_STEP = 200
    const val MAX_FILE_CHARS = 1_000_000

    fun toJson(routines: List<Routine>): String {
        val array = JSONArray()
        routines.forEach { r ->
            array.put(
                JSONObject().apply {
                    put("id", r.id)
                    put("name", r.name)
                    put("emoji", r.emoji)
                    put("steps", JSONArray(r.steps))
                }
            )
        }
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", 1)
            put("routines", array)
        }.toString(2)
    }

    /**
     * Parses routines from JSON text. Anything malformed is skipped, and a completely invalid
     * file gives an empty list. With [keepIds] false (importing) every routine gets a new id.
     */
    fun parse(text: String, keepIds: Boolean): List<Routine> {
        try {
            val trimmed = text.trim()
            val array = if (trimmed.startsWith("[")) {
                JSONArray(trimmed)
            } else {
                JSONObject(trimmed).optJSONArray("routines") ?: return emptyList()
            }
            val result = ArrayList<Routine>()
            for (i in 0 until minOf(array.length(), MAX_ROUTINES)) {
                val o = array.optJSONObject(i) ?: continue
                val name = o.optString("name", "").trim().take(MAX_NAME)
                val stepArray = o.optJSONArray("steps") ?: continue
                val steps = ArrayList<String>()
                for (j in 0 until minOf(stepArray.length(), MAX_STEPS)) {
                    val step = stepArray.optString(j, "").trim().take(MAX_STEP)
                    if (step.isNotEmpty()) steps.add(step)
                }
                if (name.isEmpty() || steps.isEmpty()) continue
                val emoji = o.optString("emoji", "").trim().take(8).ifEmpty { DEFAULT_EMOJI }
                val id = if (keepIds) o.optString("id", "") else ""
                result.add(
                    Routine(
                        id = id.ifEmpty { UUID.randomUUID().toString() },
                        name = name,
                        emoji = emoji,
                        steps = steps
                    )
                )
            }
            return result
        } catch (e: Exception) {
            return emptyList()
        }
    }

    const val DEFAULT_EMOJI = "✨"
}
