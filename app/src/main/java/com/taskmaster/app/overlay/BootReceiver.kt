package com.taskmaster.app.overlay

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Brings the bubble back after a reboot or an app update, if the user left it switched on. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) {
            return
        }
        if (OverlaySettings.isEnabled(context) && OverlaySettings.canDraw(context)) {
            try {
                OverlayService.start(context)
            } catch (e: Exception) {
                // The system may refuse to start a foreground service from the background;
                // the bubble then simply comes back the next time the app is opened.
            }
        }
    }
}
