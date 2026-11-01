package com.taskmaster.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.CheckboxBorder
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceModal
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted

/**
 * Explains a permission with a looping animation of the switch being turned on, then opens the
 * app's own settings page, where that switch is the only thing to do.
 */
@Composable
fun PermissionGuideDialog(
    title: String,
    switchLabel: String,
    steps: List<String>,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceModal,
        titleContentColor = TextMain,
        textContentColor = TextMuted,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                SwitchDemo(label = switchLabel)
                Spacer(Modifier.height(18.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    steps.forEachIndexed { index, step -> GuideStep(index, step) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text("Open settings", color = PrimaryLight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now", color = TextMuted) }
        }
    )
}

/** A mock settings row whose switch keeps turning on: pulse, slide, hold, repeat. */
@Composable
private fun SwitchDemo(label: String) {
    val transition = rememberInfiniteTransition(label = "guideDemo")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "guideT"
    )

    // 0.00-0.40 pulse around the switch, 0.40-0.55 knob slides on, rest of the loop stays on.
    val on = ((t - 0.4f) / 0.15f).coerceIn(0f, 1f)
    val pulse = if (t < 0.4f) (t / 0.4f * 2f) % 1f else 0f
    val trackColor = lerp(CheckboxBorder, Primary, on)
    val shape = RoundedCornerShape(16.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(SurfaceInput)
            .border(1.dp, BorderColor, shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextMain,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .graphicsLayer {
                        val s = 0.8f + 0.7f * pulse
                        scaleX = s
                        scaleY = s
                        alpha = if (t < 0.4f) 0.7f * (1f - pulse) else 0f
                    }
                    .border(2.dp, PrimaryLight, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 30.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(trackColor)
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset((4.dp + 22.dp * on).roundToPx(), 4.dp.roundToPx()) }
                        .size(22.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

/** One numbered step that fades and slides in a little after the previous one. */
@Composable
private fun GuideStep(index: Int, text: String) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 400, delayMillis = 150 * index),
        label = "guideStep"
    )

    Row(
        modifier = Modifier.graphicsLayer {
            alpha = progress
            translationX = (1f - progress) * 24.dp.toPx()
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(PrimaryGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                color = TextMain,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text = text, color = TextMuted, fontSize = 14.sp)
    }
}
