package com.taskmaster.app.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.data.NoteStore
import com.taskmaster.app.data.TaskStore
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceMain
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted

/** Full-screen Brain dump: the same panel as the popup, plus a title bar and export. */
@Composable
fun BrainDumpScreen(
    notes: NoteStore,
    tasks: TaskStore,
    autoFocus: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Surface(modifier = Modifier.fillMaxSize(), color = SurfaceMain) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 25.dp, end = 25.dp, top = 24.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val shape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(shape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderColor, shape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextMain,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Brain dump",
                        maxLines = 1,
                        style = TextStyle(
                            brush = PrimaryGradient,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        )
                    )
                    Text(
                        text = "${notes.notes.size} ${if (notes.notes.size == 1) "note" else "notes"}",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(shape)
                        .background(SurfaceCard)
                        .border(1.dp, BorderColor, shape)
                        .clickable { exportNotes(context, notes) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Export", color = PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(10.dp))
            BrainDumpPanel(
                notes = notes,
                tasks = tasks,
                autoFocus = autoFocus,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 25.dp)
            )
        }
    }
}

/** Sends every note as plain text, grouped by category, through the share sheet. */
private fun exportNotes(context: Context, notes: NoteStore) {
    if (notes.notes.isEmpty()) {
        Toast.makeText(context, "Nothing to export yet", Toast.LENGTH_SHORT).show()
        return
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "TaskMaster brain dump")
        putExtra(Intent.EXTRA_TEXT, exportNotesText(notes.notes))
    }
    context.startActivity(Intent.createChooser(send, "Export brain dump").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}
