package com.taskmaster.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import androidx.compose.foundation.layout.RowScope

/** Two side-by-side cards under the progress card: Brain dump and Routines. */
@Composable
internal fun HomeShortcuts(
    noteCount: Int,
    routineCount: Int,
    onDumpClick: () -> Unit,
    onRoutinesClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp, top = 6.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ShortcutCard(
            emoji = "🧠",
            title = "Brain dump",
            subtitle = if (noteCount == 0) "Capture any idea" else "$noteCount ${if (noteCount == 1) "note" else "notes"}",
            onClick = onDumpClick
        )
        ShortcutCard(
            emoji = "🔁",
            title = "Routines",
            subtitle = if (routineCount == 0) "Big task, small steps" else "$routineCount ${if (routineCount == 1) "template" else "templates"}",
            onClick = onRoutinesClick
        )
    }
}

@Composable
private fun RowScope.ShortcutCard(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 17.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextMain,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
