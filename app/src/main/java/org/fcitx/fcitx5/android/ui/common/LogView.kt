package org.fcitx.fcitx5.android.ui.common

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.textclassifier.TextClassifier
import android.widget.HorizontalScrollView
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.Logcat
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.core.*
import splitties.views.padding

class LogView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) :
    NestedScrollView(context, attributeSet) {

    private var logcat: Logcat? = null

    private val textView = textView {
        padding = dp(4)
        textSize = 12f
        typeface = Typeface.MONOSPACE
        setTextIsSelectable(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setTextClassifier(object : TextClassifier {})
        }
    }

    private val scrollView = view(::HorizontalScrollView) {
        add(textView, lParams(matchParent, matchParent))
    }

    init {
        add(scrollView, lParams(matchParent, matchParent))
    }


    override fun onDetachedFromWindow() {
        logcat?.shutdownLogFlow()
        super.onDetachedFromWindow()
    }

    fun setLogcat(logcat: Logcat) {
        this.logcat = logcat
        logcat.initLogFlow()
        logcat.logFlow.onEach {
            val color = styledColor(
                when (it.first()) {
                    'V' -> R.attr.colorLogVerbose
                    'D' -> R.attr.colorLogDebug
                    'I' -> R.attr.colorLogInfo
                    'W' -> R.attr.colorLogWarning
                    'E' -> R.attr.colorLogError
                    'F' -> R.attr.colorLogFatal
                    else -> android.R.attr.colorForeground
                }
            )
            val colored = SpannableString(it).apply {
                setSpan(
                    ForegroundColorSpan(color),
                    0,
                    it.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            textView.append(colored)
            textView.append("\n")
        }.launchIn(findViewTreeLifecycleOwner()!!.lifecycleScope)
    }

    val currentLog: CharSequence
        get() = textView.text ?: ""

    fun clear() {
        textView.text = ""
    }
}