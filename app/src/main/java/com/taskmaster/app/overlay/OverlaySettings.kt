package com.taskmaster.app.overlay

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

/** Stored on/off state and last position of the floating bubble. */
object OverlaySettings {

    private const val PREFS_NAME = "taskmaster_prefs"
    const val KEY_ENABLED = "bubble_enabled"
    private const val KEY_RIGHT = "bubble_right"
    private const val KEY_Y = "bubble_y"

    fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** True when the user has granted "Display over other apps". */
    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun isOnRightSide(context: Context): Boolean = prefs(context).getBoolean(KEY_RIGHT, true)

    /** Saved vertical position in pixels, or -1 when the bubble has never been moved. */
    fun savedY(context: Context): Int = prefs(context).getInt(KEY_Y, -1)

    fun savePosition(context: Context, rightSide: Boolean, y: Int) {
        prefs(context).edit().putBoolean(KEY_RIGHT, rightSide).putInt(KEY_Y, y).apply()
    }
}
