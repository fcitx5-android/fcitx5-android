package me.rocka.fcitx5test.keyboard.layout

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.absoluteValue

abstract class MyOnGestureListener : GestureDetector.SimpleOnGestureListener() {
    open fun onDoubleTap(): Boolean = false
    open fun onSwipeUp(): Boolean = false
    open fun onSwipeDown(): Boolean = false
    open fun onRawTouchEvent(motionEvent: MotionEvent) = false

    override fun onDoubleTapEvent(e: MotionEvent?) = onDoubleTap()
    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent?,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        if (e1 == null || e2 == null) return false
        val deltaY = e2.y - e1.y
        if (deltaY.absoluteValue > THRESHOLD && velocityY > THRESHOLD)
            when {
                deltaY > 0 -> return onSwipeDown()
                deltaY < 0 -> return onSwipeUp()
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