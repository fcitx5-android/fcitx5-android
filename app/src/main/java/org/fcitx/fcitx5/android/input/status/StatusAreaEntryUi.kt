package org.fcitx.fcitx5.android.input.status

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.icu.text.BreakIterator
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.AutoScaleTextView
import org.fcitx.fcitx5.android.input.keyboard.CustomGestureView
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.views.dsl.constraintlayout.above
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.centerOn
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.imageView
import splitties.views.dsl.core.lParams
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.gravityCenter
import splitties.views.imageDrawable

class StatusAreaEntryUi(override val ctx: Context, private val theme: Theme) : Ui {

    private val bkgDrawable = ShapeDrawable(OvalShape())

    val bkg = frameLayout {
        background = bkgDrawable
    }

    val icon = imageView {
        scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    val textIcon = view(::AutoScaleTextView) {
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20f)
        // keep original typeface, apply textStyle only
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // 600 = Semi Bold, 700 = Bold which is too heavy
            typeface = Typeface.create(typeface, 600, false)
        } else {
            setTypeface(typeface, Typeface.BOLD)
        }
    }

    val label = textView {
        textSize = 12f
        gravity = gravityCenter
        setTextColor(theme.keyTextColor)
    }

    override val root = object : CustomGestureView(ctx) {
        val content = constraintLayout {
            add(bkg, lParams(dp(48), dp(48)) {
                topOfParent(dp(4))
                centerHorizontally()
                above(label)
            })
            add(icon, lParams {
                centerOn(bkg)
            })
            add(textIcon, lParams(wrapContent, wrapContent) {
                centerOn(bkg)
            })
            add(label, lParams(wrapContent, wrapContent) {
                below(bkg, dp(6))
                centerHorizontally()
            })
        }

        init {
            add(content, lParams(matchParent, matchParent))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(96))
        }
    }

    fun setEntry(entry: StatusAreaEntry) {
        val contentColor =
            if (entry.active) theme.genericActiveForegroundColor else theme.keyTextColor
        if (entry.icon != 0) {
            icon.visibility = View.VISIBLE
            textIcon.visibility = View.GONE
            icon.imageDrawable = ctx.drawable(entry.icon)!!.apply {
                setTint(contentColor)
            }
        } else {
            icon.visibility = View.GONE
            textIcon.visibility = View.VISIBLE
            textIcon.text = getFirstCharacter(entry.label)
            textIcon.setTextColor(contentColor)
        }
        bkgDrawable.paint.color =
            if (entry.active) theme.genericActiveBackgroundColor else theme.keyBackgroundColor
        label.text = entry.label
    }

    private fun getFirstCharacter(s: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val iterator = BreakIterator.getCharacterInstance()
            iterator.setText(s)
            s.substring(iterator.first(), iterator.next())
        } else {
            s.substring(0, s.offsetByCodePoints(0, 1))
        }
    }
}
