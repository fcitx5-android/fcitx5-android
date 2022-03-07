package me.rocka.fcitx5test.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import me.rocka.fcitx5test.utils.Logcat
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView

class LogView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) :
    NestedScrollView(context, attributeSet) {

    private val textView = textView {
        setTextIsSelectable(true)
        setTextColor(Color.WHITE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setTextClassifier(object : TextClassifier {})
        }
    }
    private val logcat = Logcat()

    init {
        add(HorizontalScrollView(context).also {
            it.add(textView, lParams(matchParent, matchParent))
        }, lParams(matchParent, matchParent))
    }

    @SuppressLint("SetTextI18n")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        logcat.initLogFlow()
        logcat.logFlow.onEach {
            val colored = SpannableString(it)
            fun setColor(color: Int) {
                colored.setSpan(
                    ForegroundColorSpan(color),
                    0,
                    it.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            when (it.first()) {
                'D' -> setColor(Color.MAGENTA)
                'I' -> setColor(Color.GREEN)
                'W' -> setColor(Color.YELLOW)
                'E' -> setColor(Color.RED)
                else -> {}
            }
            textView.append(colored)
            textView.append("\n")
        }.launchIn(findViewTreeLifecycleOwner()!!.lifecycleScope)
    }

    override fun onDetachedFromWindow() {
        logcat.shutdownLogFlow()
        super.onDetachedFromWindow()
    }

    fun clear() {
        logcat.shutdownLogFlow()
        textView.text = ""
    }
}