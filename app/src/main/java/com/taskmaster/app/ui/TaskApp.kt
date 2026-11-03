package com.taskmaster.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.taskmaster.app.TaskViewModel
import com.taskmaster.app.data.Priority
import com.taskmaster.app.data.RoutineStore
import com.taskmaster.app.data.Task
import com.taskmaster.app.data.TaskFilter
import com.taskmaster.app.ui.theme.BgOuter
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.CheckboxBorder
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.LowPri
import com.taskmaster.app.ui.theme.MedPri
import com.taskmaster.app.ui.theme.Placeholder
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceInputFocused
import com.taskmaster.app.ui.theme.SurfaceMain
import com.taskmaster.app.ui.theme.SurfaceModal
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun Priority.color(): Color = when (this) {
    Priority.HIGH -> HighPri
    Priority.MEDIUM -> MedPri
    Priority.LOW -> LowPri
}

/* ------------------------------------------------------------------ */
/* Root                                                                */
/* ------------------------------------------------------------------ */

@Composable
fun TaskApp(vm: TaskViewModel = viewModel()) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showRoutines by rememberSaveable { mutableStateOf(false) }
    // "" = no editor open, "new" = creating, otherwise the id of the routine being edited.
    var editorId by rememberSaveable { mutableStateOf("") }
    var runningId by rememberSaveable { mutableStateOf("") }

    val routineStore = RoutineStore.get(LocalContext.current)

    val updates = rememberUpdateState()
    LaunchedEffect(Unit) { updates.checkOnLaunch() }

    val editing = editingId?.let { id -> vm.tasks.firstOrNull { it.id == id } }
    val sheetOpen = showSheet || editing != null

    // Confetti plays when the last open task gets ticked off.
    val allDone = vm.tasks.isNotEmpty() && vm.tasks.all { it.completed }
    var confetti by remember { mutableIntStateOf(0) }
    var wasAllDone by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone && !wasAllDone) confetti++
        wasAllDone = allDone
    }

    BackHandler(enabled = showSettings) { showSettings = false }
    BackHandler(enabled = showRoutines) { showRoutines = false }
    BackHandler(enabled = editorId.isNotEmpty()) { editorId = "" }
    BackHandler(enabled = runningId.isNotEmpty()) { runningId = "" }

    // iOS-style "push back" of the main screen while the sheet is open.
    val push by animateFloatAsState(
        targetValue = if (sheetOpen) 1f else 0f,
        animationSpec = tween(400),
        label = "push"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgOuter)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val s = 1f - 0.08f * push
                    scaleX = s
                    scaleY = s
                    translationY = 10.dp.toPx() * push
                    alpha = 1f - 0.6f * push
                    transformOrigin = TransformOrigin(0.5f, 0f)
                },
            shape = RoundedCornerShape((30f * push).dp),
            color = SurfaceMain
        ) {
            MainScreen(
                vm = vm,
                onAddClick = { showSheet = true },
                onEdit = { task -> editingId = task.id },
                onSettingsClick = { showSettings = true },
                routineCount = routineStore.routines.size,
                onRoutinesClick = { showRoutines = true }
            )
        }

        if (sheetOpen) {
            AddTaskSheet(
                initial = editing,
                onDismiss = {
                    showSheet = false
                    editingId = null
                },
                onSave = { title, desc, priority, dueDate ->
                    val task = editing
                    if (task != null) {
                        vm.editTask(task.id, title, desc, priority, dueDate)
                    } else {
                        vm.addTask(title, desc, priority, dueDate)
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(320)),
            exit = slideOutHorizontally(tween(260)) { it } + fadeOut(tween(260))
        ) {
            SettingsScreen(vm = vm, updates = updates, onBack = { showSettings = false })
        }

        AnimatedVisibility(
            visible = showRoutines,
            enter = slideInHorizontally(tween(320)) { it } + fadeIn(tween(320)),
            exit = slideOutHorizontally(tween(260)) { it } + fadeOut(tween(260))
        ) {
            RoutinesScreen(
                store = routineStore,
                vm = vm,
                onBack = { showRoutines = false },
                onNew = { editorId = "new" },
                onEdit = { editorId = it.id },
                onStart = { runningId = it.id }
            )
        }

        if (editorId.isNotEmpty()) {
            val id = editorId
            val routine = routineStore.routines.firstOrNull { it.id == id }
            SlideInPage {
                RoutineEditor(
                    initial = routine,
                    onBack = { editorId = "" },
                    onSave = { name, emoji, steps ->
                        val saved = routineStore.save(routine?.id, name, emoji, steps)
                        if (saved) editorId = ""
                        saved
                    },
                    onDelete = routine?.let { r ->
                        {
                            routineStore.delete(r.id)
                            editorId = ""
                        }
                    }
                )
            }
        }

        if (runningId.isNotEmpty()) {
            val running = routineStore.routines.firstOrNull { it.id == runningId }
            if (running != null) {
                SlideInPage { RoutineRunner(routine = running, onClose = { runningId = "" }) }
            }
        }

        ConfettiBurst(trigger = confetti)

        // Asks to update when a newer release is published on GitHub.
        UpdateDialog(updates)
    }
}

