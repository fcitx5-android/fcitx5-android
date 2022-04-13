package org.fcitx.fcitx5.android.input.status

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.Action
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import splitties.views.gravityCenter

class StatusAreaEntryUi(override val ctx: Context) : Ui {

    val icon = imageView {
    }

    val plate = view(::CardView) {
        radius = dp(24f)
        cardElevation = 0f
        add(icon, lParams { gravity = gravityCenter })
    }

    val label = textView {
        textSize = 12f
        gravity = gravityCenter
        setTextColor(styledColor(android.R.attr.colorForeground))
    }

    override val root = constraintLayout {
        add(plate, lParams(dp(48), dp(48)) {
            topOfParent(dp(4))
            startOfParent()
            endOfParent()
            above(label)
        })
        add(label, lParams(wrapContent, wrapContent) {
            below(plate, dp(6))
            startOfParent()
            endOfParent()
        })
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(90))
    }

    fun setAction(action: Action) {
        icon.setImageDrawable(drawableForIconName(action.icon))
        icon.colorFilter = foregroundColorForIconName(action.icon)
        plate.setCardBackgroundColor(backgroundColorForIconName(action.icon))
        label.text = action.shortText
    }

    private fun drawableForIconName(icon: String) = ctx.drawable(
        when (icon) {
            "fcitx-chttrans-active" -> R.drawable.ic_fcitx_status_chttrans_trad
            "fcitx-chttrans-inactive" -> R.drawable.ic_fcitx_status_chttrans_simp
            "fcitx-punc-active" -> R.drawable.ic_fcitx_status_punc_active
            "fcitx-punc-inactive" -> R.drawable.ic_fcitx_status_punc_inactive
            "fcitx-fullwidth-active" -> R.drawable.ic_fcitx_status_fullwidth_active
            "fcitx-fullwidth-inactive" -> R.drawable.ic_fcitx_status_fullwidth_inactive
            "fcitx-remind-active" -> R.drawable.ic_fcitx_status_prediction_active
            "fcitx-remind-inactive" -> R.drawable.ic_fcitx_status_prediction_inactive
            else -> when {
                icon.endsWith("-active") -> R.drawable.ic_baseline_code_24
                else -> R.drawable.ic_baseline_code_off_24
            }
        }
    )

    private fun foregroundColorForIconName(icon: String) = PorterDuffColorFilter(
        ctx.styledColor(
            when {
                icon.endsWith("-active") -> android.R.attr.colorForegroundInverse
                else -> android.R.attr.colorControlNormal
            }
        ), PorterDuff.Mode.SRC_IN
    )

    private fun backgroundColorForIconName(icon: String) = ctx.styledColor(
        when {
            icon.endsWith("-active") -> android.R.attr.colorAccent
            else -> android.R.attr.colorButtonNormal
        }
    )
}
