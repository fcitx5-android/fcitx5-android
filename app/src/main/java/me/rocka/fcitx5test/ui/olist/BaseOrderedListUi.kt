package me.rocka.fcitx5test.ui.olist

import android.app.AlertDialog
import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.ItemTouchHelper
import cn.berberman.girls.utils.identity
import com.google.android.material.snackbar.Snackbar
import me.rocka.fcitx5test.R
import splitties.dimensions.dip
import splitties.resources.styledColor
import splitties.views.dsl.constraintlayout.*
import splitties.views.dsl.coordinatorlayout.coordinatorLayout
import splitties.views.dsl.coordinatorlayout.defaultLParams
import splitties.views.dsl.core.*
import splitties.views.dsl.material.floatingActionButton
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.gravityEndBottom
import splitties.views.recyclerview.verticalLayoutManager


abstract class BaseOrderedListUi<T>(
    override val ctx: Context,
    private val mode: Mode<T>,
    initialEntries: List<T>,
    enableOrder: Boolean,
    initSettingsButton: (ImageButton.(Int) -> Unit) = { visibility = View.GONE }
) : Ui,
    OrderedAdapter<T>(initialEntries, enableOrder, {}, initSettingsButton) {

    private val fab = floatingActionButton {
        setImageResource(R.drawable.ic_baseline_plus_24)
        setColorFilter(styledColor(android.R.attr.colorForegroundInverse), PorterDuff.Mode.SRC_IN)
    }


    sealed class Mode<T> {
        /**
         * Pick one from a list of [T]
         */
        data class ChooseOne<T>(val candidatesSource: BaseOrderedListUi<T>.() -> Array<T>) :
            Mode<T>()

        /**
         * Enter a string that can be converted into [T]
         */
        data class FreeAdd<T>(
            val hint: String,
            val converter: (String) -> T,
            val validator: (String) -> Boolean = { it.isNotBlank() }
        ) : Mode<T>()

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
        }
        addOnItemChangedListener(object : OnItemChangedListener<T> {
            override fun onItemSwapped(fromIdx: Int, toIdx: Int, item: T) {
                updateFAB()
                Snackbar.make(
                    fab,
                    "${showEntry(item)} swapped from #${fromIdx} -> #$toIdx}",
                    Snackbar.LENGTH_SHORT
                )
                    .setAction(R.string.undo) { swapItem(toIdx, fromIdx) }
                    .show()
                Log.d(
                    javaClass.name,
                    "${showEntry(item)} swapped from #${fromIdx} -> #$toIdx}, current: ${
                        entries.joinToString {
                            showEntry(
                                it
                            )
                        }
                    }"
                )
            }

            override fun onItemAdded(idx: Int, item: T) {
                updateFAB()
                Snackbar.make(fab, "${showEntry(item)} added", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) { removeItem(idx) }
                    .show()
                Log.d(
                    javaClass.name,
                    "${showEntry(item)} added, current: ${entries.joinToString { showEntry(it) }}"
                )
            }

            override fun onItemRemoved(idx: Int, item: T) {
                updateFAB()
                Snackbar.make(fab, "${showEntry(item)} removed", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) { addItem(idx, item) }
                    .show()
                Log.d(
                    javaClass.name,
                    "${showEntry(item)} removed, current: ${entries.joinToString { showEntry(it) }}"
                )
            }

            override fun onItemUpdated(idx: Int, old: T, new: T) {
                updateFAB()
                Snackbar.make(fab, "${showEntry(old)} -> ${showEntry(new)}", Snackbar.LENGTH_SHORT)
                    .setAction(R.string.undo) { updateItem(idx, old) }
                    .show()
                Log.d(
                    javaClass.name,
                    "${showEntry(old)} -> ${showEntry(new)}" +
                            ", current: ${entries.joinToString { showEntry(it) }}"
                )
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
                height = dip(64)
                width = matchParent
                topOfParent(16)
                bottomOfParent(16)
                leftOfParent(16)
                rightOfParent(16)
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
            adapter = this@BaseOrderedListUi
            layoutManager = verticalLayoutManager()
            ItemTouchHelper(OrderedTouchCallback(this@BaseOrderedListUi)).attachToRecyclerView(this)

        }, defaultLParams {
            height = matchParent
            width = matchParent
        })

        add(fab, defaultLParams {
            gravity = gravityEndBottom
            margin = dip(16)
        })
    }.also { updateFAB() }


}