/* ------------------------------------------------------------------ */
/* Main screen                                                         */
/* ------------------------------------------------------------------ */

@Composable
private fun MainScreen(
    vm: TaskViewModel,
    onAddClick: () -> Unit,
    onEdit: (Task) -> Unit,
    onSettingsClick: () -> Unit,
    routineCount: Int,
    onRoutinesClick: () -> Unit
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Header(
            searchOpen = searchOpen,
            onSearchClick = {
                searchOpen = !searchOpen
                if (!searchOpen) vm.updateQuery("")
            },
            onSettingsClick = onSettingsClick,
            onAddClick = onAddClick
        )
        AnimatedVisibility(visible = searchOpen) {
            SearchField(query = vm.query, onChange = vm::updateQuery)
        }
        ProgressCard(done = vm.tasks.count { it.completed }, total = vm.tasks.size)
        RoutinesEntryCard(count = routineCount, onClick = onRoutinesClick)
        Filters(selected = vm.filter, onSelect = vm::selectFilter)
        TaskList(
            tasks = vm.visibleTasks,
            searching = vm.query.isNotBlank(),
            onToggle = vm::toggleTask,
            onDelete = vm::deleteTask,
            onEdit = onEdit
        )
    }
}

@Composable
private fun Header(
    searchOpen: Boolean,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val today = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp, top = 24.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "My Tasks",
                style = TextStyle(
                    brush = PrimaryGradient,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            )
            Text(
                text = today,
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            HeaderIconButton(
                icon = Icons.Filled.Settings,
                description = "Settings",
                onClick = onSettingsClick
            )
            HeaderIconButton(
                icon = Icons.Filled.Search,
                description = "Search",
                highlighted = searchOpen,
                onClick = onSearchClick
            )

            val shape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .shadow(14.dp, shape, ambientColor = Primary, spotColor = Primary)
                    .clip(shape)
                    .background(PrimaryGradient)
                    .clickable(onClick = onAddClick),
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
    }
}

@Composable
private fun HeaderIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val shape = RoundedCornerShape(14.dp)
    val tint by animateColorAsState(
        targetValue = if (highlighted) PrimaryLight else TextMuted,
        animationSpec = tween(250),
        label = "headerIcon"
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
private fun Filters(selected: TaskFilter, onSelect: (TaskFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp, top = 10.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        TaskFilter.entries.forEach { f ->
            FilterTab(label = f.label, selected = f == selected, onClick = { onSelect(f) })
        }
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val fraction by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(400),
        label = "underline"
    )
    val color by animateColorAsState(
        targetValue = if (selected) TextMain else TextMuted,
        animationSpec = tween(300),
        label = "tabColor"
    )

    Column(
        modifier = Modifier
            // Size the tab to its label, so the underline (fillMaxWidth) matches the text
            // instead of stretching across the row and pushing the other tabs off screen.
            .width(IntrinsicSize.Max)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(3.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(PrimaryGradient)
            )
        }
    }
}

