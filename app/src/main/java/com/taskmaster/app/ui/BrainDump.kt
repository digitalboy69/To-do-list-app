package com.taskmaster.app.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.data.Note
import com.taskmaster.app.data.NoteCategory
import com.taskmaster.app.data.NoteStore
import com.taskmaster.app.data.TaskStore
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.Placeholder
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceInputFocused
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import kotlinx.coroutines.delay

/**
 * The whole brain-dump experience in one scrolling list: a big capture box with auto-detected
 * category and exact date/time, category filters, search, and every saved note grouped by day.
 *
 * Used by the floating popup and by the full Brain dump screen, so they behave identically.
 * It deliberately uses no dialogs, because the popup runs in an overlay window.
 */
@Composable
fun BrainDumpPanel(
    notes: NoteStore,
    tasks: TaskStore,
    autoFocus: Boolean,
    modifier: Modifier = Modifier
) {
    var picked by remember { mutableStateOf<NoteCategory?>(null) }
    var filter by remember { mutableStateOf<NoteCategory?>(null) }
    var query by remember { mutableStateOf("") }
    var justSaved by remember { mutableStateOf(false) }
    var stamp by remember { mutableStateOf(nowStamp()) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Keep the shown time current while the panel stays open.
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            stamp = nowStamp()
        }
    }
    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            delay(250)
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }
    LaunchedEffect(justSaved) {
        if (justSaved) {
            delay(1400)
            justSaved = false
        }
    }

    val text = notes.draft
    val category = picked ?: NoteCategory.guess(text)
    val all = notes.notes
    val q = query.trim()
    val shown = all.filter {
        (filter == null || it.category == filter) &&
            (q.isEmpty() || it.text.contains(q, ignoreCase = true))
    }
    val rows = remember(shown) { groupNotes(shown) }

    fun save() {
        if (notes.add(text, category)) {
            notes.updateDraft("")
            picked = null
            justSaved = true
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "capture") {
            val shape = RoundedCornerShape(22.dp)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(SurfaceCard)
                    .border(1.dp, CardBorderBrush, shape)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🕐  $stamp",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    if (text.isNotBlank()) {
                        val words = text.trim().split(Regex("\\s+")).size
                        Text("$words ${if (words == 1) "word" else "words"}", color = Placeholder, fontSize = 11.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
                NoteTextField(
                    value = text,
                    onValueChange = notes::updateDraft,
                    placeholder = "Dump it here. An idea, a worry, a link, anything on your mind...",
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Spacer(Modifier.height(10.dp))
                CategoryChips(selected = category, onSelect = { picked = it })
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = when {
                            justSaved -> "Saved to your brain dump"
                            text.isBlank() -> "Saved with today's date and time"
                            picked == null -> "Sorted as ${category.label}. Tap a category to change"
                            else -> "Filed under ${category.label}"
                        },
                        color = if (justSaved) Color(0xFF10B981) else TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    val saveShape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .bounceClick(
                                pressedScale = 0.92f,
                                enabled = text.isNotBlank(),
                                onClick = { save() }
                            )
                            .height(46.dp)
                            .clip(saveShape)
                            .then(
                                if (text.isBlank()) Modifier.background(SurfaceInput)
                                else Modifier.background(PrimaryGradient)
                            )
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (justSaved) "Saved ✓" else "Save",
                            color = if (text.isBlank()) Placeholder else TextMain,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (all.isNotEmpty()) {
            item(key = "filters") {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterPill(
                        label = "All  ${all.size}",
                        color = PrimaryLight,
                        selected = filter == null,
                        onClick = { filter = null }
                    )
                    NoteCategory.entries.forEach { c ->
                        val count = all.count { it.category == c }
                        if (count > 0 || filter == c) {
                            FilterPill(
                                label = "${c.emoji} ${c.label}  $count",
                                color = Color(c.color),
                                selected = filter == c,
                                onClick = { filter = if (filter == c) null else c }
                            )
                        }
                    }
                }
            }
        }

        if (all.size >= 4) {
            item(key = "search") {
                SheetTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search your notes",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                )
            }
        }

        if (all.isEmpty()) {
            item(key = "empty") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧠", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "Your head is not a storage unit",
                        color = TextMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Dump every idea above the moment it appears. Each one is saved with its date and time, sorted for you, and can become a task later.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (shown.isEmpty()) {
            item(key = "nomatch") {
                Text(
                    text = "No notes match.",
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        } else {
            itemsIndexed(rows, key = { _, r -> r.key }) { index, item ->
                when (item) {
                    is DumpItem.Header -> Text(
                        text = item.label.uppercase(),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                    )

                    is DumpItem.Entry -> NoteRow(
                        modifier = Modifier.popIn(index),
                        note = item.note,
                        onPin = { notes.togglePin(item.note.id) },
                        onTask = { notes.toTask(item.note.id, tasks) },
                        onDelete = { notes.delete(item.note.id) },
                        onEdit = { newText, newCategory ->
                            notes.edit(item.note.id, newText, newCategory)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 15.sp) },
        textStyle = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
        minLines = 3,
        maxLines = 8,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextMain,
            unfocusedTextColor = TextMain,
            focusedContainerColor = SurfaceInputFocused,
            unfocusedContainerColor = SurfaceInput,
            cursorColor = PrimaryLight,
            focusedBorderColor = PrimaryLight,
            unfocusedBorderColor = BorderColor,
            focusedPlaceholderColor = Placeholder,
            unfocusedPlaceholderColor = Placeholder
        )
    )
}

@Composable
private fun CategoryChips(selected: NoteCategory, onSelect: (NoteCategory) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NoteCategory.entries.forEach { c ->
            val color = Color(c.color)
            val on = c == selected
            val shape = RoundedCornerShape(12.dp)
            Row(
                modifier = Modifier
                    .bounceClick(pressedScale = 0.92f, onClick = { onSelect(c) })
                    .clip(shape)
                    .background(if (on) color.copy(alpha = 0.22f) else SurfaceInput)
                    .border(1.dp, if (on) color else BorderColor, shape)
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(c.emoji, fontSize = 13.sp)
                Spacer(Modifier.width(5.dp))
                Text(
                    text = c.label,
                    color = if (on) TextMain else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Text(
        text = label,
        color = if (selected) TextMain else TextMuted,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .bounceClick(pressedScale = 0.92f, onClick = onClick)
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.25f) else SurfaceCard)
            .border(1.dp, if (selected) color else BorderColor, shape)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}

@Composable
private fun NoteRow(
    modifier: Modifier = Modifier,
    note: Note,
    onPin: () -> Unit,
    onTask: () -> Unit,
    onDelete: () -> Unit,
    onEdit: (String, NoteCategory) -> Boolean
) {
    val accent = Color(note.category.color)
    var expanded by remember(note.id) { mutableStateOf(false) }
    var editing by remember(note.id) { mutableStateOf(false) }
    var confirmDelete by remember(note.id) { mutableStateOf(false) }
    LaunchedEffect(confirmDelete) {
        if (confirmDelete) {
            delay(3000)
            confirmDelete = false
        }
    }

    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clip(shape)
            .background(SurfaceCard)
            .drawBehind { drawRect(accent, size = Size(4.dp.toPx(), size.height)) }
            .border(1.dp, CardBorderBrush, shape)
            .padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = "${note.category.emoji} ${note.category.label}",
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(noteStamp(note.createdAt), color = TextMuted, fontSize = 11.sp)
                val ago = noteAgo(note.createdAt)
                if (ago != null) Text(ago, color = Placeholder, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(8.dp))

        if (editing) {
            var editText by remember(note.id, editing) { mutableStateOf(note.text) }
            var editCategory by remember(note.id, editing) { mutableStateOf(note.category) }
            NoteTextField(value = editText, onValueChange = { editText = it }, placeholder = "")
            Spacer(Modifier.height(8.dp))
            CategoryChips(selected = editCategory, onSelect = { editCategory = it })
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NotePill("Save", PrimaryLight, filled = true) {
                    if (onEdit(editText, editCategory)) editing = false
                }
                NotePill("Cancel", TextMuted, filled = false) { editing = false }
            }
        } else {
            Text(
                text = note.text,
                color = TextMain,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                NotePill(
                    text = if (note.pinned) "Unpin" else "Pin",
                    color = if (note.pinned) accent else TextMuted,
                    filled = note.pinned,
                    onClick = onPin
                )
                NotePill(
                    text = if (note.tasked) "In tasks ✓" else "To task",
                    color = if (note.tasked) Color(0xFF10B981) else PrimaryLight,
                    filled = false,
                    enabled = !note.tasked,
                    onClick = onTask
                )
                NotePill("Edit", TextMuted, filled = false) { editing = true }
                NotePill(
                    text = if (confirmDelete) "Sure?" else "Delete",
                    color = HighPri,
                    filled = confirmDelete
                ) {
                    if (confirmDelete) onDelete() else confirmDelete = true
                }
            }
        }
    }
}

@Composable
private fun RowScope.NotePill(
    text: String,
    color: Color,
    filled: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .weight(1f, fill = false)
            .bounceClick(pressedScale = 0.9f, enabled = enabled, onClick = onClick)
            .clip(shape)
            .background(if (filled) color.copy(alpha = 0.2f) else SurfaceInput)
            .border(1.dp, if (filled) color else BorderColor, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) color else color.copy(alpha = 0.7f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
