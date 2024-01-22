/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.ui.common

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import androidx.activity.OnBackPressedDispatcher
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import arrow.core.identity
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.onPositiveButtonClick
import org.fcitx.fcitx5.android.utils.str
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledColor
import splitties.views.backgroundColor
import splitties.views.bottomPadding
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.leftOfParent
import splitties.views.dsl.constraintlayout.rightOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.coordinatorlayout.coordinatorLayout
import splitties.views.dsl.coordinatorlayout.defaultLParams
import splitties.views.dsl.core.Ui
import splitties.views.dsl.core.add
import splitties.views.dsl.core.editText
import splitties.views.dsl.core.margin
import splitties.views.dsl.core.matchParent
import splitties.views.dsl.core.view
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityEndBottom
import splitties.views.imageDrawable
import splitties.views.recyclerview.verticalLayoutManager
import kotlin.math.min

abstract class BaseDynamicListUi<T>(
    override val ctx: Context,
    private val mode: Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean = false,
    initCheckBox: (CheckBox.(T) -> Unit) = { visibility = View.GONE },
    initSettingsButton: (ImageButton.(T) -> Unit) = { visibility = View.GONE },
) : Ui,
    DynamicListAdapter<T>(
        initialEntries,
        enableAddAndDelete = mode !is Mode.Immutable,
        enableOrder,
        initCheckBox,
        initEditButton = {},
        initSettingsButton
    ) {

    protected var shouldShowFab = false

    protected val fab = view(::FloatingActionButton) {
        imageDrawable = drawable(R.drawable.ic_baseline_plus_24)!!.apply {
            setTint(styledColor(android.R.attr.colorForegroundInverse))
        }
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

    /**
     * whether to show "undo" snackbar after item update
     */
    var enableUndo = true

    /**
     * suspend "undo" snackbar temporarily to prevent undo undo
     */
    private var suspendUndo = false

    init {
        initEditButton = when (mode) {
            is Mode.ChooseOne -> { _ -> visibility = View.GONE }
            is Mode.FreeAdd -> { entry ->
                visibility = View.VISIBLE
                setOnClickListener {
                    showEditDialog(ctx.getString(R.string.edit), entry) {
                        if (it != entry) updateItem(indexItem(entry), it)
                    }
                }
            }
            is Mode.Immutable -> { _ -> visibility = View.GONE }
            is Mode.Custom -> { _ -> visibility = View.GONE }
        }
        addOnItemChangedListener(object : OnItemChangedListener<T> {
            override fun onItemAdded(idx: Int, item: T) {
                updateFAB()
                showUndoSnackbar(ctx.getString(R.string.added_x, showEntry(item))) {
                    removeItem(idx)
                }
            }

            override fun onItemRemoved(idx: Int, item: T) {
                updateFAB()
                showUndoSnackbar(ctx.getString(R.string.removed_x, showEntry(item))) {
                    addItem(idx, item)
                }
            }

            override fun onItemUpdated(idx: Int, old: T, new: T) {
                updateFAB()
                if (mode is Mode.Immutable) return
                showUndoSnackbar(ctx.getString(R.string.edited_x, showEntry(new))) {
                    updateItem(idx, old)
                }
            }

            override fun onItemRemovedBatch(indexed: List<Pair<Int, T>>) {
                updateFAB()
                showUndoSnackbar(ctx.getString(R.string.removed_n_items, indexed.size)) {
                    indexed.sortedBy { it.first }.forEach {
                        addItem(it.first, it.second)
                    }
                }
            }
        })
    }

    private fun showUndoSnackbar(text: String, action: () -> Unit) {
        if (!enableUndo || suspendUndo) return
        Snackbar.make(root, text, Snackbar.LENGTH_SHORT)
            .setAction(R.string.undo) {
                suspendUndo = true
                action.invoke()
                suspendUndo = false
            }
            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onShown(transientBottomBar: Snackbar) {
                    // snackbar is invisible when it attached to parent,
                    // but change visibility won't trigger `onDependentViewChanged`.
                    // so we need to update fab position when snackbar fully shown
                    // see [^1]
                    fab.translationY = -transientBottomBar.view.height.toFloat()
                }
            })
            .show()
    }

    open fun updateFAB() {
        when (mode) {
            is Mode.ChooseOne -> {
                val candidatesSource = mode.candidatesSource(this)
                if (candidatesSource.isEmpty()) {
                    shouldShowFab = false
                    fab.hide()
                } else {
                    shouldShowFab = true
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
                shouldShowFab = true
                fab.show()
                fab.setOnClickListener {
                    showEditDialog(ctx.getString(R.string.add)) { addItem(item = it) }
                }
            }
            is Mode.Immutable -> {
                shouldShowFab = false
                fab.hide()
            }
            is Mode.Custom -> {
            }
        }
    }

    override fun enterMultiSelect(onBackPressedDispatcher: OnBackPressedDispatcher) {
        if (shouldShowFab) {
            fab.hide()
        }
        super.enterMultiSelect(onBackPressedDispatcher)
    }

    override fun exitMultiSelect() {
        if (shouldShowFab) {
            fab.show()
        }
        super.exitMultiSelect()
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
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setView(layout)
            .setPositiveButton(android.R.string.ok, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
            .onPositiveButtonClick {
                val str = editText.str
                if (mode.validator(str)) {
                    editText.error = null
                    block(mode.converter(str))
                    true
                } else {
                    editText.error = ctx.getString(R.string.invalid_value)
                    false
                }
            }
        editText.requestFocus()
    }

    protected val recyclerView = recyclerView {
        adapter = this@BaseDynamicListUi
        layoutManager = verticalLayoutManager()
        clipToPadding = false
    }

    fun addTouchCallback(
        touchCallback: DynamicListTouchCallback<T> = DynamicListTouchCallback(ctx, this)
    ) {
        itemTouchHelper = ItemTouchHelper(touchCallback).also {
            it.attachToRecyclerView(recyclerView)
        }
    }

    private fun updateViewMargin(insets: WindowInsetsCompat? = null) {
        val windowInsets = (insets ?: ViewCompat.getRootWindowInsets(root)) ?: return
        val navBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
        fab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            bottomMargin = navBars.bottom + ctx.dp(16)
        }
        recyclerView.bottomPadding = navBars.bottom
    }

    override val root = coordinatorLayout {
        backgroundColor = styledColor(android.R.attr.colorBackground)
        add(recyclerView, defaultLParams {
            height = matchParent
            width = matchParent
        })
        add(fab, defaultLParams {
            gravity = gravityEndBottom
            margin = dp(16)
            behavior = object : HideBottomViewOnScrollBehavior<FloatingActionButton>() {
                @SuppressLint("RestrictedApi")
                override fun layoutDependsOn(
                    parent: CoordinatorLayout,
                    child: FloatingActionButton,
                    dependency: View
                ): Boolean {
                    return dependency is Snackbar.SnackbarLayout
                }

                override fun onDependentViewChanged(
                    parent: CoordinatorLayout,
                    child: FloatingActionButton,
                    dependency: View
                ): Boolean {
                    // [^1]: snackbar is invisible when it attached to parent
                    // update fab position only when snackbar is visible
                    if (dependency.isVisible) {
                        child.translationY = min(0f, dependency.translationY - dependency.height)
                        return true
                    }
                    return false
                }

                override fun onDependentViewRemoved(
                    parent: CoordinatorLayout,
                    child: FloatingActionButton,
                    dependency: View
                ) {
                    child.translationY = 0f
                }
            }
        })
        doOnAttach { updateViewMargin() }
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, windowInsets ->
            updateViewMargin(windowInsets)
            windowInsets
        }
        updateFAB()
    }

}