/* ------------------------------------------------------------------ */
/* Task list                                                           */
/* ------------------------------------------------------------------ */

@Composable
private fun TaskList(
    tasks: List<Task>,
    searching: Boolean,
    onToggle: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (Task) -> Unit
) {
    if (tasks.isEmpty()) {
        EmptyState(searching)
        return
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 25.dp,
            end = 25.dp,
            top = 10.dp,
            bottom = 30.dp + bottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(items = tasks, key = { _, task -> task.id }) { index, task ->
            // Each card fades and rises in, a little after the one above it. This only changes
            // how the card is drawn, never where it sits, so it cannot get stuck mid-screen.
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { shown = true }
            val entrance by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(
                    durationMillis = 450,
                    delayMillis = 60 * index.coerceAtMost(8),
                    easing = FastOutSlowInEasing
                ),
                label = "entrance"
            )
            TaskItem(
                task = task,
                onToggle = { onToggle(task.id) },
                onDelete = { onDelete(task.id) },
                onEdit = { onEdit(task) },
                modifier = Modifier.graphicsLayer {
                    alpha = entrance
                    translationY = (1f - entrance) * 36.dp.toPx()
                }
            )
        }
    }
}

@Composable
private fun EmptyState(searching: Boolean) {
    val floating by rememberInfiniteTransition(label = "moon").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "moonFloat"
    )
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🌙",
            fontSize = 54.sp,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .graphicsLayer {
                    alpha = 0.35f
                    translationY = (floating - 0.5f) * 16.dp.toPx()
                }
        )
        Text(
            text = if (searching) "Nothing matches your search." else "No tasks found.",
            color = TextMuted,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        Text(
            text = if (searching) "Try a different word." else "Time to chill and relax.",
            color = TextMuted.copy(alpha = 0.7f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

/** Label and colour for a due date: overdue is red, today amber, tomorrow accent. */
private fun dueInfo(epochDay: Long, completed: Boolean): Pair<String, Color> {
    val short = LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
    val diff = epochDay - LocalDate.now().toEpochDay()
    return when {
        completed -> "Due $short" to TextMuted
        diff < 0L -> "Overdue \u00B7 $short" to HighPri
        diff == 0L -> "Due today" to MedPri
        diff == 1L -> "Due tomorrow" to PrimaryLight
        else -> "Due $short" to TextMuted
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(20.dp)
    val titleColor by animateColorAsState(
        targetValue = if (task.completed) TextMuted else TextMain,
        animationSpec = tween(300),
        label = "titleColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(SurfaceCard)
            .border(1.dp, BorderColor, cardShape)
            .clickable(onClick = onEdit)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.completed, onClick = onToggle)

        Spacer(Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                color = titleColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.completed) TextDecoration.LineThrough else null
            )
            if (task.desc.isNotEmpty()) {
                Text(
                    text = task.desc,
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            val due = task.dueDate
            if (due != null) {
                val (dueText, dueColor) = dueInfo(due, task.completed)
                Text(
                    text = dueText,
                    color = dueColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier.padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dotColor = task.priority.color()
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .shadow(6.dp, CircleShape, ambientColor = dotColor, spotColor = dotColor)
                        .background(dotColor, CircleShape)
                )
                Text(
                    text = "${task.priority.label} Priority",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HighPri.copy(alpha = 0.1f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = "Delete task",
                tint = HighPri,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
internal fun Checkbox(checked: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val bounce = remember { Animatable(1f) }
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(checked) {
        if (firstRun) {
            firstRun = false
            return@LaunchedEffect
        }
        bounce.snapTo(0.7f)
        bounce.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    val iconProgress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(300),
        label = "check"
    )

    Box(
        modifier = Modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = bounce.value
                scaleY = bounce.value
            }
            .then(
                if (checked) {
                    Modifier.shadow(10.dp, shape, ambientColor = Primary, spotColor = Primary)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .then(
                if (checked) {
                    Modifier.background(PrimaryGradient)
                } else {
                    Modifier.border(2.dp, CheckboxBorder, shape)
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = if (checked) "Mark as pending" else "Mark as completed",
            tint = Color.White,
            modifier = Modifier
                .size(15.dp)
                .graphicsLayer {
                    alpha = iconProgress
                    val s = 0.5f + 0.5f * iconProgress
                    scaleX = s
                    scaleY = s
                }
        )
    }
}

/* ------------------------------------------------------------------ */
/* Add-task bottom sheet                                               */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskSheet(
    initial: Task?,
    onDismiss: () -> Unit,
    onSave: (title: String, desc: String, priority: Priority, dueDate: Long?) -> Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf(initial?.title ?: "") }
    var desc by rememberSaveable { mutableStateOf(initial?.desc ?: "") }
    var priority by rememberSaveable { mutableStateOf(initial?.priority ?: Priority.MEDIUM) }
    var dueDate by rememberSaveable { mutableStateOf(initial?.dueDate) }

    val shake = remember { Animatable(0f) }
    val titleFocus = remember { FocusRequester() }

    fun dismiss() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    fun submit() {
        if (onSave(title, desc, priority, dueDate)) {
            dismiss()
        } else {
            scope.launch {
                shake.animateTo(-8f, tween(75))
                shake.animateTo(8f, tween(75))
                shake.animateTo(0f, tween(75))
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(300) // focus after the sheet has slid up
        titleFocus.requestFocus()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceModal,
        shape = RoundedCornerShape(topStart = 35.dp, topEnd = 35.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 15.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(CheckboxBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 25.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (initial == null) "New Task" else "Edit Task",
                    color = TextMain,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { dismiss() },
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

            // Title
            FieldGroup(label = "What are you planning?") {
                SheetTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "e.g. Design new landing page...",
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier
                        .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                        .focusRequester(titleFocus)
                )
            }

            // Details
            FieldGroup(label = "Details (Optional)") {
                SheetTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    placeholder = "Add some extra notes here...",
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    modifier = Modifier.height(110.dp)
                )
            }

            // Priority
            FieldGroup(label = "Priority") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Priority.entries.forEach { p ->
                        PriorityButton(
                            priority = p,
                            selected = p == priority,
                            onClick = { priority = p },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Due date
            FieldGroup(label = "Due date") {
                DueChips(selected = dueDate, onSelect = { dueDate = it })
            }

            Spacer(Modifier.height(4.dp))

            // Create button
            val buttonShape = RoundedCornerShape(16.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(14.dp, buttonShape, ambientColor = Primary, spotColor = Primary)
                    .clip(buttonShape)
                    .background(PrimaryGradient)
                    .clickable { submit() }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (initial == null) "Create Task" else "Save Changes",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FieldGroup(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = label.uppercase(Locale.US),
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
internal fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    keyboardOptions: KeyboardOptions,
    modifier: Modifier = Modifier,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, fontSize = 15.sp) },
        textStyle = TextStyle(fontSize = 15.sp),
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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
internal fun PriorityButton(
    priority: Priority,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    val accent = priority.color()
    val background by animateColorAsState(
        targetValue = if (selected) accent else SurfaceInput,
        animationSpec = tween(300),
        label = "priBg"
    )
    val border by animateColorAsState(
        targetValue = if (selected) accent else BorderColor,
        animationSpec = tween(300),
        label = "priBorder"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextMuted,
        animationSpec = tween(300),
        label = "priText"
    )

    Box(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.shadow(10.dp, shape, ambientColor = accent, spotColor = accent)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = priority.label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
