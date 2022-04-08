package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.fcitx.fcitx5.android.data.Prefs
import org.fcitx.fcitx5.android.utils.hapticIfEnabled
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

    @Volatile
    private var longPressTriggered = false

    var repeatEnabled = false
    private var repeatJob: Job? = null

    @Volatile
    private var repeatStarted = false

    var swipeEnabled = false
    var swipeRepeatEnabled = false
    var swipeThresholdX = 24f
    var swipeThresholdY = 24f

    @Volatile
    private var swipeTriggered = false
    private var swipeLastX = -1f
    private var swipeLastY = -1f
    private var swipeXUnconsumed = 0f
    private var swipeYUnconsumed = 0f


    var doubleTapEnabled = false
    private var lastClickTime = 0L
    private var firstClick = false

    var onSwipeUpListener: ((View) -> Unit)? = null
    var onSwipeRightListener: ((View) -> Unit)? = null
    var onSwipeDownListener: ((View) -> Unit)? = null
    var onSwipeLeftListener: ((View) -> Unit)? = null
    var onDoubleTapListener: ((View) -> Unit)? = null
    var onRepeatListener: ((View) -> Unit)? = null

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
                        delay(longPressDelay.toLong())
                        if (!swipeTriggered) {
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
                    swipeLastX = event.x
                    swipeLastY = event.y
                }
            }
            MotionEvent.ACTION_UP -> {
                isPressed = false

                val shouldPerformClick = !(longPressTriggered || swipeTriggered || repeatStarted)

                if (longPressEnabled) {
                    longPressTriggered = false
                    longPressJob?.cancel()
                    longPressJob = null
                }
                if (repeatEnabled) {
                    repeatJob?.cancel()
                    repeatJob = null
                    repeatStarted = false
                }
                if (swipeEnabled) {
                    swipeXUnconsumed = 0f
                    swipeYUnconsumed = 0f
                    swipeTriggered = false
                }

                if (shouldPerformClick) {
                    if (doubleTapEnabled) {
                        if (firstClick && System.currentTimeMillis() - lastClickTime <= ViewConfiguration.getDoubleTapTimeout()) {
                            onDoubleTapListener?.invoke(this)
                            firstClick = false
                        } else {
                            firstClick = true
                            lastClickTime = System.currentTimeMillis()
                            performClick()
                        }
                    } else performClick()
                }
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (swipeEnabled) {
                    swipeXUnconsumed += event.x - swipeLastX
                    swipeYUnconsumed += event.y - swipeLastY
                    swipeLastX = event.x
                    swipeLastY = event.y
                    while (swipeXUnconsumed.absoluteValue > swipeThresholdX) {
                        if ((longPressTriggered || swipeTriggered) && !swipeRepeatEnabled) return true
                        swipeTriggered = true
                        val direction = swipeXUnconsumed.sign
                        swipeXUnconsumed -= direction * swipeThresholdX
                        if (direction > 0) {
                            onSwipeRightListener?.invoke(this)
                        } else {
                            onSwipeLeftListener?.invoke(this)
                        }
                    }
                    while (swipeYUnconsumed.absoluteValue > swipeThresholdY) {
                        if ((longPressTriggered || swipeTriggered) && !swipeRepeatEnabled) return true
                        swipeTriggered = true
                        val direction = swipeYUnconsumed.sign
                        swipeYUnconsumed -= direction * swipeThresholdX
                        if (direction > 0) {
                            onSwipeDownListener?.invoke(this)
                        } else {
                            onSwipeUpListener?.invoke(this)
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
        val longPressDelay by Prefs.getInstance().longPressDelay

        const val initialInterval: Long = 200L
        const val endInterval: Long = 30L
        const val accelerateTime: Long = 1000L
    }
}
