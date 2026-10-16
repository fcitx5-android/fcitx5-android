/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.ui.main.playground

import android.graphics.Typeface
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.utils.applyNavBarInsetsBottomPadding
import org.fcitx.fcitx5.android.utils.inputMethodManager
import splitties.dimensions.dp
import splitties.resources.drawable
import splitties.resources.styledColorSL
import splitties.resources.styledDrawable
import splitties.views.dsl.constraintlayout.after
import splitties.views.dsl.constraintlayout.below
import splitties.views.dsl.constraintlayout.bottomOfParent
import splitties.views.dsl.constraintlayout.centerHorizontally
import splitties.views.dsl.constraintlayout.constraintLayout
import splitties.views.dsl.constraintlayout.endOfParent
import splitties.views.dsl.constraintlayout.lParams
import splitties.views.dsl.constraintlayout.matchConstraints
import splitties.views.dsl.constraintlayout.startOfParent
import splitties.views.dsl.constraintlayout.topOfParent
import splitties.views.dsl.core.add
import splitties.views.dsl.core.button
import splitties.views.dsl.core.imageButton
import splitties.views.dsl.core.textView
import splitties.views.dsl.core.wrapContent
import splitties.views.dsl.recyclerview.recyclerView
import splitties.views.imageDrawable
import splitties.views.recyclerview.verticalLayoutManager
import timber.log.Timber
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class EditTextPlaygroundFragment : Fragment() {
    companion object {
        private const val INPUT_TYPE = "inputType"

        /**
         * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r4/core/res/res/values/attrs.xml#1291
         */
        private val inputTypeMap = mapOf(
            "none" to 0x00000000,
            "text" to 0x00000001,
            "textCapCharacters" to 0x00001001,
            "textCapWords" to 0x00002001,
            "textCapSentences" to 0x00004001,
            "textAutoCorrect" to 0x00008001,
            "textAutoComplete" to 0x00010001,
            "textMultiLine" to 0x00020001,
            "textImeMultiLine" to 0x00040001,
            "textNoSuggestions" to 0x00080001,
            "textEnableTextConversionSuggestions" to 0x00100001,
            "textUri" to 0x00000011,
            "textEmailAddress" to 0x00000021,
            "textEmailSubject" to 0x00000031,
            "textShortMessage" to 0x00000041,
            "textLongMessage" to 0x00000051,
            "textPersonName" to 0x00000061,
            "textPostalAddress" to 0x00000071,
            "textPassword" to 0x00000081,
            "textVisiblePassword" to 0x00000091,
            "textWebEditText" to 0x000000a1,
            "textFilter" to 0x000000b1,
            "textPhonetic" to 0x000000c1,
            "textWebEmailAddress" to 0x000000d1,
            "textWebPassword" to 0x000000e1,
            "number" to 0x00000002,
            "numberSigned" to 0x00001002,
            "numberDecimal" to 0x00002002,
            "numberPassword" to 0x00000012,
            "phone" to 0x00000003,
            "datetime" to 0x00000004,
            "date" to 0x00000014,
            "time" to 0x00000024,
        )

        private const val IME_OPTIONS = "imeOptions"

        /**
         * https://android.googlesource.com/platform/frameworks/base/+/refs/tags/android-16.0.0_r4/core/res/res/values/attrs.xml#1440
         */
        private val imeOptionsMap = mapOf(
            "normal" to 0x00000000,
            "actionUnspecified" to 0x00000000,
            "actionNone" to 0x00000001,
            "actionGo" to 0x00000002,
            "actionSearch" to 0x00000003,
            "actionSend" to 0x00000004,
            "actionNext" to 0x00000005,
            "actionDone" to 0x00000006,
            "actionPrevious" to 0x00000007,
            "flagNoPersonalizedLearning" to 0x1000000,
            "flagNoFullscreen" to 0x2000000,
            "flagNavigatePrevious" to 0x4000000,
            "flagNavigateNext" to 0x8000000,
            "flagNoExtractUi" to 0x10000000,
            "flagNoAccessoryAction" to 0x20000000,
            "flagNoEnterAction" to 0x40000000,
            "flagForceAscii" to 0x80000000.toInt(),
        )
    }

    private lateinit var editText: EditText
    private lateinit var inputTypeButton: Button
    private lateinit var imeOptionsButton: Button
    private lateinit var clearButton: ImageButton
    private lateinit var recyclerView: RecyclerView

    // TODO: export logs
    private val logs = mutableListOf<String>()

    class LogViewHolder(val root: TextView) : RecyclerView.ViewHolder(root)

    private val logsAdapter by lazy {
        object : RecyclerView.Adapter<LogViewHolder>() {
            override fun getItemCount() = logs.size

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                LogViewHolder(requireContext().textView {
                    textSize = 12f
                    typeface = Typeface.MONOSPACE
                })

            override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
                holder.root.text = logs.getOrNull(position) ?: ""
            }
        }
    }

    private fun logText(text: String) {
        Timber.d(text)
        logs.add(text)
        logsAdapter.notifyItemInserted(logs.size - 1)
        recyclerView.post {
            recyclerView.smoothScrollToPosition(logs.size - 1)
        }
    }

    private fun clearLogs() {
        val size = logs.size
        logs.clear()
        logsAdapter.notifyItemRangeRemoved(0, size)
    }

    private var selectedInputType = listOf("text")
    private var selectedImeOptions = listOf("normal")

    private fun makeMultipleChoiceItems(
        map: Map<String, Int>,
        selected: List<String>
    ): Pair<Array<String>, BooleanArray> {
        val choices = map.keys.toTypedArray()
        val checked = choices.map { selected.contains(it) }.toBooleanArray()
        return choices to checked
    }

    private fun resolveMultipleChoiceResult(
        choices: Array<String>,
        selected: BooleanArray
    ): List<String> {
        return choices.filterIndexed { index, _ -> selected[index] }
    }

    private fun resolveFlags(map: Map<String, Int>, selected: List<String>): Int {
        var acc = 0
        selected.forEach { acc = acc or (map[it] ?: 0) }
        return acc
    }

    // TODO: imeActionId/imeActionLabel, minLines/maxLines, AllCaps, imeHintLocales, autofill ...
    private fun updateEditText() {
        fun List<String>.join() = joinToString("|")
        editText.inputType = resolveFlags(inputTypeMap, selectedInputType)
        editText.imeOptions = resolveFlags(imeOptionsMap, selectedImeOptions)
        logText("-".repeat(9))
        logText("updateEditText: inputType=${selectedInputType.join()}, imeOptions=${selectedImeOptions.join()}")
        requireContext().inputMethodManager.restartInput(editText)
    }

    private fun stringifyKeyEvent(e: KeyEvent): String {
        // formats like "KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_A, ... }"
        val eventString = e.toString()
        val additionalProps = ", displayLabel=${e.displayLabel}, scanCode=${e.scanCode}, " +
                "unicodeChar=${e.unicodeChar}, number=${e.number}"
        return eventString.replaceRange(eventString.length - 2, eventString.length, additionalProps)
    }

    private fun formatArgument(a: Any?): String {
        return when (a) {
            is String -> "\"$a\""
            is KeyEvent -> stringifyKeyEvent(a)
            is Int -> if (a < 0xff) a.toString() else "$a(0x${a.toString(16)})"
            else -> a.toString()
        }
    }

    private fun createInputConnectionProxy(ic: InputConnection): InputConnection {
        return Proxy.newProxyInstance(
            InputConnection::class.java.classLoader,
            arrayOf(InputConnection::class.java)
        ) handler@{ _, method: Method, args: Array<Any>? ->
            val argArray = args ?: emptyArray()
            logText("${method.name}(${argArray.joinToString(transform = ::formatArgument)})")
            return@handler method.invoke(ic, *argArray)
        } as InputConnection
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = requireContext().constraintLayout {
        editText = object : AppCompatEditText(context) {
            override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
                val ic = super.onCreateInputConnection(outAttrs)
                return if (ic == null) null else createInputConnectionProxy(ic)
            }
        }
        add(editText, lParams(matchConstraints, wrapContent) {
            topOfParent()
            centerHorizontally()
        })
        // TODO: add tabs for ime properties and logs
        inputTypeButton = button {
            text = INPUT_TYPE
            isAllCaps = false
            setOnClickListener {
                val (choices, checked) = makeMultipleChoiceItems(inputTypeMap, selectedInputType)
                AlertDialog.Builder(requireContext())
                    .setTitle(INPUT_TYPE)
                    .setMultiChoiceItems(choices, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        selectedInputType = resolveMultipleChoiceResult(choices, checked)
                        updateEditText()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        add(inputTypeButton, lParams(wrapContent, dp(40)) {
            below(editText)
            startOfParent()
        })
        imeOptionsButton = button {
            text = IME_OPTIONS
            isAllCaps = false
            setOnClickListener {
                val (choices, checked) = makeMultipleChoiceItems(imeOptionsMap, selectedImeOptions)
                AlertDialog.Builder(requireContext())
                    .setTitle(IME_OPTIONS)
                    .setMultiChoiceItems(choices, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        selectedImeOptions = resolveMultipleChoiceResult(choices, checked)
                        updateEditText()
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
        add(imeOptionsButton, lParams(wrapContent, dp(40)) {
            below(editText)
            after(inputTypeButton)
        })
        clearButton = imageButton {
            imageDrawable = drawable(R.drawable.ic_baseline_delete_sweep_24)
            imageTintList = styledColorSL(android.R.attr.colorControlNormal)
            background = styledDrawable(android.R.attr.actionBarItemBackground)
            setOnClickListener { clearLogs() }
        }
        add(clearButton, lParams(dp(40), dp(40)) {
            below(editText)
            endOfParent()
        })
        recyclerView = recyclerView {
            layoutManager = verticalLayoutManager()
            adapter = logsAdapter
        }
        recyclerView.applyNavBarInsetsBottomPadding()
        add(recyclerView, lParams {
            below(clearButton)
            centerHorizontally()
            bottomOfParent()
        })
    }

    override fun onStart() {
        super.onStart()
        updateEditText()
    }
}
