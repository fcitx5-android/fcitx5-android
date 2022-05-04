package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
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
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign

abstract class CustomGestureView(ctx: Context) : FrameLayout(ctx) {

    enum class SwipeAxis { X, Y }

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

    private var maybeSwipeOnKeyUp = false
    private var swipeRepeatTriggered = false
    private var swipeLastX = -1f
    private var swipeLastY = -1f
    private var swipeXUnconsumed = 0f
    private var swipeYUnconsumed = 0f

    var doubleTapEnabled = false
    private var lastClickTime = 0L
    private var maybeDoubleTap = false

    var onSwipeUpListener: ((View, Int) -> Unit)? = null
    var onSwipeRightListener: ((View, Int) -> Unit)? = null
    var onSwipeDownListener: ((View, Int) -> Unit)? = null
    var onSwipeLeftListener: ((View, Int) -> Unit)? = null
    var onDoubleTapListener: ((View) -> Unit)? = null
    var onRepeatListener: ((View) -> Unit)? = null
    var onTouchDownListener: ((View) -> Unit)? = null
    var onTouchLeaveListener: ((View) -> Unit)? = null

    private fun calculateInterval(t: Long) =
        if (t > accelerateTime) endInterval
        else
            (initialInterval * exp(-ln(initialInterval.toDouble() / endInterval) / accelerateTime * t)).toLong()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                drawableHotspotChanged(x, y)
                isPressed = true
                hapticIfEnabled()
                onTouchDownListener?.invoke(this)
                if (longPressEnabled) {
                    longPressJob?.cancel()
                    longPressJob = lifecycleScope.launch {
                        delay(longPressDelay.toLong())
                        if (!(maybeSwipeOnKeyUp || swipeRepeatTriggered)) {
                            longPressTriggered = true
                            performLongClick()
                        }
                    }
                }
                if (repeatEnabled) {
                    repeatJob?.cancel()
                    repeatJob = lifecycleScope.launch {
                        delay(longPressDelay.toLong())
                        repeatStarted = true
                        val t0 = System.currentTimeMillis()
                        while (isActive && isEnabled) {
                            onRepeatListener?.invoke(this@CustomGestureView)
                            val t = System.currentTimeMillis() - t0
                            delay(calculateInterval(t))
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

                val shouldPerformClick =
                    !(longPressTriggered || repeatStarted || swipeRepeatTriggered || maybeSwipeOnKeyUp)

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
                    if (maybeSwipeOnKeyUp) {
                        maybeSwipeOnKeyUp = false
                        if (swipeXUnconsumed.absoluteValue > swipeThresholdX) {
                            swipeX(swipeXUnconsumed.sign)
                        }
                        if (swipeYUnconsumed.absoluteValue > swipeThresholdY) {
                            swipeY(swipeYUnconsumed.sign)
                        }
                    }
                    if (swipeRepeatEnabled) {
                        swipeRepeatTriggered = false
                    }
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
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
                } else {
                    // TODO refactor select-to-delete and cursor move KeyAction
                    // invoking this listener in else branch kinda defeats the purpose of 'onTouchLeave'
                    // since only backspace key is using it, this could be a workaround for double-delete issue
                    // onTouchLeaveListener?.invoke(this)
                }
                // FIXME this causes double-delete when swipe backspace key
                onTouchLeaveListener?.invoke(this)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val x = event.x
                val y = event.y
                drawableHotspotChanged(x, y)
                if (swipeEnabled) {
                    if ((!swipeRepeatEnabled && maybeSwipeOnKeyUp)
                        || longPressTriggered
                        || repeatStarted
                    ) return true
                    swipeXUnconsumed += x - swipeLastX
                    swipeYUnconsumed += y - swipeLastY
                    swipeLastX = x
                    swipeLastY = y
                    swipeXUnconsumed = consumeSwipe(swipeXUnconsumed, swipeThresholdX, SwipeAxis.X)
                    swipeYUnconsumed = consumeSwipe(swipeYUnconsumed, swipeThresholdY, SwipeAxis.Y)
                    return true
                }
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
                    maybeSwipeOnKeyUp = false
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
                    if (swipeRepeatEnabled) {
                        swipeRepeatTriggered = false
                    }
                }
                onTouchLeaveListener?.invoke(this)
                isPressed = false
                return true
            }
        }
        return true
    }

    private fun swipeX(direction: Float, count: Int = 1) {
        if (direction > 0) {
            onSwipeRightListener?.invoke(this, count)
        } else {
            onSwipeLeftListener?.invoke(this, count)
        }
    }

    private fun swipeY(direction: Float, count: Int = 1) {
        if (direction > 0) {
            onSwipeDownListener?.invoke(this, count)
        } else {
            onSwipeUpListener?.invoke(this, count)
        }
    }

    private fun consumeSwipe(unconsumed: Float, threshold: Float, axis: SwipeAxis): Float {
        val direction = unconsumed.sign
        var remaining = unconsumed.absoluteValue
        var count = 0
        while (remaining >= threshold) {
            if (repeatEnabled && !repeatStarted) {
                repeatJob?.cancel()
                repeatJob = null
            }
            remaining -= threshold
            count += 1
            if (swipeRepeatEnabled) {
                swipeRepeatTriggered = true
            } else {
                maybeSwipeOnKeyUp = true
                return unconsumed
            }
        }
        if (count > 0) {
            when (axis) {
                SwipeAxis.X -> swipeX(direction, count)
                SwipeAxis.Y -> swipeY(direction, count)
            }
        }
        return direction * remaining
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        longPressEnabled = l != null
        super.setOnLongClickListener(l)
    }

    companion object {
        val longPressDelay by AppPrefs.getInstance().keyboard.longPressDelay

        const val initialInterval: Long = 200L
        const val endInterval: Long = 30L
        const val accelerateTime: Long = 1000L
    }
}
