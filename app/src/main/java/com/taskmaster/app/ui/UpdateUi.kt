package com.taskmaster.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.HighPri
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.PrimaryLight
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.SurfaceModal
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted
import com.taskmaster.app.update.UpdateChecker
import com.taskmaster.app.update.UpdateInfo
import com.taskmaster.app.update.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Everything about updating, shared by the "Check for updates" row and the update dialog. */
@Stable
class UpdateState(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var available by mutableStateOf<UpdateInfo?>(null)
        private set
    var dialogOpen by mutableStateOf(false)
        private set
    var checking by mutableStateOf(false)
        private set
    var progress by mutableStateOf<Float?>(null) // null = not downloading
        private set
    var failed by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var messageIsError by mutableStateOf(false)
        private set

    /** Asks once per app launch, quietly: nothing is shown unless a newer version exists. */
    fun checkOnLaunch() {
        if (autoChecked) return
        autoChecked = true
        check(manual = false)
    }

    /** [manual] checks also report "up to date" or an error below the button. */
    fun check(manual: Boolean) {
        if (checking || progress != null) return
        scope.launch {
            checking = true
            message = null
            messageIsError = false
            when (val result = UpdateChecker.check(context)) {
                is UpdateResult.Available -> {
                    available = result.info
                    dialogOpen = true
                    if (manual) message = "Version ${result.info.version} is available"
                }

                is UpdateResult.UpToDate -> {
                    if (manual) message = "You're on the latest version"
                }

                is UpdateResult.Failed -> {
                    if (manual) {
                        message = "Couldn't check. Is your internet on?"
                        messageIsError = true
                    }
                }
            }
            checking = false
        }
    }

    fun later() {
        dialogOpen = false
    }

    fun startUpdate() {
        val info = available ?: return
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

    private companion object {
        var autoChecked = false
    }
}

@Composable
fun rememberUpdateState(): UpdateState {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    return remember { UpdateState(context, scope) }
}

/** Card showing the installed version with a "Check" button. */
@Composable
fun UpdateRow(state: UpdateState, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val version = remember { UpdateChecker.currentVersion(context) }
    val shape = RoundedCornerShape(20.dp)
    val busy = state.checking || state.progress != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 25.dp, end = 25.dp, bottom = 6.dp)
            .clip(shape)
            .background(SurfaceCard)
            .border(1.dp, BorderColor, shape)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "TaskMaster $version",
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = state.message ?: "See if a newer version is out",
                color = if (state.messageIsError) HighPri else TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryGradient)
                .clickable(enabled = !busy) { state.check(manual = true) }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.checking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = TextMain,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Check",
                    color = TextMain,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** "Update available" popup with Update now / Later. */
@Composable
fun UpdateDialog(state: UpdateState) {
    val info = state.available
    if (info == null || !state.dialogOpen) return

    val context = LocalContext.current
    val downloading = state.progress != null

    AlertDialog(
        onDismissRequest = { if (!downloading) state.later() },
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
                val p = state.progress
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
                if (state.failed) {
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
            TextButton(enabled = !downloading, onClick = { state.startUpdate() }) {
                Text("Update now", color = PrimaryLight, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(enabled = !downloading, onClick = { state.later() }) {
                Text("Later", color = TextMuted)
            }
        }
    )
}
