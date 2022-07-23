package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.prefs.AppPrefs
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView.OnGestureListener
import org.fcitx.fcitx5.android.utils.hapticIfEnabled

abstract class CustomGestureView(ctx: Context) : FrameLayout(ctx) {

    enum class SwipeAxis { X, Y }

    enum class GestureType { Down, Move, Up }

    data class Event(
        val type: GestureType,
        val x: Float,
        val y: Float,
        val countX: Int,
        val countY: Int,
        val totalX: Int,
        val totalY: Int
    ) {
        constructor(type: GestureType, x: Float, y: Float) : this(type, x, y, 0, 0, 0, 0)
    }

    fun interface OnGestureListener {
        fun onGesture(view: View, event: Event): Boolean

        companion object {
            val Empty = OnGestureListener { _, _ -> false }
        }
    }

    private val lifecycleScope by lazy {
        findViewTreeLifecycleOwner()?.lifecycleScope!!
    }

    @Volatile
    private var longPressTriggered = false
    var longPressEnabled = false
    private var longPressJob: Job? = null

    @Volatile
    private var repeatStarted = false
    var repeatEnabled = false
    private var repeatJob: Job? = null

    var swipeEnabled = false
    var swipeRepeatEnabled = false
    var swipeThresholdX = 24f
    var swipeThresholdY = 24f

    private var swipeRepeatTriggered = false
    private var swipeLastX = -1f
    private var swipeLastY = -1f
    private var swipeXUnconsumed = 0f
    private var swipeYUnconsumed = 0f
    private var swipeTotalX = 0
    private var swipeTotalY = 0
    private var gestureConsumed = false

    var doubleTapEnabled = false
    private var lastClickTime = 0L
    private var maybeDoubleTap = false

    var onDoubleTapListener: ((View) -> Unit)? = null
    var onRepeatListener: ((View) -> Unit)? = null
    var onGestureListener: OnGestureListener? = null

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        if (!enabled) {
            isPressed = false
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!isEnabled) return false
                val x = event.x
                val y = event.y
                drawableHotspotChanged(x, y)
                isPressed = true
                hapticIfEnabled()
                dispatchGestureEvent(GestureType.Down, x, y)
                if (longPressEnabled) {
                    longPressJob?.cancel()
                    longPressJob = lifecycleScope.launch {
                        delay(longPressDelay.toLong())
                        longPressTriggered = performLongClick()
                    }
                }
                if (repeatEnabled) {
                    repeatJob?.cancel()
                    repeatJob = lifecycleScope.launch {
                        delay(longPressDelay.toLong())
                        repeatStarted = true
                        var lastTriggerTime: Long
                        while (isActive && isEnabled) {
                            lastTriggerTime = SystemClock.uptimeMillis()
                            onRepeatListener?.invoke(this@CustomGestureView)
                            val t = lastTriggerTime + RepeatInterval - SystemClock.uptimeMillis()
                            if (t > 0) delay(t)
                        }
                    }
                }
                if (swipeEnabled) {
                    swipeLastX = x
                    swipeLastY = y
                }
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false

                dispatchGestureEvent(GestureType.Up, event.x, event.y)
                val shouldPerformClick =
                    !(longPressTriggered || repeatStarted || swipeRepeatTriggered || gestureConsumed)

                if (longPressEnabled) {
                    longPressTriggered = false
                    longPressJob?.cancel()
                    longPressJob = null
                }
                if (repeatEnabled) {
                    repeatStarted = false
                    repeatJob?.cancel()
                    repeatJob = null
                }
                if (swipeEnabled) {
                    if (swipeRepeatEnabled) {
                        swipeRepeatTriggered = false
                    }
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
                    swipeTotalX = 0
                    swipeTotalY = 0
                    gestureConsumed = false
                }

                if (shouldPerformClick) {
                    if (doubleTapEnabled) {
                        val now = System.currentTimeMillis()
                        if (maybeDoubleTap && now - lastClickTime <= longPressDelay) {
                            maybeDoubleTap = false
                            onDoubleTapListener?.invoke(this)
                        } else {
                            maybeDoubleTap = true
                            performClick()
                        }
                        lastClickTime = now
                    } else {
                        performClick()
                    }
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!isEnabled) return false
                val x = event.x
                val y = event.y
                drawableHotspotChanged(x, y)
                if (!swipeEnabled || longPressTriggered || repeatStarted) return true
                val countX = consumeSwipe(x - swipeLastX + swipeXUnconsumed, SwipeAxis.X)
                val countY = consumeSwipe(y - swipeLastY + swipeYUnconsumed, SwipeAxis.Y)
                if (countX != 0 || countY != 0) {
                    swipeTotalX += countX
                    swipeTotalY += countY
                    dispatchGestureEvent(GestureType.Move, x, y, countX, countY)
                }
                swipeLastX = x
                swipeLastY = y
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                if (longPressEnabled) {
                    longPressTriggered = false
                    longPressJob?.cancel()
                    repeatJob = null
                }
                if (repeatEnabled) {
                    repeatStarted = false
                    repeatJob?.cancel()
                    repeatJob = null
                }
                if (doubleTapEnabled) {
                    maybeDoubleTap = false
                    lastClickTime = 0
                }
                if (swipeEnabled) {
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
                    if (swipeRepeatEnabled) {
                        swipeRepeatTriggered = false
                    }
                    swipeTotalX = 0
                    swipeTotalY = 0
                    gestureConsumed = false
                }
                dispatchGestureEvent(GestureType.Up, event.x, event.y)
                gestureConsumed = false
                isPressed = false
                return true
            }
        }
        return true
    }

    private fun dispatchGestureEvent(
        type: GestureType,
        x: Float,
        y: Float,
        countX: Int = 0,
        countY: Int = 0
    ) {
        onGestureListener
            ?.onGesture(this, Event(type, x, y, countX, countY, swipeTotalX, swipeTotalY))
            ?.also { gestureConsumed = it }
    }

    private fun consumeSwipe(unconsumed: Float, axis: SwipeAxis): Int {
        val threshold = when (axis) {
            SwipeAxis.X -> swipeThresholdX
            SwipeAxis.Y -> swipeThresholdY
        }
        val remains: Float = unconsumed % threshold
        val count: Int = (unconsumed / threshold).toInt()
        if (count != 0) {
            if (swipeRepeatEnabled && !swipeRepeatTriggered) {
                swipeRepeatTriggered = true
            }
            if (longPressEnabled && !longPressTriggered) {
                longPressJob?.cancel()
                longPressJob = null
            }
            if (repeatEnabled && !repeatStarted) {
                repeatJob?.cancel()
                repeatJob = null
            }
        }
        when (axis) {
            SwipeAxis.X -> swipeXUnconsumed = remains
            SwipeAxis.Y -> swipeYUnconsumed = remains
        }
        return count
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        longPressEnabled = l != null
        super.setOnLongClickListener(l)
    }

    companion object {
        val longPressDelay by AppPrefs.getInstance().keyboard.longPressDelay

        const val RepeatInterval = 50L
    }
}
