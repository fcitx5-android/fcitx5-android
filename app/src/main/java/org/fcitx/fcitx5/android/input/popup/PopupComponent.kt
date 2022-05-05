package org.fcitx.fcitx5.android.input.popup

import org.fcitx.fcitx5.android.data.theme.ThemeManager
import org.fcitx.fcitx5.android.input.dependency.context
import org.fcitx.fcitx5.android.input.dependency.theme
import org.mechdancer.dependency.Dependent
import org.mechdancer.dependency.UniqueComponent
import org.mechdancer.dependency.manager.ManagedHandler
import org.mechdancer.dependency.manager.managedHandler
import splitties.dimensions.dp
import splitties.views.dsl.core.add
import splitties.views.dsl.core.frameLayout
import splitties.views.dsl.core.lParams
import timber.log.Timber
import java.util.*

class PopupComponent :
    UniqueComponent<PopupComponent>(), Dependent, ManagedHandler by managedHandler() {

    private val context by manager.context()
    private val theme by manager.theme()

    private val showingEntryUi = HashMap<Int, PopupEntryUi>()
    private val freeEntryUi = LinkedList<PopupEntryUi>()

    private val keyBottomMargin by lazy {
        context.dp(ThemeManager.prefs.keyVerticalMargin.getValue())
    }
    private val popupWidth by lazy {
        context.dp(36)
    }
    private val popupHeight by lazy {
        context.dp(115)
    }

    val view by lazy {
        context.frameLayout {
            isClickable = false
            isFocusable = false
        }
    }

    fun showPopup(viewId: Int, character: String, left: Int, top: Int, right: Int, bottom: Int) {
        Timber.d("showPopup('$character', left=$left, top=$top, right=$right, bottom=$bottom)")
        showingEntryUi[viewId]?.apply {
            setText(character)
            return
        }
        val popup = (freeEntryUi.removeFirstOrNull() ?: PopupEntryUi(context, theme)).apply {
            setText(character)
        }
        view.apply {
            add(popup.root, lParams(popupWidth, popupHeight) {
                topMargin = bottom - popupHeight - keyBottomMargin
                leftMargin = (left + right - popupWidth) / 2
            })
        }
        showingEntryUi[viewId] = popup
    }

    fun dismissPopup(viewId: Int) {
        showingEntryUi[viewId]?.also {
            showingEntryUi.remove(viewId)
            view.removeView(it.root)
            freeEntryUi.add(it)
        }
    }
}
