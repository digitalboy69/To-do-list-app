package com.taskmaster.app.ui

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.taskmaster.app.overlay.OverlayService
import com.taskmaster.app.overlay.OverlaySettings
import com.taskmaster.app.ui.theme.BorderColor
import com.taskmaster.app.ui.theme.CheckboxBorder
import com.taskmaster.app.ui.theme.Primary
import com.taskmaster.app.ui.theme.PrimaryGradient
import com.taskmaster.app.ui.theme.SurfaceCard
import com.taskmaster.app.ui.theme.SurfaceInput
import com.taskmaster.app.ui.theme.TextMain
import com.taskmaster.app.ui.theme.TextMuted

/**
 * On/off switch for the floating bubble. Turning it on walks the user through the "Display over
 * other apps" permission (and, on Android 13+, the notification permission) first.
 */
@Composable
fun BubbleToggle(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun isOn() = OverlaySettings.isEnabled(context) && OverlaySettings.canDraw(context)

    var enabled by remember { mutableStateOf(isOn()) }

    // Keep the switch honest if the bubble was turned off from its notification, or the
    // permission was revoked while the app was in the background.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { enabled = isOn() }
    DisposableEffect(context) {
        val prefs = OverlaySettings.prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == OverlaySettings.KEY_ENABLED) enabled = isOn()
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun turnOn() {
        OverlaySettings.setEnabled(context, true)
        OverlayService.start(context)
        enabled = true
    }

    fun turnOff() {
        OverlaySettings.setEnabled(context, false)
        OverlayService.stop(context)
        enabled = false
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // The settings screen always reports "cancelled", so check the permission itself.
        if (OverlaySettings.canDraw(context)) {
            turnOn()
        } else {
            Toast.makeText(
                context,
                "Allow \"Display over other apps\" to show the bubble",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun requestOverlayPermissionOrTurnOn() {
        if (OverlaySettings.canDraw(context)) {
            turnOn()
        } else {
            overlayPermissionLauncher.launch(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }
    }

    // The bubble works even if this is denied; the notification is just hidden in that case.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { requestOverlayPermissionOrTurnOn() }

    fun startEnable() {
        val needsNotificationPermission = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsNotificationPermission) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestOverlayPermissionOrTurnOn()
        }
    }

    val shape = RoundedCornerShape(20.dp)
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
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PrimaryGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = TextMain,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Floating bubble",
                color = TextMain,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Add and tick tasks from any app",
                color = TextMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        Switch(
            checked = enabled,
            onCheckedChange = { wantsOn -> if (wantsOn) startEnable() else turnOff() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                checkedBorderColor = Primary,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = SurfaceInput,
                uncheckedBorderColor = CheckboxBorder
            )
        )
    }
}
