/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.candidates

import android.view.View
import org.fcitx.fcitx5.android.core.CandidateWord
import org.fcitx.fcitx5.android.core.FcitxEvent
import org.fcitx.fcitx5.android.daemon.FcitxConnection
import org.fcitx.fcitx5.android.daemon.launchOnReady
import org.fcitx.fcitx5.android.input.InputView

/**
 * A source of candidates shown in a candidate bar.
 *
 * The default implementation, [FcitxCandidateSource], backs the fcitx engine's
 * candidate list (click selects the candidate in the engine). Plugins providing
 * an interactive input panel may plug in their own source, e.g. one that commits
 * the tapped candidate text directly through the input session.
 */
interface CandidateSource {

    /** Current candidates */
    val candidates: List<CandidateWord>

    /**
     * Total number of candidates. May be larger than [candidates].size
     * when the list is paged.
     */
    val total: Int

    /** Called when the candidate at [idx] is clicked */
    fun onCandidateClick(idx: Int)

    /**
     * Called when the candidate at [idx] is long pressed.
     * Return `true` if the event was handled.
     */
    fun onCandidateLongClick(idx: Int, view: View): Boolean
}

/**
 * [CandidateSource] backed by the fcitx engine.
 * Clicking a candidate selects it in the engine; long pressing shows the
 * candidate action menu.
 */
class FcitxCandidateSource(
    private val fcitx: FcitxConnection,
    private val inputView: InputView
) : CandidateSource {

    private var data = FcitxEvent.CandidateListEvent.Data()

    override val candidates: List<CandidateWord>
        get() = data.candidates.toList()

    override val total: Int
        get() = data.total

    override fun onCandidateClick(idx: Int) {
        fcitx.launchOnReady { it.select(idx) }
    }

    override fun onCandidateLongClick(idx: Int, view: View): Boolean {
        val candidate = candidates.getOrNull(idx) ?: return false
        inputView.showCandidateActionMenu(idx, candidate.text, view)
        return true
    }

    /** Update the candidate list from a fcitx candidate event */
    fun update(data: FcitxEvent.CandidateListEvent.Data) {
        this.data = data
    }
}
