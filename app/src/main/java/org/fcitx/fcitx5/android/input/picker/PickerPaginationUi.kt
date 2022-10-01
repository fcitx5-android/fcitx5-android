package org.fcitx.fcitx5.android.input.picker

import android.content.Context
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.updateLayoutParams
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.views.backgroundColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.core.*
import kotlin.math.roundToInt

class PickerPaginationUi(override val ctx: Context, val theme: Theme) : Ui {

    private val highlight = view(::View) {
        backgroundColor = theme.keyTextColor.color
    }

    override val root = constraintLayout {
        backgroundColor = ColorUtils.setAlphaComponent(theme.keyTextColor.color, 0x4c)
        add(highlight, lParams(matchConstraints, matchParent) {
            centerVertically()
            startOfParent()
            matchConstraintPercentWidth = 0f
        })
    }

    var pageCount: Int = 0
        set(value) {
            if (value == field) return
            field = value
            highlight.apply {
                if (value > 1) {
                    visibility = View.VISIBLE
                    updateLayoutParams<ConstraintLayout.LayoutParams> {
                        matchConstraintPercentWidth = 1f / value
                    }
                } else {
                    visibility = View.GONE
                }
            }
        }

    var currentPage: Int = 0
        set(value) {
            if (value == field) return
            field = value
            highlight.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startMargin = ((value + scrollProgress) * highlight.width).roundToInt()
            }
        }

    var scrollProgress: Float = 0f
        set(value) {
            if (value == field) return
            field = value
            highlight.updateLayoutParams<ConstraintLayout.LayoutParams> {
                startMargin = ((currentPage + value) * highlight.width).roundToInt()
            }
        }
}
