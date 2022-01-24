package me.rocka.fcitx5test.ui.common

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.ScrollView
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
    ScrollView(context, attributeSet) {

    private val textView = textView().also { add(it, lParams(matchParent, matchParent)) }
    private val logcat = Logcat()

    @SuppressLint("SetTextI18n")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        logcat.initLogFlow()
        logcat.logFlow.onEach {
            textView.text = textView.text.toString() + "\n" + it
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