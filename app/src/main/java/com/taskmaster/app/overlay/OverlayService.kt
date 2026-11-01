package com.taskmaster.app.overlay

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.taskmaster.app.MainActivity
import com.taskmaster.app.R
import com.taskmaster.app.data.TaskStore
import com.taskmaster.app.ui.theme.TaskMasterTheme
import kotlin.math.hypot

/**
 * Keeps the floating bubble on screen. Tapping it opens a popup where tasks can be added and
 * ticked; dragging moves it and it snaps to the nearest screen edge.
 *
 * Runs as a foreground service so Android does not kill it while other apps are in front.
 */
class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var store: TaskStore
    private lateinit var owner: ServiceOwner

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var popupView: View? = null
    private var snapAnimator: ValueAnimator? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        store = TaskStore.get(this)
        owner = ServiceOwner().also { it.start() }
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A service started with startForegroundService() must call startForeground() quickly,
        // even if it is about to stop again.
        startInForeground()

        if (intent?.action == ACTION_STOP) {
            OverlaySettings.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!OverlaySettings.canDraw(this)) {
            // Permission was taken away in Settings: switch the bubble off cleanly.
            OverlaySettings.setEnabled(this, false)
            stopSelf()
            return START_NOT_STICKY
        }

        showBubble()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Keep the bubble on the screen (and on its edge) after rotation.
        val params = bubbleParams ?: return
        params.x = if (OverlaySettings.isOnRightSide(this)) maxBubbleX() else 0
        params.y = params.y.coerceIn(0, maxBubbleY())
        updateBubbleLayout()
    }

    override fun onDestroy() {
        snapAnimator?.cancel()
        removeView(popupView)
        popupView = null
        removeView(bubbleView)
        bubbleView = null
        bubbleParams = null
        owner.stop()
        super.onDestroy()
    }

    /* -------------------------------------------------------------- */
    /* Bubble                                                          */
    /* -------------------------------------------------------------- */

    private fun showBubble() {
        if (bubbleView != null) return

        val size = dp(BUBBLE_WINDOW_DP)
        val params = WindowManager.LayoutParams(
            size,
            size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = if (OverlaySettings.isOnRightSide(this@OverlayService)) maxBubbleX() else 0
            val savedY = OverlaySettings.savedY(this@OverlayService)
            y = (if (savedY >= 0) savedY else screenHeight() / 3).coerceIn(0, maxBubbleY())
        }

        val root = DragFrame(this).apply {
            attachOwner(this)
            setOnClickListener { showPopup() }
            addView(ComposeView(context).apply {
                setContent { TaskMasterTheme { BubbleContent(store) } }
            })
        }

        bubbleParams = params
        bubbleView = root
        windowManager.addView(root, params)
    }

    /** Receives all touches itself, so dragging and tapping never reach the Compose child. */
    private inner class DragFrame(context: Context) : FrameLayout(context) {
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val params = bubbleParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    snapAnimator?.cancel()
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragging = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && hypot(dx, dy) > touchSlop) dragging = true
                    if (dragging) {
                        params.x = (startX + dx).toInt().coerceIn(0, maxBubbleX())
                        params.y = (startY + dy).toInt().coerceIn(0, maxBubbleY())
                        updateBubbleLayout()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (dragging) snapToEdge() else performClick()
                }
            }
            return true
        }

        override fun performClick(): Boolean = super.performClick()
    }

    private fun snapToEdge() {
        val params = bubbleParams ?: return
        val toRight = params.x + dp(BUBBLE_WINDOW_DP) / 2 > screenWidth() / 2
        val targetX = if (toRight) maxBubbleX() else 0
        OverlaySettings.savePosition(this, toRight, params.y)

        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofInt(params.x, targetX).apply {
            duration = 200
            addUpdateListener {
                params.x = it.animatedValue as Int
                updateBubbleLayout()
            }
            start()
        }
    }

    private fun updateBubbleLayout() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        if (view.isAttachedToWindow) windowManager.updateViewLayout(view, params)
    }

    /* -------------------------------------------------------------- */
    /* Popup                                                           */
    /* -------------------------------------------------------------- */

    private fun showPopup() {
        if (popupView != null) return

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable (no FLAG_NOT_FOCUSABLE) so the on-screen keyboard can type into it.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }

        val root = PopupFrame(this).apply {
            attachOwner(this)
            addView(ComposeView(context).apply {
                setContent {
                    TaskMasterTheme {
                        QuickTasksPopup(
                            store = store,
                            onClose = { hidePopup() },
                            onOpenApp = { openApp() }
                        )
                    }
                }
            })
        }

        popupView = root
        windowManager.addView(root, params)
        // Only one of the two is visible at a time.
        bubbleView?.visibility = View.GONE
    }

    private fun hidePopup() {
        val view = popupView ?: return
        popupView = null
        removeView(view)
        bubbleView?.visibility = View.VISIBLE
    }

    private fun openApp() {
        // Start the activity while a window is still visible, then close the popup.
        startActivity(
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        hidePopup()
    }

    /** Closes the popup when Back is pressed (the keyboard, if open, handles Back first). */
    private inner class PopupFrame(context: Context) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) hidePopup()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }

    /* -------------------------------------------------------------- */
    /* Helpers                                                         */
    /* -------------------------------------------------------------- */

    /** Compose needs lifecycle, saved-state and ViewModel owners; a Service has none of its own. */
    private fun attachOwner(view: View) {
        view.setViewTreeLifecycleOwner(owner)
        view.setViewTreeViewModelStoreOwner(owner)
        view.setViewTreeSavedStateRegistryOwner(owner)
    }

    private fun removeView(view: View?) {
        if (view != null && view.isAttachedToWindow) {
            try {
                windowManager.removeView(view)
            } catch (e: IllegalArgumentException) {
                // Already removed.
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun screenWidth(): Int = resources.displayMetrics.widthPixels
    private fun screenHeight(): Int = resources.displayMetrics.heightPixels
    private fun maxBubbleX(): Int = (screenWidth() - dp(BUBBLE_WINDOW_DP)).coerceAtLeast(0)
    private fun maxBubbleY(): Int = (screenHeight() - dp(BUBBLE_WINDOW_DP)).coerceAtLeast(0)

    private class ServiceOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val store = ViewModelStore()
        private val savedStateController = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun start() {
            savedStateController.performAttach()
            savedStateController.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }

    /* -------------------------------------------------------------- */
    /* Foreground notification                                         */
    /* -------------------------------------------------------------- */

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.bubble_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply { setShowBadge(false) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val turnOff = PendingIntent.getService(
            this,
            1,
            Intent(this, OverlayService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_bubble)
            .setContentTitle(getString(R.string.bubble_notification_title))
            .setContentText(getString(R.string.bubble_notification_text))
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.bubble_notification_turn_off), turnOff)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "taskmaster_bubble"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.taskmaster.app.overlay.STOP"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, OverlayService::class.java)
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
