package org.fcitx.fcitx5.android.ui.main.log

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.widget.HorizontalScrollView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.Logcat
import splitties.resources.styledColor
import splitties.views.dsl.core.add
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.recyclerview.verticalLayoutManager

class LogView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null) :
    HorizontalScrollView(context, attributeSet) {

    private var logcat: Logcat? = null

    private val logAdapter = LogAdapter()

    private val rv = recyclerView {
        adapter = logAdapter
        layoutManager = verticalLayoutManager()
    }

    init {
        add(rv, lParams(wrapContent, matchParent))
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
            logAdapter.append(SpannableString(it).apply {
                setSpan(
                    ForegroundColorSpan(color),
                    0,
                    it.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            })
        }.launchIn(findViewTreeLifecycleOwner()!!.lifecycleScope)
    }

    val currentLog: String
        get() = logAdapter.fullLogString()

    fun clear() {
        logAdapter.clear()
    }
}