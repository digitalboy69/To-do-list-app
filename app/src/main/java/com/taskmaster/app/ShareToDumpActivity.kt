package com.taskmaster.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.taskmaster.app.data.NoteCategory
import com.taskmaster.app.data.NoteStore

/**
 * Target of the system "Share" sheet. Whatever text or link another app shares is saved as a
 * brain-dump note (stamped with the current date and time) and the screen closes at once.
 */
class ShareToDumpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty().trim()
        val subject = intent?.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
        val full = if (subject.isNotEmpty() && !text.contains(subject)) {
            "$subject\n$text".trim()
        } else {
            text
        }

        val saved = full.isNotEmpty() && NoteStore.get(this).add(full, NoteCategory.guess(full))
        Toast.makeText(
            this,
            if (saved) "Saved to Brain dump" else "Nothing to save",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}
