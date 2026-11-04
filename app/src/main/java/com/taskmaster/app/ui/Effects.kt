package com.taskmaster.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.Purple

/** Border for cards: a soft highlight along the top edge that fades out, like light on glass. */
internal val CardBorderBrush: Brush =
    Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x0AFFFFFF)))

/** Enter / exit used by every full-screen page, so they all feel like one family. */
internal val PageEnter: EnterTransition =
    slideInHorizontally(tween(380, easing = FastOutSlowInEasing)) { it / 3 } +
        fadeIn(tween(300)) +
        scaleIn(tween(380, easing = FastOutSlowInEasing), initialScale = 0.95f)

internal val PageExit: ExitTransition =
    slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 3 } +
        fadeOut(tween(240)) +
        scaleOut(tween(300, easing = FastOutSlowInEasing), targetScale = 0.95f)

/**
 * Click with a springy squeeze while the finger is down. Put it FIRST in a modifier chain, so
 * the background, border and content squeeze together.
 */
internal fun Modifier.bounceClick(
    pressedScale: Float = 0.94f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press"
    )
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/**
 * Fades, grows and rises into place when first shown, a little later for each [index].
 * Only changes how the item is drawn, never where it sits, so it cannot get stuck.
 */
internal fun Modifier.popIn(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            1f,
            tween(
                durationMillis = 420,
                delayMillis = 50 * index.coerceIn(0, 8),
                easing = FastOutSlowInEasing
            )
        )
    }
    this.graphicsLayer {
        val p = progress.value
        alpha = p
        scaleX = 0.95f + 0.05f * p
        scaleY = 0.95f + 0.05f * p
        translationY = (1f - p) * 26.dp.toPx()
    }
}

/**
 * Two big, soft colour glows that drift slowly behind the content. Drawn in the draw phase
 * only, so it never causes recomposition.
 */
@Composable
internal fun AmbientBackground(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ambientA"
    )
    val b by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(19000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ambientB"
    )
    Box(
        modifier = modifier.drawBehind {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Primary.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(w * (0.10f + 0.55f * a), h * (0.04f + 0.12f * b)),
                    radius = w * 0.85f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Purple.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(w * (0.95f - 0.5f * b), h * (0.55f + 0.2f * a)),
                    radius = w * 0.9f
                )
            )
        }
    )
}
