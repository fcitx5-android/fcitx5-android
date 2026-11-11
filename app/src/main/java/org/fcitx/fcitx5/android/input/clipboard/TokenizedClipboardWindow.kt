/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.clipboard

import android.content.ClipData
import android.widget.Toast
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.input.FcitxInputMethodService
import org.fcitx.fcitx5.android.input.dependency.inputMethodService
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.fcitx.fcitx5.android.utils.clipboardManager
import org.mechdancer.dependency.manager.must

class TokenizedClipboardWindow(
    private val sourceText: String
) : InputWindow.SimpleInputWindow<TokenizedClipboardWindow>() {

    private val service: FcitxInputMethodService by manager.inputMethodService()
    private val theme: Theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val tokens by lazy { ClipboardTextTokenizer.tokenize(sourceText) }
    private val adapter by lazy {
        TokenizedClipboardAdapter(theme) { selectedCount, totalCount ->
            ui.updateSelectionState(selectedCount, totalCount)
        }
    }

    private lateinit var ui: TokenizedClipboardUi

    override fun onCreateView() = TokenizedClipboardUi(context, theme).apply {
        ui = this
        recyclerView.layoutManager = FlexboxLayoutManager(context).apply {
            flexDirection = FlexDirection.ROW
            flexWrap = FlexWrap.WRAP
            justifyContent = JustifyContent.FLEX_START
        }
        recyclerView.adapter = adapter
        backButton.setOnClickListener {
            windowManager.attachWindow(ClipboardWindow())
        }
        copyButton.setOnClickListener {
            val joined = currentSelectionText()
            if (joined.isBlank()) {
                Toast.makeText(context, R.string.tokenized_clipboard_empty_selection, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            context.clipboardManager.setPrimaryClip(ClipData.newPlainText("TokenizedClipboard", joined))
            Toast.makeText(context, R.string.tokenized_clipboard_copied, Toast.LENGTH_SHORT).show()
        }
        selectAllButton.setOnClickListener {
            adapter.toggleSelectAll()
        }
        invertSelectionButton.setOnClickListener {
            adapter.invertSelection()
        }
        clearSelectionButton.setOnClickListener {
            adapter.clearSelection()
        }
        sendButton.setOnClickListener {
            val joined = currentSelectionText()
            if (joined.isBlank()) {
                Toast.makeText(context, R.string.tokenized_clipboard_empty_selection, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            service.commitClipboardEntry(joined)
            adapter.clearSelection()
        }
    }.root

    override fun onAttached() {
        adapter.submitTokens(tokens)
        ui.setEmptyState(tokens.isEmpty())
    }

    override fun onDetached() = Unit

    private fun currentSelectionText(): String =
        ClipboardTextTokenizer.joinSelection(sourceText, adapter.selectedTokens())
}
