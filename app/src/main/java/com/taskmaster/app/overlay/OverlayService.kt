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
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import android.view.Gravity
import android.view.HapticFeedbackConstants
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
import com.taskmaster.app.data.NoteStore
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
    private lateinit var noteStore: NoteStore
    private lateinit var owner: ServiceOwner

    private var bubbleView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var popupView: View? = null
    private var snapAnimator: ValueAnimator? = null

    // After a few idle seconds the bubble tucks into the screen edge, mostly hidden and faded,
    // leaving just enough showing to tap. Touching it brings it back at full colour.
    private val handler = Handler(Looper.getMainLooper())
    private val dockRunnable = Runnable { dock() }
    private var isDocked = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        store = TaskStore.get(this)
        noteStore = NoteStore.get(this)
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
        // "Brain dump" button in the notification: open the popup straight on the dump tab.
        if (intent?.action == ACTION_DUMP) showPopup(PopupTab.DUMP)
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Keep the bubble on the screen (and on its edge) after rotation.
        val params = bubbleParams ?: return
        params.x = edgeX(OverlaySettings.isOnRightSide(this))
        params.y = params.y.coerceIn(0, maxBubbleY())
        updateBubbleLayout()
        bubbleView?.alpha = 1f
        isDocked = false
        scheduleDock()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
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
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                // Lets the window slide partly off the screen when it tucks into the edge.
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = edgeX(OverlaySettings.isOnRightSide(this@OverlayService))
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
        scheduleDock()
    }

    /** Receives all touches itself, so dragging and tapping never reach the Compose child. */
    private inner class DragFrame(context: Context) : FrameLayout(context) {
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private var downRawX = 0f
        private var downRawY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false
        private var longPressed = false

        // Holding the bubble opens the popup straight on Brain dump, keyboard up.
        private val longPressRunnable = Runnable {
            if (!dragging) {
                longPressed = true
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                showPopup(PopupTab.DUMP)
            }
        }

        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val params = bubbleParams ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    handler.removeCallbacks(dockRunnable)
                    snapAnimator?.cancel()
                    if (isDocked) {
                        // Touching a tucked-away bubble brings it fully back, in full colour.
                        undock()
                        startX = edgeX(params.x + dp(BUBBLE_WINDOW_DP) / 2 > screenWidth() / 2)
                    } else {
                        startX = params.x
                    }
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startY = params.y
                    dragging = false
                    longPressed = false
                    handler.postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout().toLong())
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (!dragging && hypot(dx, dy) > touchSlop) {
                        dragging = true
                        handler.removeCallbacks(longPressRunnable)
                        snapAnimator?.cancel()
                    }
                    if (dragging) {
                        params.x = (startX + dx).toInt().coerceIn(0, maxBubbleX())
                        params.y = (startY + dy).toInt().coerceIn(0, maxBubbleY())
                        updateBubbleLayout()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)
                    if (longPressed) {
                        // The long press already opened the popup.
                    } else if (dragging) {
                        snapToEdge()
                    } else {
                        performClick()
                    }
                    scheduleDock()
                }

                MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(longPressRunnable)
                    scheduleDock()
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

    /** Window x that puts the bubble flush against the left or right edge. */
    private fun edgeX(right: Boolean): Int = if (right) maxBubbleX() else 0

    /** Window x that leaves only a small slice of the bubble showing at the edge. */
    private fun peekX(right: Boolean): Int {
        val inset = dp((BUBBLE_WINDOW_DP - BUBBLE_DP) / 2)
        return if (right) {
            screenWidth() - inset - dp(PEEK_VISIBLE_DP)
        } else {
            -(dp(BUBBLE_WINDOW_DP) - inset - dp(PEEK_VISIBLE_DP))
        }
    }

    private fun animateBubble(toX: Int, toAlpha: Float) {
        val params = bubbleParams ?: return
        val view = bubbleView ?: return
        snapAnimator?.cancel()
        view.animate().alpha(toAlpha).setDuration(220).start()
        snapAnimator = ValueAnimator.ofInt(params.x, toX).apply {
            duration = 220
            addUpdateListener {
                params.x = it.animatedValue as Int
                updateBubbleLayout()
            }
            start()
        }
    }

    private fun onRightHalf(): Boolean {
        val params = bubbleParams ?: return true
        return params.x + dp(BUBBLE_WINDOW_DP) / 2 > screenWidth() / 2
    }

    private fun dock() {
        if (popupView != null || bubbleParams == null) return
        isDocked = true
        animateBubble(peekX(onRightHalf()), DOCKED_ALPHA)
    }

    private fun undock() {
        isDocked = false
        animateBubble(edgeX(onRightHalf()), 1f)
    }

    private fun scheduleDock() {
        handler.removeCallbacks(dockRunnable)
        handler.postDelayed(dockRunnable, DOCK_DELAY_MS)
    }

    private fun updateBubbleLayout() {
        val view = bubbleView ?: return
        val params = bubbleParams ?: return
        if (view.isAttachedToWindow) windowManager.updateViewLayout(view, params)
    }

    /* -------------------------------------------------------------- */
    /* Popup                                                           */
    /* -------------------------------------------------------------- */

    private fun showPopup(tab: PopupTab? = null) {
        if (popupView != null) return
        val startTab = tab ?: OverlaySettings.lastTab(this)
        if (tab != null) OverlaySettings.saveTab(this, tab)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Focusable (no FLAG_NOT_FOCUSABLE) so the on-screen keyboard can type into it.
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                if (startTab == PopupTab.DUMP) {
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                } else {
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED
                }
        }

        val root = PopupFrame(this).apply {
            attachOwner(this)
            addView(ComposeView(context).apply {
                setContent {
                    TaskMasterTheme {
                        QuickPopup(
                            tasks = store,
                            notes = noteStore,
                            startTab = startTab,
                            onTabChange = { OverlaySettings.saveTab(this@OverlayService, it) },
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
        handler.removeCallbacks(dockRunnable)
        bubbleView?.visibility = View.GONE
    }

    private fun hidePopup() {
        val view = popupView ?: return
        popupView = null
        removeView(view)
        bubbleView?.visibility = View.VISIBLE
        scheduleDock()
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
        val dump = PendingIntent.getService(
            this,
            2,
            Intent(this, OverlayService::class.java).setAction(ACTION_DUMP),
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
            .addAction(0, getString(R.string.bubble_notification_dump), dump)
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
        private const val ACTION_DUMP = "com.taskmaster.app.overlay.DUMP"

        private const val DOCK_DELAY_MS = 2500L
        private const val DOCKED_ALPHA = 0.5f
        private const val PEEK_VISIBLE_DP = 22

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
