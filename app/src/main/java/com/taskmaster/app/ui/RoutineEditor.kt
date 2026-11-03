package com.taskmaster.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.data.Routine
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceMain
import com.taskmaster.app.ui.theme.SurfaceModal
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import java.util.Collections

private val EMOJIS = listOf("🚿", "🌅", "🚪", "🧹", "🍳", "💊", "🎒", "📚", "💪", "🛏️", "🧺", "✨")

/**
 * Create or edit a routine. [onSave] returns false when something is missing (the editor then
 * stays open); [onDelete] is null for a new routine.
 */
@Composable
fun RoutineEditor(
    initial: Routine?,
    onBack: () -> Unit,
    onSave: (name: String, emoji: String, steps: List<String>) -> Boolean,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val key = initial?.id ?: "new"
    var name by rememberSaveable(key) { mutableStateOf(initial?.name ?: "") }
    var emoji by rememberSaveable(key) { mutableStateOf(initial?.emoji ?: "✨") }
    var steps by rememberSaveable(key) {
        mutableStateOf(ArrayList(initial?.steps ?: listOf("", "")))
    }
    var confirmDelete by rememberSaveable(key) { mutableStateOf(false) }

    fun setStep(index: Int, text: String) {
        steps = ArrayList(steps).also { it[index] = text }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SurfaceMain) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 25.dp, end = 25.dp, top = 24.dp, bottom = 8.dp),
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
                Text(
                    text = if (initial == null) "New routine" else "Edit routine",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    style = TextStyle(
                        brush = PrimaryGradient,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
                if (onDelete != null) {
                    Box(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(HighPri.copy(alpha = 0.15f))
                            .clickable { confirmDelete = true }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Delete", color = HighPri, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Label("Name")
            SheetTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                placeholder = "e.g. Take a shower",
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.padding(horizontal = 25.dp)
            )

            Label("Icon")
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 25.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EMOJIS.forEach { e ->
                    val selected = e == emoji
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(shape)
                            .background(if (selected) Primary.copy(alpha = 0.25f) else SurfaceCard)
                            .border(1.dp, if (selected) PrimaryLight else BorderColor, shape)
                            .clickable { emoji = e },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = 22.sp)
                    }
                }
            }

            Label("Steps")
            Text(
                text = "Keep each step tiny and obvious, so you never have to decide what comes next.",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 25.dp, end = 25.dp, bottom = 10.dp)
            )
            Column(
                modifier = Modifier.padding(horizontal = 25.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEachIndexed { index, text ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(PrimaryGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${index + 1}", color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        SheetTextField(
                            value = text,
                            onValueChange = { setStep(index, it.take(200)) },
                            placeholder = "Step ${index + 1}",
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable(enabled = index > 0) {
                                    steps = ArrayList(steps).also { Collections.swap(it, index, index - 1) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "↑",
                                color = if (index > 0) PrimaryLight else TextMuted.copy(alpha = 0.3f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable {
                                    steps = if (steps.size <= 1) {
                                        arrayListOf("")
                                    } else {
                                        ArrayList(steps).also { it.removeAt(index) }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Remove step",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .padding(start = 25.dp, end = 25.dp, top = 14.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                    .clickable {
                        if (steps.size < 100) steps = ArrayList(steps).also { it.add("") }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("+  Add step", color = PrimaryLight, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            Box(modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 24.dp)) {
                BigButton(text = "Save routine", gradient = true) {
                    if (!onSave(name, emoji, steps.toList())) {
                        Toast.makeText(
                            context,
                            "Add a name and at least one step",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    if (confirmDelete && onDelete != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurfaceModal,
            titleContentColor = TextMain,
            textContentColor = TextMuted,
            title = { Text("Delete this routine?", fontWeight = FontWeight.SemiBold) },
            text = { Text("\"${initial?.name ?: ""}\" will be removed. Tasks you already made from it stay.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = HighPri, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text = text.uppercase(),
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 25.dp, end = 25.dp, top = 20.dp, bottom = 10.dp)
    )
}
