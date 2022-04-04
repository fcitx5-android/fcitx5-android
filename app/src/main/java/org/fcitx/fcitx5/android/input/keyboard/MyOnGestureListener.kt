package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.absoluteValue

abstract class MyOnGestureListener : GestureDetector.SimpleOnGestureListener() {
    open fun onDoubleTap(): Boolean = false
    open fun onSwipeUp(displacement: Float, velocity: Float): Boolean = false
    open fun onSwipeDown(displacement: Float, velocity: Float): Boolean = false
    open fun onSwipeLeft(displacement: Float, velocity: Float): Boolean = false
    open fun onSwipeRight(displacement: Float, velocity: Float): Boolean = false
    open fun onRawTouchEvent(motionEvent: MotionEvent) = false
    override fun onDoubleTap(e: MotionEvent?): Boolean = onDoubleTap()
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null || e2 == null) return false
        val deltaX = e2.x - e1.x
        val deltaY = e2.y - e1.y
        if (deltaX.absoluteValue > THRESHOLD && velocityX > THRESHOLD)
            when {
                deltaX > 0 -> return onSwipeRight(deltaX, velocityX)
                deltaX < 0 -> return onSwipeLeft(-deltaX, -velocityX)
            }
        if (deltaY.absoluteValue > THRESHOLD && velocityY > THRESHOLD)
            when {
                deltaY > 0 -> return onSwipeDown(deltaX, velocityX)
                deltaY < 0 -> return onSwipeUp(-deltaX, -velocityX)
            }
        return false
    }

    companion object {
        private const val THRESHOLD = 100
    }
}

@SuppressLint("ClickableViewAccessibility")
fun View.setupOnGestureListener(listener: MyOnGestureListener) {
    isLongClickable = true
    val detector = GestureDetector(context, listener)
    setOnTouchListener { _, motionEvent ->
        detector.onTouchEvent(motionEvent) || listener.onRawTouchEvent(motionEvent)
    }
}