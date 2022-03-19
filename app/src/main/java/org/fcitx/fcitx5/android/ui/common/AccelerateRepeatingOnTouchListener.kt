package org.fcitx.fcitx5.android.ui.common

import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.exp
import kotlin.math.ln

class AccelerateRepeatingOnTouchListener(
    private val firstClickInterval: Long,
    private val initialInterval: Long,
    private val endInterval: Long,
    private val accelerateTime: Long
) : View.OnTouchListener {

    init {
        if (initialInterval <= 0)
            throw IllegalArgumentException("Initial interval $initialInterval should >= 0")
        if (endInterval > initialInterval)
            throw IllegalArgumentException("End interval $endInterval should < initial interval $initialInterval")
        if (accelerateTime <= 0)
            throw IllegalArgumentException("Accelerate time $accelerateTime should > 0")
    }

    private var job: Job? = null

    // interval is exponentially decreasing
    // so we are very slow at first, but become faster and faster until we reach [endInterval] within [accelerateTime]
    private fun calculateInterval(t: Long) =
        if (t > accelerateTime) endInterval
        else
            (initialInterval * exp(-ln(initialInterval.toDouble() / endInterval) / accelerateTime * t)).toLong()

    override fun onTouch(view: View, event: MotionEvent): Boolean =
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                job?.cancel()
                job = (view.findViewTreeLifecycleOwner()?.lifecycleScope!!).launch {
                    delay(firstClickInterval)
                    val t0 = System.currentTimeMillis()
                    while (isActive && view.isEnabled) {
                        val t = System.currentTimeMillis() - t0
                        delay(calculateInterval(t))
                        view.performClick()
                    }
                }
                view.isPressed = true
                true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                job?.cancel()
                job = null
                view.isPressed = false
                true
            }
            else -> false
        }
}