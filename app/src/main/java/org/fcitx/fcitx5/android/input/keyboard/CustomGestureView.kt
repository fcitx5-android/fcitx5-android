package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.MotionEvent
import android.widget.FrameLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
import timber.log.Timber
import kotlin.math.absoluteValue
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sign

abstract class CustomGestureView(ctx: Context) : FrameLayout(ctx) {

    private val lifecycleScope by lazy {
        findViewTreeLifecycleOwner()?.lifecycleScope!!
    }

    var longPressEnabled = false
    private var longPressJob: Job? = null
    private var longPressTriggered = false

    var repeatEnabled = false
    private var repeatJob: Job? = null
    private var repeatStarted = false

    var swipeEnabled = false
    var swipeRepeatEnabled = false
    var swipeThreshold = 24f
    private var swipeTriggered = false
    private var swipeLastX = -1f
    private var swipeLastY = -1f
    private var swipeXUnconsumed = 0f
    private var swipeYUnconsumed = 0f

    fun interface OnSwipeListener {
        fun onSwipe()
    }

    var onSwipeUpListener : OnSwipeListener? = null
    var onSwipeRightListener : OnSwipeListener? = null
    var onSwipeDownListener : OnSwipeListener? = null
    var onSwipeLeftListener : OnSwipeListener? = null

    private fun calculateInterval(t: Long) =
        if (t > accelerateTime) endInterval
        else
            (initialInterval * exp(-ln(initialInterval.toDouble() / endInterval) / accelerateTime * t)).toLong()

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                isPressed = true
                hapticIfEnabled()
                if (longPressEnabled) {
                    longPressJob?.cancel()
                    longPressJob = lifecycleScope.launch {
                        delay(LONG_PRESS_DELAY)
                        longPressTriggered = true
                        performLongClick()
                    }
                }
                if (repeatEnabled) {
                    repeatJob?.cancel()
                    repeatJob = lifecycleScope.launch {
                        delay(firstClickInterval)
                        repeatStarted = true
                        val t0 = System.currentTimeMillis()
                        while (isActive && isEnabled) {
                            performClick()
                            val t = System.currentTimeMillis() - t0
                            delay(calculateInterval(t))
                        }
                    }
                }
                if (swipeEnabled) {
                    swipeLastX = event.x
                    swipeLastY = event.y
                }
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false
                if (longPressEnabled) {
                    longPressTriggered = false
                    longPressJob?.cancel()
                    longPressJob = null
                    if (event.eventTime - event.downTime < LONG_PRESS_DELAY) {
                        performClick()
                    }
                    return true
                }
                if (repeatEnabled) {
                    repeatJob?.cancel()
                    repeatJob = null
                    if (repeatStarted) {
                        repeatStarted = false
                    } else {
                        performClick()
                    }
                    return true
                }
                if (swipeEnabled) {
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
                    if (!swipeTriggered) {
                        performClick()
                    }
                    return true
                }
                performClick()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeEnabled) {
                    swipeXUnconsumed += event.x - swipeLastX
                    swipeYUnconsumed += event.y - swipeLastY
                    swipeLastX = event.x
                    swipeLastY = event.y
                    while (swipeXUnconsumed.absoluteValue > swipeThreshold) {
                        swipeTriggered = true
                        if (!swipeRepeatEnabled) return true
                        val direction = swipeXUnconsumed.sign
                        swipeXUnconsumed -= direction * swipeThreshold
                        if (direction > 0) {
                            onSwipeRightListener?.onSwipe()
                        } else {
                            onSwipeLeftListener?.onSwipe()
                        }
                    }
                    while (swipeYUnconsumed.absoluteValue > swipeThreshold){
                        swipeTriggered = true
                        if (!swipeRepeatEnabled) return true
                        val direction = swipeYUnconsumed.sign
                        swipeYUnconsumed -= direction * swipeThreshold
                        if (direction > 0) {
                            onSwipeDownListener?.onSwipe()
                        } else {
                            onSwipeUpListener?.onSwipe()
                        }
                    }
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
                    repeatJob?.cancel()
                    repeatJob = null
                }
                if (swipeEnabled) {
                    swipeTriggered = false
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
                }
                isPressed = false
                return true
            }
        }
        return true
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        longPressEnabled = l != null
        super.setOnLongClickListener(l)
    }

    companion object {
        const val LONG_PRESS_DELAY = 300L

        const val firstClickInterval: Long = LONG_PRESS_DELAY
        const val initialInterval: Long = 200L
        const val endInterval: Long = 30L
        const val accelerateTime: Long = 1000L
    }
}
