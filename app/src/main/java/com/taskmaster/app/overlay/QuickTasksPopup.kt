package com.taskmaster.app.overlay

import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.data.Priority
import com.taskmaster.app.data.NoteStore
import com.taskmaster.app.data.Task
import com.taskmaster.app.data.TaskStore
import com.taskmaster.app.ui.BrainDumpPanel
import com.taskmaster.app.ui.Checkbox
import com.taskmaster.app.ui.PriorityButton
import com.taskmaster.app.ui.SheetTextField
import com.taskmaster.app.ui.color
import com.taskmaster.app.ui.nowStamp
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceModal
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted

/** Visible circle, and the (slightly larger) window that leaves room for the shadow and badge. */
const val BUBBLE_DP = 52
const val BUBBLE_WINDOW_DP = 64

/* ------------------------------------------------------------------ */
/* The floating bubble                                                 */
/* ------------------------------------------------------------------ */

@Composable
fun BubbleContent(store: TaskStore) {
    val pending = store.tasks.count { !it.completed }

    Box(
        modifier = Modifier.size(BUBBLE_WINDOW_DP.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(BUBBLE_DP.dp)
                .shadow(8.dp, CircleShape, ambientColor = Primary, spotColor = Primary)
                .clip(CircleShape)
                .background(PrimaryGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Open quick tasks",
                tint = TextMain,
                modifier = Modifier.size(26.dp)
            )
        }

        if (pending > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
                    .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                    .background(HighPri, CircleShape)
                    .padding(horizontal = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (pending > 99) "99+" else pending.toString(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* The popup that opens when the bubble is tapped                      */
/* ------------------------------------------------------------------ */

enum class PopupTab { TASKS, DUMP }

/**
 * Tall popup with two tabs: Tasks (add / tick) and Brain dump (capture any thought with its date
 * and time). It opens on the tab used last, or straight on Brain dump with the keyboard up.
 */
@Composable
fun QuickPopup(
    tasks: TaskStore,
    notes: NoteStore,
    startTab: PopupTab,
    onTabChange: (PopupTab) -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit
) {
    var tab by remember { mutableStateOf(startTab) }

    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(220),
        label = "popupIn"
    )

    val pending = tasks.tasks.count { !it.completed }
    val noRipple = remember { MutableInteractionSource() }
    val panelShape = RoundedCornerShape(28.dp)

    // Tapping the dimmed area closes the popup.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f * progress))
            .clickable(interactionSource = noRipple, indication = null, onClick = onClose),
        contentAlignment = Alignment.TopCenter
    ) {
        // Anchored to the top and sized to the window, so the keyboard shrinks it instead of
        // covering the capture box.
        Column(
            modifier = Modifier
                .padding(top = 40.dp, start = 12.dp, end = 12.dp, bottom = 12.dp)
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer {
                    alpha = progress
                    translationY = (1f - progress) * -24.dp.toPx()
                }
                .shadow(24.dp, panelShape)
                .clip(panelShape)
                .background(SurfaceModal)
                .border(1.dp, BorderColor, panelShape)
                // Swallow taps so touching the panel itself does not dismiss it.
                .clickable(interactionSource = noRipple, indication = null, onClick = {})
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TaskMaster",
                        style = TextStyle(
                            brush = PrimaryGradient,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                    )
                    Text(
                        text = nowStamp(),
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceInput)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            val tabsShape = RoundedCornerShape(16.dp)
            val pill by animateFloatAsState(
                targetValue = if (tab == PopupTab.DUMP) 1f else 0f,
                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                label = "tabPill"
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tabsShape)
                    .background(SurfaceInput)
                    .padding(4.dp)
            ) {
                // One gradient pill slides between the tabs.
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(42.dp)
                        .graphicsLayer { translationX = size.width * pill }
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryGradient)
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    PopupTabButton(
                        label = "Tasks",
                        count = pending,
                        selected = tab == PopupTab.TASKS,
                        onClick = {
                            tab = PopupTab.TASKS
                            onTabChange(PopupTab.TASKS)
                        }
                    )
                    PopupTabButton(
                        label = "Brain dump",
                        count = notes.notes.size,
                        selected = tab == PopupTab.DUMP,
                        onClick = {
                            tab = PopupTab.DUMP
                            onTabChange(PopupTab.DUMP)
                        }
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(
                    targetState = tab,
                    animationSpec = tween(220),
                    label = "popupTab"
                ) { current ->
                    if (current == PopupTab.TASKS) {
                        TasksTab(store = tasks)
                    } else {
                        BrainDumpPanel(
                            notes = notes,
                            tasks = tasks,
                            autoFocus = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onOpenApp)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Open TaskMaster",
                    color = PrimaryLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RowScope.PopupTabButton(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier = Modifier
            .weight(1f)
            .height(42.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            color = if (selected) TextMain else TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (count > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (count > 99) "99+" else count.toString(),
                color = if (selected) TextMain else PrimaryLight,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(
                        if (selected) Color.White.copy(alpha = 0.22f) else SurfaceModal,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun TasksTab(store: TaskStore) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }

    // Unfinished tasks first; the sort is stable, so each group keeps its newest-first order.
    val tasks = store.tasks.sortedBy { it.completed }

    fun submit() {
        if (store.add(title, "", priority)) title = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SheetTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Add a task...",
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.weight(1f)
            )
            val addShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, addShape, ambientColor = Primary, spotColor = Primary)
                    .clip(addShape)
                    .background(PrimaryGradient)
                    .clickable { submit() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add task",
                    tint = TextMain,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Priority.entries.forEach { p ->
                PriorityButton(
                    priority = p,
                    selected = p == priority,
                    onClick = { priority = p },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            Text(
                text = "No tasks yet. Add your first one above.",
                color = TextMuted,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 18.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(tasks, key = { it.id }) { task ->
                    QuickTaskRow(task = task, onToggle = { store.toggle(task.id) })
                }
            }
        }
    }
}

@Composable
private fun QuickTaskRow(task: Task, onToggle: () -> Unit) {
    val titleColor by animateColorAsState(
        targetValue = if (task.completed) TextMuted else TextMain,
        animationSpec = tween(300),
        label = "quickTitle"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.completed, onClick = onToggle)
        Spacer(Modifier.width(14.dp))
        Text(
            text = task.title,
            color = titleColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textDecoration = if (task.completed) TextDecoration.LineThrough else null,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(task.priority.color(), CircleShape)
        )
    }
}
