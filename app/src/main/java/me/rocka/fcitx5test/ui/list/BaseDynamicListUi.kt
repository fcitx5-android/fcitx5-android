package me.rocka.fcitx5test.ui.list

import android.app.AlertDialog
import android.content.Context
import android.graphics.PorterDuff
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import cn.berberman.girls.utils.identity
import com.google.android.material.snackbar.Snackbar
import me.rocka.fcitx5test.R
import splitties.dimensions.dp
import splitties.resources.styledColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.coordinatorlayout.coordinatorLayout
import splitties.views.dsl.coordinatorlayout.defaultLParams
import splitties.views.dsl.core.*
import splitties.views.dsl.material.floatingActionButton
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityEndBottom
import splitties.views.recyclerview.verticalLayoutManager


abstract class BaseDynamicListUi<T>(
    override val ctx: Context,
    private val mode: Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean = true,
    initCheckBox: (CheckBox.(Int) -> Unit) = { visibility = View.GONE },
    initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE }
) : Ui,
    DynamicListAdapter<T>(
        initialEntries,
        enableOrder,
        mode !is Mode.Immutable,
        initCheckBox,
        {},
        initSettingsButton
    ) {

    private val fab = floatingActionButton {
        setImageResource(R.drawable.ic_baseline_plus_24)
        setColorFilter(styledColor(android.R.attr.colorForegroundInverse), PorterDuff.Mode.SRC_IN)
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
                    createTextAlertDialog(
                        context.getString(R.string.edit),
                        { setText(showEntry(entries[idx])) },
                        mode.validator
                    ) {
                        if (it != entries[idx])
                            updateItem(idx, mode.converter(it))
                    }
                }
            }
            is Mode.Immutable -> { _ -> visibility = View.GONE }
        }
        addOnItemChangedListener(object : OnItemChangedListener<T> {
            override fun onItemSwapped(fromIdx: Int, toIdx: Int, item: T) {
                updateFAB()
                Snackbar.make(
                    fab,
                    "${showEntry(entries[toIdx])} <-> ${showEntry(entries[fromIdx])}",
                    Snackbar.LENGTH_SHORT
                )
                    .setAction(R.string.undo) { swapItem(toIdx, fromIdx) }
                    .show()
            }

            override fun onItemAdded(idx: Int, item: T) {
                updateFAB()
                Snackbar.make(fab, "${showEntry(item)} added", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) { removeItem(idx) }
                    .show()
            }

            override fun onItemRemoved(idx: Int, item: T) {
                updateFAB()
                Snackbar.make(fab, "${showEntry(item)} removed", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) { addItem(idx, item) }
                    .show()
            }

            override fun onItemUpdated(idx: Int, old: T, new: T) {
                updateFAB()
                if (mode !is Mode.Immutable)
                    Snackbar.make(
                        fab,
                        "${showEntry(old)} -> ${showEntry(new)}",
                        Snackbar.LENGTH_SHORT
                    )
                        .setAction(R.string.undo) { updateItem(idx, old) }
                        .show()
            }

        })
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
                            .setItems(items) { _, which ->
                                addItem(item = candidatesSource[which])
                            }
                            .show()
                    }
                }
            }
            is Mode.FreeAdd -> {
                fab.show()
                fab.setOnClickListener {
                    createTextAlertDialog(
                        fab.context.getString(R.string.add),
                        { hint = mode.hint },
                        mode.validator
                    ) {
                        addItem(item = mode.converter(it))
                    }

                }
            }
            is Mode.Immutable -> fab.hide()
        }

    }

    private fun createTextAlertDialog(
        title: String,
        initEditText: EditText.() -> Unit = {},
        validator: (String) -> Boolean = { true },
        block: EditText.(String) -> Unit
    ) {
        val editText = editText(initView = initEditText)
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
            .setPositiveButton(R.string.ok) { _, _ ->
            }
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .show()
        editText.requestFocus()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val t = editText.editableText.toString()
            if (validator(t)) {
                block(editText, t)
                dialog.dismiss()
            } else
                editText.error = "Invalid text!"
        }

        editText.addTextChangedListener {
            editText.error = null
        }

    }


    override val root: View = coordinatorLayout {
        add(recyclerView {
            adapter = this@BaseDynamicListUi
            layoutManager = verticalLayoutManager()
            ItemTouchHelper(DynamicListTouchCallback(this@BaseDynamicListUi)).attachToRecyclerView(
                this
            )

        }, defaultLParams {
            height = matchParent
            width = matchParent
        })

        add(fab, defaultLParams {
            gravity = gravityEndBottom
            margin = dp(16)
        })
    }.also { updateFAB() }


}