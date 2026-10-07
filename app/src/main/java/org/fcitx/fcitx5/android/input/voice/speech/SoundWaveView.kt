/*
 * SPDX-License-Identifier: MIT
 * SPDX-FileCopyrightText: Copyright (C) 2021 ultimateHandsomeBoy666
 *
 * Vendored from https://github.com/ultimateHandsomeBoy666/SoundView (soundwavelib)
 * and adapted for programmatic (splitties DSL) instantiation:
 * - XML attributes are not read; all properties are configured in code after construction
 * - volumeCount rebuilds the volume bar list instead of keeping stale bars
 * - bar width follows volumeBarHalfWidth changes instead of a constructor-time snapshot
 * - the animation loop starts on attach when visible, not only on visibility changes
 */
package org.fcitx.fcitx5.android.input.voice.speech

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.View
import android.view.animation.Interpolator
import kotlin.math.pow
import kotlin.random.Random

class VolumeDanceInterpolator : Interpolator {

    override fun getInterpolation(f: Float): Float {
        val a = -2.8f
        val b = 3.8f
        return a * f.pow(2) + b * f
    }
}

private class State {
    companion object {
        const val INIT = -1
        const val IDLE = 0
        const val DANCE = 1
        const val STOP = 2
    }
}

@Suppress("MemberVisibilityCanBePrivate")
open class SoundWaveView(context: Context) : View(context) {

    inner class VolumeBar {
        private var height: Int = 0
        private var danceStartTime: Long = 0
        private var danceStartDelay: Int = 0
        private var idleStartTime: Long = 0

        // read dynamically so DSL users can change volumeBarHalfWidth after construction
        private val halfWidth: Int get() = volumeBarHalfWidth

        fun draw(
            canvas: Canvas?,
            paint: Paint,
            moveCanvas: Boolean,
            @Suppress("UNUSED_PARAMETER") state: Int,
            index: Int
        ) {
            canvas ?: return
            // reset idleStartTime outside idle state so easing restarts on fast state switches
            if (state != State.IDLE) {
                idleStartTime = 0
            }
            when (state) {
                State.IDLE -> {
                    drawIdle(canvas, paint, moveCanvas, index)
                }
                State.DANCE -> {
                    drawDance(canvas, paint, moveCanvas, index)
                }
                State.INIT, State.STOP -> {
                    drawStop(canvas, paint, moveCanvas)
                }
            }
        }

        private fun drawStop(canvas: Canvas, paint: Paint, moveCanvas: Boolean) {
            if (moveCanvas) {
                canvas.translate((volumeBarMargin + 2 * volumeBarHalfWidth).toFloat(), 0f)
            }
            val rectF = RectF(
                -halfWidth.toFloat(), -minVolumeBarHeight.toFloat() / 2,
                halfWidth.toFloat(), minVolumeBarHeight.toFloat() / 2
            )
            canvas.drawRoundRect(
                rectF, halfWidth.toFloat(),
                halfWidth.toFloat(), paint
            )
        }

        private fun drawIdle(canvas: Canvas, paint: Paint, moveCanvas: Boolean, index: Int) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - idleStartTime > idleDuration) {
                idleStartTime = currentTime
            }
            val idleStart =
                (1f * (currentTime - idleStartTime) / idleDuration * (volumeCount / 2 + volumeIdleCount + 4)).toInt()
            val mid = volumeCount / 2
            val x = if (index >= mid) {
                mid + idleStart - index
            } else {
                index - (mid - idleStart)
            }
            val drawHeight = if (x in 0..volumeIdleCount / 2) {
                dp2px(getIdleHeight(x)).toFloat()
            } else if (x in volumeIdleCount / 2 + 1..volumeIdleCount) {
                dp2px(getIdleHeight(volumeIdleCount - x)).toFloat()
            } else {
                minVolumeBarHeight.toFloat()
            }

