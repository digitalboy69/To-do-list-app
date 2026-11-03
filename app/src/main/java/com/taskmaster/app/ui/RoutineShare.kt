package com.taskmaster.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.taskmaster.app.data.Routine
import com.taskmaster.app.data.RoutineJson
import java.io.File

/** Shares a routine as a small `.taskmaster` file through the Android share sheet. */
internal fun shareRoutine(context: Context, routine: Routine) {
    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
    dir.listFiles()?.forEach { it.delete() }

    val safeName = routine.name
        .replace(Regex("[^A-Za-z0-9]+"), "-")
        .trim('-')
        .take(40)
        .ifEmpty { "routine" }
    val file = File(dir, "$safeName.${RoutineJson.FILE_EXTENSION}")
    file.writeText(RoutineJson.toJson(listOf(routine)))

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "TaskMaster routine: ${routine.name}")
        putExtra(
            Intent.EXTRA_TEXT,
            "A TaskMaster routine: ${routine.name}. " +
                "Save the file, then open TaskMaster > Routines > Import and pick it."
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, "Share routine").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

/** Reads a picked file as text, or returns null when it cannot be read or is too large. */
internal fun readSharedFile(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            val out = StringBuilder()
            val buffer = CharArray(4096)
            while (true) {
                val n = reader.read(buffer)
                if (n < 0) break
                out.append(buffer, 0, n)
                if (out.length > RoutineJson.MAX_FILE_CHARS) return null
            }
            out.toString()
        }
    } catch (e: Exception) {
        null
    }
}
