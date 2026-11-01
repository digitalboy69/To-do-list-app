package com.taskmaster.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceModal
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import com.taskmaster.app.update.UpdateChecker
import com.taskmaster.app.update.UpdateInfo
import kotlinx.coroutines.launch

/**
 * Checks GitHub for a newer release when the app opens and, if there is one, asks the user.
 * "Later" hides it until the next time the app is opened.
 */
@Composable
fun UpdatePrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var update by remember { mutableStateOf<UpdateInfo?>(null) }
    var later by rememberSaveable { mutableStateOf(false) }
    var progress by remember { mutableStateOf<Float?>(null) } // null = not downloading
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { update = UpdateChecker.check(context) }

    val info = update
    if (info == null || later) return

    fun startUpdate() {
        failed = false
        if (!UpdateChecker.canInstall(context)) {
            Toast.makeText(
                context,
                "Allow TaskMaster to install updates, then come back and tap Update now",
                Toast.LENGTH_LONG
            ).show()
            UpdateChecker.openInstallPermissionSettings(context)
            return
        }
        scope.launch {
            progress = 0f
            try {
                val apk = UpdateChecker.download(context, info) { progress = it }
                progress = null
                UpdateChecker.install(context, apk)
            } catch (e: Exception) {
                progress = null
                failed = true
            }
        }
    }

    val downloading = progress != null

    AlertDialog(
        onDismissRequest = { if (!downloading) later = true },
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceModal,
        titleContentColor = TextMain,
        textContentColor = TextMuted,
        title = { Text("Update available", fontWeight = FontWeight.SemiBold) },
        text = {
            Column {
                Text(
                    text = "Version ${info.version} is ready. You have " +
                        "${UpdateChecker.currentVersion(context)}.",
                    fontSize = 14.sp
                )
                val p = progress
                if (p != null) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier.fillMaxWidth(),
                        color = Primary,
                        trackColor = SurfaceInput
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Downloading... ${(p * 100).toInt()}%", fontSize = 13.sp)
                }
                if (failed) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Download failed. Check your connection and try again.",
                        color = HighPri,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !downloading, onClick = { startUpdate() }) {
                Text("Update now", color = PrimaryLight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !downloading, onClick = { later = true }) {
                Text("Later", color = TextMuted)
            }
        }
    )
}
