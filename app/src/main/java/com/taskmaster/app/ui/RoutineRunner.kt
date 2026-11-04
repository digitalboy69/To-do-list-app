package com.taskmaster.app.ui

import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.data.Routine
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceMain
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted

/**
 * Runs a routine one step at a time: a single big instruction, a single big "Done" button, no
 * list to scan. Finishing the last step plays confetti.
 */
@Composable
fun RoutineRunner(routine: Routine, onClose: () -> Unit) {
    val steps = routine.steps
    var index by rememberSaveable(routine.id) { mutableIntStateOf(0) }
    val finished = index >= steps.size
    val haptic = LocalHapticFeedback.current

    // The screen stays on while a routine is running, so it never locks mid-step.
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val progress by animateFloatAsState(
        targetValue = if (steps.isEmpty()) 0f else (index.toFloat() / steps.size).coerceIn(0f, 1f),
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "runnerProgress"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = SurfaceMain) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 25.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(shape)
                            .background(SurfaceCard)
                            .border(1.dp, BorderColor, shape)
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = TextMain,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${routine.emoji}  ${routine.name}",
                            color = TextMain,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (finished) "All steps done" else "Step ${index + 1} of ${steps.size}",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SurfaceCard)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PrimaryGradient)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AnimatedContent(
                        targetState = index,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) togetherWith
                                    (slideOutHorizontally(tween(260)) { -it / 3 } + fadeOut(tween(200)))
                            } else {
                                (slideInHorizontally(tween(320)) { -it / 3 } + fadeIn(tween(320))) togetherWith
                                    (slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(200)))
                            }
                        },
                        label = "runnerStep"
                    ) { i ->
                        if (i >= steps.size) {
                            FinishedCard(routine)
                        } else {
                            StepCard(i, steps)
                        }
                    }
                }

                if (finished) {
                    BigButton(text = "Close", gradient = true, onClick = onClose)
                    Spacer(Modifier.height(10.dp))
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        SmallTextButton("Do it again") { index = 0 }
                    }
                    Spacer(Modifier.height(20.dp))
                } else {
                    val last = index == steps.lastIndex
                    BigButton(text = if (last) "Finish  ✓" else "Done  ✓", gradient = true) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        index++
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (index > 0) SmallTextButton("←  Back") { index-- } else Spacer(Modifier.width(1.dp))
                        SmallTextButton("Skip  →") { index++ }
                    }
                }
            }

            ConfettiBurst(trigger = if (finished) 1 else 0)
        }
    }
}

@Composable
private fun StepCard(index: Int, steps: List<String>) {
    val pulse by rememberInfiniteTransition(label = "stepRing").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "stepRingValue"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // A ring that keeps rippling outwards, a gentle "this is the one" cue.
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        val scale = 1f + 0.7f * pulse
                        scaleX = scale
                        scaleY = scale
                        alpha = 0.55f * (1f - pulse)
                    }
                    .border(2.dp, PrimaryLight, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(PrimaryGradient),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            text = steps[index],
            color = TextMain,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))
        val next = steps.getOrNull(index + 1)
        Text(
            text = if (next != null) "Next: $next" else "Last step",
            color = TextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FinishedCard(routine: Routine) {
    val pop = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🎉",
            fontSize = 72.sp,
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
                rotationZ = (1f - pop.value) * -30f
            }
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "You did it!",
            color = TextMain,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = "\"${routine.name}\" is done. That was ${routine.steps.size} small wins.",
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SmallTextButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = PrimaryLight, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}
