package org.fcitx.fcitx5.android.data.theme

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import org.fcitx.fcitx5.android.utils.resource.toColorFilter
import splitties.resources.styledDrawable
import splitties.views.backgroundColor

fun Theme.applyBarIconColor(view: ImageView) {
    view.apply {
        background = styledDrawable(android.R.attr.actionBarItemBackground)
        colorFilter = funKeyColor.toColorFilter(PorterDuff.Mode.SRC_IN).resolve(context)
    }
}

fun Theme.applyKeyAccentBackgroundColor(view: View) {
    view.apply {
        keyAccentBackgroundColor.let {
            backgroundColor = it.resolve(context)
        }
    }
}

fun Theme.applyBarColor(view: View) {
    view.apply {
        barColor.let {
            backgroundColor = it.resolve(context)
        }
    }
}

fun Theme.applyKeyTextColor(view: TextView) {
    view.apply {
        setTextColor(keyTextColor.resolve(context))
    }
}

fun Theme.applyKeyAltTextColor(view: TextView) {
    view.apply {
        setTextColor(keyAltTextColor.resolve(context))
    }
}
