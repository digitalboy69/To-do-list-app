package com.taskmaster.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val BgOuter = Color(0xFF000000)
val SurfaceMain = Color(0xFF131316)
val SurfaceCard = Color(0xFF1C1C21)
val SurfaceModal = Color(0xFF222228)
val SurfaceInput = Color(0xFF121215)
val SurfaceInputFocused = Color(0xFF18181C)

val TextMain = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF8A8A93)
val Placeholder = Color(0xFF52525B)
val CheckboxBorder = Color(0xFF3F3F46)

val Primary = Color(0xFF6366F1)
val PrimaryLight = Color(0xFF818CF8)
val Purple = Color(0xFFA855F7)

val HighPri = Color(0xFFEF4444)
val MedPri = Color(0xFFF59E0B)
val LowPri = Color(0xFF10B981)

val BorderColor = Color(0x0FFFFFFF) // white @ ~6%

val PrimaryGradient: Brush = Brush.linearGradient(listOf(Primary, Purple))

private val TaskMasterColors = darkColorScheme(
    primary = Primary,
    onPrimary = TextMain,
    background = SurfaceMain,
    onBackground = TextMain,
    surface = SurfaceMain,
    onSurface = TextMain
)

/** The design is dark-only, so the system light/dark setting is intentionally ignored. */
@Composable
fun TaskMasterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = TaskMasterColors, content = content)
}
