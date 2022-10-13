package org.fcitx.fcitx5.android.ui.main.log

import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.textclassifier.TextClassifier
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import splitties.dimensions.dp
import splitties.views.dsl.core.endMargin
import splitties.views.dsl.core.startMargin
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent

class LogAdapter(private val entries: ArrayList<SpannableString> = ArrayList()) :
    RecyclerView.Adapter<LogAdapter.Holder>() {
    inner class Holder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    fun append(line: SpannableString) {
        val size = entries.size
        entries.add(line)
        notifyItemInserted(size)
    }

    fun clear() {
        val size = entries.size
        entries.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun fullLogString() = entries.joinToString("\n")

    override fun getItemCount() = entries.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        parent.textView {
            textSize = 12f
            typeface = Typeface.MONOSPACE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setTextClassifier(TextClassifier.NO_OP)
            }
            layoutParams = MarginLayoutParams(wrapContent, wrapContent).apply {
                startMargin = dp(4)
                endMargin = dp(4)
            }
        }
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.textView.text = entries[position]
    }
}
