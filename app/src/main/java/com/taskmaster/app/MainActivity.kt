package com.taskmaster.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.taskmaster.app.ui.TaskApp
import com.taskmaster.app.ui.theme.TaskMasterTheme

class MainActivity : ComponentActivity() {

    // Bumped each time the "Brain dump" shortcut launches the app, even if it is already open.
    private var dumpRequest by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            TaskMasterTheme {
                TaskApp(dumpRequest = dumpRequest)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_BRAIN_DUMP) dumpRequest++
    }

    companion object {
        const val ACTION_BRAIN_DUMP = "com.taskmaster.app.action.BRAIN_DUMP"
    }
}
