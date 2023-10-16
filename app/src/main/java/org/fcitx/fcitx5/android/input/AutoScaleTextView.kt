package org.fcitx.fcitx5.android.input

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.widget.TextView
import androidx.core.graphics.withSave
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@SuppressLint("AppCompatCustomView")
class AutoScaleTextView @JvmOverloads constructor(
    context: Context?,
    attributeSet: AttributeSet? = null
) : TextView(context, attributeSet) {

    enum class Mode {
        /**
         * do not scale or ellipse text, overflow when cannot fit width
         */
        None,
        /**
         * only scale in X axis, makes text looks "condensed" or "slim"
         */
        Horizontal,
        /**
         * scale both in X and Y axis, align center vertically
         */
        Proportional
    }

    var scaleMode = Mode.None

    private lateinit var text: String

    private var needsMeasureText = true
    private val fontMetrics = Paint.FontMetrics()
    private val textBounds = Rect()

    private var needsCalculateTransform = true
    private var translateY = 0.0f
    private var translateX = 0.0f
    private var textScaleX = 1.0f
    private var textScaleY = 1.0f

    override fun setText(charSequence: CharSequence?, bufferType: BufferType) {
        // setText can be called in super constructor
        if (!::text.isInitialized || charSequence == null || !text.contentEquals(charSequence)) {
            needsMeasureText = true
            needsCalculateTransform = true
            text = charSequence?.toString() ?: ""
            requestLayout()
            invalidate()
        }
    }

    override fun getText(): CharSequence {
        return text
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val width = measureTextBounds().width() + paddingLeft + paddingRight
        val height = ceil(fontMetrics.bottom - fontMetrics.top + paddingTop + paddingBottom).toInt()
        val maxHeight = if (maxHeight >= 0) maxHeight else Int.MAX_VALUE
        val maxWidth = if (maxWidth >= 0) maxWidth else Int.MAX_VALUE
        setMeasuredDimension(
            measure(widthMode, widthSize, min(max(width, minimumWidth), maxWidth)),
            measure(heightMode, heightSize, min(max(height, minimumHeight), maxHeight))
        )
    }

    private fun measure(specMode: Int, specSize: Int, calculatedSize: Int): Int = when (specMode) {
        MeasureSpec.EXACTLY -> specSize
        MeasureSpec.AT_MOST -> min(calculatedSize, specSize)
        else -> calculatedSize
    }

    private fun measureTextBounds(): Rect {
        if (needsMeasureText) {
            val paint = paint
            paint.getFontMetrics(fontMetrics)
            val codePointCount = Character.codePointCount(text, 0, text.length)
            if (codePointCount == 1) {
                // use actual text bounds when there is only one "character",
                // eg. full-width punctuation
                paint.getTextBounds(text, 0, text.length, textBounds)
            } else {
                textBounds.set(
                    /* left = */ 0,
                    /* top = */ floor(fontMetrics.top).toInt(),
                    /* right = */ ceil(paint.measureText(text)).toInt(),
                    /* bottom = */ ceil(fontMetrics.bottom).toInt()
                )
            }
            needsMeasureText = false
        }
        return textBounds
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        if (needsCalculateTransform || changed) {
            calculateTransform(right - left, bottom - top)
            needsCalculateTransform = false
        }
    }

    private fun calculateTransform(viewWidth: Int, viewHeight: Int) {
        val contentWidth = viewWidth - paddingLeft - paddingRight
        val contentHeight = viewHeight - paddingTop - paddingBottom
        measureTextBounds()
        val textWidth = textBounds.width()
        val leftAlignOffset = (paddingLeft - textBounds.left).toFloat()
        val centerAlignOffset =
            paddingLeft.toFloat() + (contentWidth - textWidth) / 2.0f - textBounds.left.toFloat()

        @SuppressLint("RtlHardcoded")
        val shouldAlignLeft = gravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.LEFT
        if (textWidth >= contentWidth) {
            when (scaleMode) {
                Mode.None -> {
                    textScaleX = 1.0f
                    textScaleY = 1.0f
                    translateX = if (shouldAlignLeft) leftAlignOffset else centerAlignOffset
                }
                Mode.Horizontal -> {
                    textScaleX = contentWidth.toFloat() / textWidth.toFloat()
                    textScaleY = 1.0f
                    translateX = leftAlignOffset
                }
                Mode.Proportional -> {
                    val textScale = contentWidth.toFloat() / textWidth.toFloat()
                    textScaleX = textScale
                    textScaleY = textScale
                    translateX = leftAlignOffset
                }
            }
        } else {
            translateX = if (shouldAlignLeft) leftAlignOffset else centerAlignOffset
            textScaleX = 1.0f
            textScaleY = 1.0f
        }
        val fontHeight = (fontMetrics.bottom - fontMetrics.top) * textScaleY
        val fontOffsetY = fontMetrics.top * textScaleY
        translateY = (contentHeight.toFloat() - fontHeight) / 2.0f - fontOffsetY + paddingTop
    }

    override fun onDraw(canvas: Canvas) {
        if (needsCalculateTransform) {
            calculateTransform(width, height)
            needsCalculateTransform = false
        }
        val paint = paint
        paint.color = currentTextColor
        canvas.withSave {
            translate(scrollX.toFloat(), scrollY.toFloat())
            scale(textScaleX, textScaleY, 0f, translateY)
            translate(translateX, translateY)
            drawText(text, 0f, 0f, paint)
        }
    }

    override fun getTextScaleX(): Float {
        return textScaleX
    }
}