            if (moveCanvas) {
                canvas.translate((volumeBarMargin + 2 * volumeBarHalfWidth).toFloat(), 0f)
            }
            val rectF = RectF(
                -halfWidth.toFloat(), -drawHeight / 2,
                halfWidth.toFloat(), drawHeight / 2
            )
            canvas.drawRoundRect(
                rectF, halfWidth.toFloat(),
                halfWidth.toFloat(), paint
            )
        }

        private fun drawDance(canvas: Canvas, paint: Paint, moveCanvas: Boolean, index: Int) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - danceStartTime > danceDuration) {
                danceStartDelay = random.nextInt(maxDanceDelay)
                danceStartTime = currentTime + danceStartDelay
                val volume = currentVolume.coerceAtLeast(minVolume).coerceAtMost(maxVolume)
                val rawHeight =
                    1f * (volume - minVolume) / (maxVolume - minVolume) * (maxVolumeBarHeight - minVolumeBarHeight) + minVolumeBarHeight
                val heightPortion = if (index in 0 until volumeHeightDistribution.size) {
                    volumeHeightDistribution[index]
                } else {
                    1f
                }
                height = (rawHeight * heightPortion).toInt()
            }
            val fraction = 1f * (currentTime - danceStartTime) / danceDuration
            val drawHeight = if (fraction in 0f..1f) {
                ((interpolator.getInterpolation(fraction) * height)
                    .coerceAtLeast(minVolumeBarHeight.toFloat()))
                    .coerceAtMost(maxVolumeBarHeight.toFloat())
            } else {
                minVolumeBarHeight.toFloat()
            }

            if (moveCanvas) {
                canvas.translate((volumeBarMargin + 2 * volumeBarHalfWidth).toFloat(), 0f)
            }
            val rectF = RectF(
                -halfWidth.toFloat(), -drawHeight / 2,
                halfWidth.toFloat(), drawHeight / 2
            )
            canvas.drawRoundRect(
                rectF, halfWidth.toFloat(),
                halfWidth.toFloat(), paint
            )
        }
    }

    var volumeCount = 37
        set(value) {
            field = value
            rebuildVolumeBars()
        }

    // bars on both sides that enter easing mode
    var volumeIdleCount = 16
    var maxVolume = 35
    var minVolume = 4
    var danceDuration = 250
    var maxDanceDelay = 80
    var idleDuration = 3500

    var maxIdleHeight = dp2px(16f)
    var minVolumeBarHeight = dp2px(8f)
    var maxVolumeBarHeight = dp2px(36f)
    var volumeBarMargin = dp2px(3f)
    var volumeBarHalfWidth = dp2px(1.5f)
    var volumeBarColor = Color.parseColor("#A0000000")
        set(value) {
            field = value
            volumePaint.color = value
        }
    var volumeHeightDistribution = floatArrayOf(
        0.6f, 0.6f, 0.6f, 0.8f, 1f, 1.1f, 0.95f, 0.9f, 0.8f,
        0.75f, 0.8f, 0.9f, 0.95f, 1f, 1.1f, 1.2f, 1.5f, 1.4f, 1.3f,
        1.4f, 1.5f, 1.2f, 1.1f, 1f, 0.95f, 0.9f, 0.8f, 0.75f, 0.8f,
        0.9f, 0.95f, 1.1f, 1f, 0.8f, 0.6f, 0.6f, 0.6f
    )

    private var state = State.INIT
    private var volumeBarList = mutableListOf<VolumeBar>()
    private var currentVolume = -1
    private var interpolator: Interpolator = VolumeDanceInterpolator()
    private val random = Random(1)
    private val volumePaint = Paint()
    private var idleHeightGetter: (Int) -> Float = { x ->
        (x + 4).toFloat()
    }
    private var volumeChangeSmoother: (Int, Int) -> Int = { currentVolume, newVolume ->
        (currentVolume + newVolume) / 2
    }

    private var enableIdle = true

    private val disableAnimation by org.fcitx.fcitx5.android.data.prefs.AppPrefs
        .getInstance().advanced.disableAnimation

    private val animationRunnable: Runnable = object : Runnable {
        override fun run() {
            invalidate()
            if (!disableAnimation) post(this)
        }
    }

    init {
        rebuildVolumeBars()
        volumePaint.isAntiAlias = true
        volumePaint.color = volumeBarColor
    }

    private fun rebuildVolumeBars() {
        volumeBarList = MutableList(volumeCount) { VolumeBar() }
        requestLayout()
        invalidate()
    }

    private fun dp2px(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }

    /**
     * Feed a volume value; the view keeps dancing (or idling) by itself between calls.
     */
    fun handleVolume(newVolume: Int) {
        if (currentVolume < 0) {
            currentVolume = newVolume
        } else {
            currentVolume = smoothVolumeChange(newVolume)
        }
        state = if ((currentVolume < minVolume) && enableIdle) {
            State.IDLE
        } else {
            State.DANCE
        }
    }

    private fun smoothVolumeChange(newVolume: Int): Int {
        return volumeChangeSmoother.invoke(currentVolume, newVolume)
    }

    private fun getIdleHeight(x: Int): Float {
        return idleHeightGetter.invoke(x)
    }

    fun setVolumeChangeSmoother(smoother: (Int, Int) -> Int) {
        this.volumeChangeSmoother = smoother
    }

    fun setIdleHeightGetter(idleHeightGetter: (x: Int) -> Float) {
        this.idleHeightGetter = idleHeightGetter
    }

    fun setDistribution(distribution: FloatArray) {
        volumeHeightDistribution = distribution
    }

    fun setInterpolator(interpolator: Interpolator) {
        this.interpolator = interpolator
    }

    fun enableIdle(enable: Boolean) {
        enableIdle = enable
    }

    fun stopDance() {
        state = State.STOP
    }

    fun setColor(color: Int) {
        volumePaint.color = color
    }

    fun isStop(): Boolean {
        return state == State.STOP
    }

    fun getWaveAreaWidth(): Int {
        val size: Int = volumeBarList.size
        return size * 2 * volumeBarHalfWidth + (size - 1) * volumeBarMargin
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width: Int
        val height: Int

        val specWidth = MeasureSpec.getSize(widthMeasureSpec)
        width = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> {
                specWidth
            }
            MeasureSpec.AT_MOST -> {
                val expectWidth = paddingStart + paddingEnd +
                    volumeCount * volumeBarHalfWidth * 2 +
                    volumeBarMargin * (volumeCount - 1)
                if (expectWidth < specWidth) {
                    expectWidth
                } else {
                    specWidth
                }
            }
            else -> {
                paddingStart + paddingEnd +
                    volumeCount * volumeBarHalfWidth * 2 +
                    volumeBarMargin * (volumeCount - 1)
            }
        }

        val specHeight = MeasureSpec.getSize(heightMeasureSpec)
        height = when (MeasureSpec.getMode(heightMeasureSpec)) {
            MeasureSpec.EXACTLY -> {
                specHeight
            }
            MeasureSpec.AT_MOST -> {
                val expectHeight = maxVolumeBarHeight + paddingTop + paddingBottom
                if (expectHeight < specHeight) {
                    expectHeight
                } else {
                    specHeight
                }
            }
            else -> {
                maxVolumeBarHeight + paddingTop + paddingBottom
            }
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas) {
        if (volumeBarList.isEmpty()) {
            return
        }

        canvas.save()
        // move canvas to the middle position
        canvas.translate((measuredWidth / 2).toFloat(), (measuredHeight / 2).toFloat())

        // draw the middle volume bar
        val midPosition: Int = (volumeBarList.size - 1) / 2
        val midVolumeBar = volumeBarList[midPosition]
        midVolumeBar.draw(canvas, volumePaint, false, state, midPosition)

        canvas.save()
        // draw right bars
        for (i in midPosition + 1 until volumeBarList.size) {
            val volumeBar = volumeBarList[i]
            volumeBar.draw(canvas, volumePaint, true, state, i)
        }

        canvas.restore()
        canvas.rotate(180f)
        // draw left bars
        for (i in midPosition - 1 downTo 0) {
            val volumeBar = volumeBarList[i]
            volumeBar.draw(canvas, volumePaint, true, state, i)
        }
        canvas.restore()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // DSL-created views may stay VISIBLE from the beginning, in which case
        // onVisibilityChanged is never invoked and the animation would never start
        if (visibility == VISIBLE && isShown) {
            state = State.INIT
            post(animationRunnable)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(animationRunnable)
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        when (visibility) {
            VISIBLE -> {
                state = State.INIT
                post(animationRunnable)
            }
            INVISIBLE, GONE -> {
                removeCallbacks(animationRunnable)
            }
        }
    }
}
