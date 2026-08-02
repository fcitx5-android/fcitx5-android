/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.common.ipc;

import java.util.List;

/**
 * Interface implemented by the host (fcitx5-android), provided to interactive
 * input panel plugins.
 *
 * A {@link IInteractiveInputPanelHost} is valid only while the panel is attached
 * to an input session; the plugin must not hold it beyond the {@link IInteractiveInputPanel#onDetach}
 * call.
 */
interface IInteractiveInputPanelHost {
    /**
     * Publish a list of candidates to be shown in the host's candidate bar.
     * A later call replaces the previous list; an empty list clears the bar.
     */
    void setCandidates(in List<String> candidates);

    /**
     * Commit text through the current input session.
     */
    void commitText(String text);

    /**
     * Hide the input method window.
     */
    void requestHideSelf();
}
