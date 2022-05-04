package org.fcitx.fcitx5.android.input.popup

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

    private val showingEntryUi = HashMap<String, PopupEntryUi>()
    private val freeEntryUi = ArrayDeque<PopupEntryUi>()

    private val popupHeight by lazy { context.dp(115) }

    val view by lazy {
        context.frameLayout {
            isClickable = false
            isFocusable = false
        }
    }

    fun showPopup(character: String, left: Int, top: Int, right: Int, bottom: Int) {
        Timber.d("showPopup('$character', left=$left, top=$top, right=$right, bottom=$bottom)")
        val popup = freeEntryUi.poll()?.apply { textView.text = character }
            ?: PopupEntryUi(context, theme, character)
        view.apply {
            add(popup.root, lParams(dp(36), popupHeight) {
                topMargin = bottom - popupHeight
                leftMargin = left
            })
        }
        showingEntryUi[character] = popup
    }

    fun dismissPopup(character: String) {
        showingEntryUi[character]?.also {
            showingEntryUi.remove(character)
            view.removeView(it.root)
            freeEntryUi.add(it)
        }
    }
}
