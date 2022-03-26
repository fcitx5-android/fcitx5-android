package org.fcitx.fcitx5.android.ui.common

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import cn.berberman.girls.utils.identity
import com.google.android.material.snackbar.Snackbar
import org.fcitx.fcitx5.android.R
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.coordinatorlayout.coordinatorLayout
import splitties.views.dsl.coordinatorlayout.defaultLParams
import splitties.views.dsl.core.*
import splitties.views.dsl.material.floatingActionButton
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityEndBottom
import splitties.views.imageResource
import splitties.views.recyclerview.verticalLayoutManager

abstract class BaseDynamicListUi<T>(
    override val ctx: Context,
    private val mode: Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean = false,
    initCheckBox: (CheckBox.(Int) -> Unit) = { visibility = View.GONE },
    initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE }
) : Ui,
    DynamicListAdapter<T>(
        initialEntries,
        enableAddAndDelete = mode !is Mode.Immutable,
        enableOrder,
        initCheckBox,
        initEditButton = {},
        initSettingsButton
    ) {

    protected val fab = floatingActionButton {
        imageResource = R.drawable.ic_baseline_plus_24
        colorFilter = PorterDuffColorFilter(
            styledColor(android.R.attr.colorForegroundInverse), PorterDuff.Mode.SRC_IN
        )
    }

    sealed class Mode<T> {
        /**
         * Pick one from a list of [T]
         */
        data class ChooseOne<T>(val candidatesSource: BaseDynamicListUi<T>.() -> Array<T>) :
            Mode<T>()

        /**
         * Enter a string that can be converted into [T]
         */
        data class FreeAdd<T>(
            val hint: String,
            val converter: (String) -> T,
            val validator: (String) -> Boolean = { it.isNotBlank() }
        ) : Mode<T>()

        class Immutable<T> : Mode<T>()

        /**
         * Do nothing
         */
        class Custom<T> : Mode<T>()

        @Suppress("FunctionName")
        companion object {
            fun FreeAddString() = FreeAdd("", ::identity)
        }
    }

    init {
        initEditButton = when (mode) {
            is Mode.ChooseOne -> { _ -> visibility = View.GONE }
            is Mode.FreeAdd -> { idx ->
                visibility = View.VISIBLE
                setOnClickListener {
                    val entry = entries[idx]
                    showEditDialog(ctx.getString(R.string.edit), entry) {
                        if (it != entry) updateItem(idx, it)
                    }
                }
            }
            is Mode.Immutable -> { _ -> visibility = View.GONE }
            is Mode.Custom -> { _ -> visibility = View.GONE }
        }
        addOnItemChangedListener(object : OnItemChangedListener<T> {
            override fun onItemAdded(idx: Int, item: T) {
                updateFAB()
                showUndoSnackbar(ctx.getString(R.string.added_x, showEntry(item))) { removeItem(idx) }
            }

            override fun onItemRemoved(idx: Int, item: T) {
                updateFAB()
                showUndoSnackbar(ctx.getString(R.string.removed_x, showEntry(item))) { addItem(idx, item) }
            }

            override fun onItemUpdated(idx: Int, old: T, new: T) {
                updateFAB()
                if (mode !is Mode.Immutable) {
                    showUndoSnackbar(ctx.getString(R.string.edited_x, showEntry(new))) {
                        updateItem(idx, old)
                    }
                }
            }
        })
    }

    private fun showUndoSnackbar(text: String, action: View.OnClickListener) {
        Snackbar.make(fab, text, Snackbar.LENGTH_SHORT)
            .setAction(R.string.undo, action)
            .show()
    }

    open fun updateFAB() {
        when (mode) {
            is Mode.ChooseOne -> {
                val candidatesSource = mode.candidatesSource(this)
                if (candidatesSource.isEmpty()) {
                    fab.hide()
                } else {
                    fab.show()
                    fab.setOnClickListener {
                        val items = candidatesSource.map { showEntry(it) }.toTypedArray()
                        AlertDialog.Builder(ctx)
                            .setTitle(R.string.add)
                            .setItems(items) { _, which -> addItem(item = candidatesSource[which]) }
                            .show()
                    }
                }
            }
            is Mode.FreeAdd -> {
                fab.show()
                fab.setOnClickListener {
                    showEditDialog(ctx.getString(R.string.add)) { addItem(item = it) }
                }
            }
            is Mode.Immutable -> fab.hide()
            is Mode.Custom -> {
            }
        }
    }

    open fun showEditDialog(
        title: String,
        entry: T? = null,
        block: (T) -> Unit
    ) {
        if (mode !is Mode.FreeAdd) return
        val editText = editText {
            hint = mode.hint
            if (entry != null) {
                setText(showEntry(entry))
            }
            addTextChangedListener { error = null }
        }
        val layout = constraintLayout {
            add(editText, lParams {
                height = wrapContent
                width = matchParent
                topOfParent()
                bottomOfParent()
                leftOfParent(dp(20))
                rightOfParent(dp(20))
            })
        }
        val dialog = AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok) { _, _ ->
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
        editText.requestFocus()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val t = editText.editableText.toString()
            if (mode.validator(t)) {
                block(mode.converter(t))
                dialog.dismiss()
            } else {
                editText.error = ctx.getString(R.string.invalid_value)
            }
        }
    }

    protected val recyclerView = recyclerView {
        adapter = this@BaseDynamicListUi
        layoutManager = verticalLayoutManager()
        var fabShown = true
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && fabShown) {
                    fabShown = false
                    val offset = fab.run {
                        height + (layoutParams as CoordinatorLayout.LayoutParams).bottomMargin
                    }.toFloat()
                    fab.animate().setDuration(150L).translationY(offset)
                } else if (dy < 0 && !fabShown) {
                    fabShown = true
                    fab.animate().setDuration(150L).translationY(0f)
                }
            }
        })
    }

    fun addTouchCallback(
        touchCallback: DynamicListTouchCallback<T> =
            DynamicListTouchCallback(this@BaseDynamicListUi)
    ) {
        ItemTouchHelper(touchCallback).attachToRecyclerView(recyclerView)
    }

    override val root: View = coordinatorLayout {
        add(recyclerView, defaultLParams {
            height = matchParent
            width = matchParent
        })
        add(fab, defaultLParams {
            gravity = gravityEndBottom
            margin = dp(16)
        })
        updateFAB()
    }

}