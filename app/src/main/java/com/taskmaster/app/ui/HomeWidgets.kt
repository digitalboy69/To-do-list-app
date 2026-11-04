package com.taskmaster.app.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "3 of 5 done" card with a bar that glides to its new value. */
@Composable
internal fun ProgressCard(done: Int, total: Int) {
    val target = if (total == 0) 0f else done.toFloat() / total
    val progress by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "progress"
    )
    val shape = RoundedCornerShape(20.dp)
    // A soft highlight keeps sliding across the filled part of the bar.
    val sweep by rememberInfiniteTransition(label = "barSweep").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "barSweepValue"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp, bottom = 6.dp)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, CardBorderBrush, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = when {
                    total == 0 -> "No tasks yet"
                    done == total -> "All done! Great work"
                    else -> "$done of $total done"
                },
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceInput)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryGradient)
                    .drawBehind {
                        val band = size.width * 0.35f
                        val start = (size.width + band) * sweep - band
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                                startX = start,
                                endX = start + band
                            )
                        )
                    }
            )
        }
    }
}

@Composable
internal fun SearchField(query: String, onChange: (String) -> Unit) {
    Box(modifier = Modifier.padding(start = 25.dp, end = 25.dp, bottom = 8.dp)) {
        SheetTextField(
            value = query,
            onValueChange = onChange,
            placeholder = "Search tasks...",
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
    }
}

/** Small pill that is either selected (gradient) or plain. */
@Composable
internal fun SelectChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextMuted,
        animationSpec = tween(250),
        label = "chipText"
    )
    Box(
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(PrimaryGradient)
                } else {
                    Modifier
                        .background(SurfaceInput)
                        .border(1.dp, BorderColor, shape)
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Quick due-date choices for the add / edit sheet. [selected] is a LocalDate epoch day. */
@Composable
internal fun DueChips(selected: Long?, onSelect: (Long?) -> Unit) {
    val today = remember { LocalDate.now().toEpochDay() }
    val options = remember(today) {
        listOf<Pair<String, Long?>>(
            "None" to null,
            "Today" to today,
            "Tomorrow" to today + 1,
            "Next week" to today + 7
        )
    }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        options.forEach { (label, day) ->
            SelectChip(label = label, selected = selected == day, onClick = { onSelect(day) })
        }
        // An existing task may have a date that none of the quick choices match.
        if (selected != null && options.none { it.second == selected }) {
            val text = LocalDate.ofEpochDay(selected)
                .format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
            SelectChip(label = text, selected = true, onClick = {})
        }
    }
}
