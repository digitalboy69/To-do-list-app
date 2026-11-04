package com.taskmaster.app.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.TaskViewModel
import com.taskmaster.app.data.Priority
import com.taskmaster.app.data.Routine
import com.taskmaster.app.data.RoutineStore
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceMain
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted

/** Fades and lifts a full-screen page in when it first appears. */
@Composable
internal fun SlideInPage(content: @Composable () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(300)) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * 48.dp.toPx()
            }
    ) {
        content()
    }
}

/** All saved routines, with start / add-to-tasks / share on each and import at the top. */
@Composable
fun RoutinesScreen(
    store: RoutineStore,
    vm: TaskViewModel,
    onBack: () -> Unit,
    onNew: () -> Unit,
    onEdit: (Routine) -> Unit,
    onStart: (Routine) -> Unit
) {
    val context = LocalContext.current
    val routines = store.routines

    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val text = readSharedFile(context, uri)
        val added = if (text == null) 0 else store.importFrom(text)
        val message = when {
            added > 0 -> "Imported $added ${if (added == 1) "routine" else "routines"}"
            else -> "That file has no TaskMaster routines"
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = SurfaceMain) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 25.dp, end = 25.dp, top = 24.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SquareButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextMain,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "Routines",
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    style = TextStyle(
                        brush = PrimaryGradient,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    )
                )
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceCard)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .clickable { importer.launch(arrayOf("*/*")) }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Import", color = PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryGradient)
                        .clickable(onClick = onNew),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "New routine",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = "Break a big task into tiny steps, then do them one at a time.",
                color = TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 25.dp, end = 25.dp, bottom = 14.dp)
            )

            if (routines.isEmpty()) {
                EmptyRoutines(
                    onExamples = {
                        val n = store.addExamples()
                        Toast.makeText(context, "Added $n example routines", Toast.LENGTH_SHORT).show()
                    },
                    onNew = onNew
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 25.dp, end = 25.dp, bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(routines, key = { _, r -> r.id }) { index, routine ->
                        RoutineCard(
                            modifier = Modifier.popIn(index),
                            routine = routine,
                            onEdit = { onEdit(routine) },
                            onStart = { onStart(routine) },
                            onAddToTasks = {
                                // Added last-to-first so step 1 ends up at the top of the list.
                                routine.steps.asReversed().forEach { step ->
                                    vm.addTask(step, "", Priority.MEDIUM, null)
                                }
                                Toast.makeText(
                                    context,
                                    "Added ${routine.steps.size} steps to your tasks",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            onShare = {
                                try {
                                    shareRoutine(context, routine)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not share this routine", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SquareButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderColor, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun EmptyRoutines(onExamples: () -> Unit, onNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🪄", fontSize = 56.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No routines yet",
            color = TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "A routine turns something big, like taking a shower, into a few small steps you can follow without thinking.",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        BigButton(text = "Add example routines", gradient = true, onClick = onExamples)
        Spacer(Modifier.height(12.dp))
        BigButton(text = "Create my own", gradient = false, onClick = onNew)
    }
}

@Composable
internal fun BigButton(text: String, gradient: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .bounceClick(pressedScale = 0.96f, onClick = onClick)
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .then(
                if (gradient) Modifier.background(PrimaryGradient)
                else Modifier.background(SurfaceCard).border(1.dp, BorderColor, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = TextMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun RoutineCard(
    modifier: Modifier = Modifier,
    routine: Routine,
    onEdit: () -> Unit,
    onStart: () -> Unit,
    onAddToTasks: () -> Unit,
    onShare: () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .bounceClick(pressedScale = 0.98f, onClick = onEdit)
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, CardBorderBrush, shape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(routine.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routine.name,
                    color = TextMain,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${routine.steps.size} ${if (routine.steps.size == 1) "step" else "steps"}  ·  tap to edit",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        routine.steps.take(3).forEachIndexed { i, step ->
            Text(
                text = "${i + 1}.  $step",
                color = TextMuted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
        if (routine.steps.size > 3) {
            Text(
                text = "+ ${routine.steps.size - 3} more",
                color = PrimaryLight,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CardAction(text = "▶  Start", primary = true, weight = 1.3f, onClick = onStart)
            CardAction(text = "To tasks", primary = false, weight = 1f, onClick = onAddToTasks)
            CardAction(text = "Share", primary = false, weight = 1f, onClick = onShare)
        }
    }
}

@Composable
private fun RowScope.CardAction(text: String, primary: Boolean, weight: Float, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .weight(weight)
            .bounceClick(pressedScale = 0.92f, onClick = onClick)
            .height(42.dp)
            .clip(shape)
            .then(
                if (primary) Modifier.background(PrimaryGradient)
                else Modifier.background(SurfaceInput).border(1.dp, BorderColor, shape)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (primary) Color.White else TextMain,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}
