package com.taskmaster.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Size
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.LowPri
import com.taskmaster.app.ui.theme.MedPri
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.Purple
import kotlin.random.Random

private class Piece(
    val startX: Float,
    val drift: Float,
    val speed: Float,
    val size: Float,
    val spin: Float,
    val color: Color
)

/** A short confetti shower. Pass a new [trigger] number each time it should play. */
@Composable
fun ConfettiBurst(trigger: Int) {
    if (trigger == 0) return

    val colors = listOf(Primary, PrimaryLight, Purple, MedPri, LowPri, HighPri)
    val pieces = remember(trigger) {
        List(70) {
            Piece(
                startX = Random.nextFloat(),
                drift = Random.nextFloat() * 0.3f - 0.15f,
                speed = 0.7f + Random.nextFloat() * 0.6f,
                size = 8f + Random.nextFloat() * 10f,
                spin = Random.nextFloat() * 720f - 360f,
                color = colors[Random.nextInt(colors.size)]
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.animateTo(1f, tween(2200, easing = LinearEasing))
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        pieces.forEach { p ->
            val x = (p.startX + p.drift * t) * size.width
            val y = (-0.1f + p.speed * t * 1.25f) * size.height
            val alpha = if (t > 0.75f) (1f - t) / 0.25f else 1f
            rotate(degrees = p.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.size / 2f, y - p.size / 2f),
                    size = Size(p.size, p.size * 0.6f)
                )
            }
        }
    }
}
