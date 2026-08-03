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
 *
 * <h2>Required components</h2>
 *
 * An interactive input panel is a keyboard-like input surface. The plugin must
 * render the following components in its own UI and invoke the corresponding
 * host methods when they are pressed:
 *
 * <ul>
 *   <li>"?123" (layout switch): draw it and call {@link #switchToSymbolLayout()}.</li>
 *   <li>language key (input method switch): draw it and call
 *       {@link #switchInputMethod()} on press and {@link #showInputMethodPicker()}
 *       on long press.</li>
 * </ul>
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

    /**
     * Close this panel and show the standard keyboard.
     * The plugin should provide a "back to keyboard" affordance in its UI.
     */
    void showKeyboard();

    /**
     * Switch to the last-used symbol layout, like pressing the "?123" key on
     * the standard keyboard. Closes this panel.
     */
    void switchToSymbolLayout();

    /**
     * Switch the input method according to the user's language-key preference,
     * like pressing the language key on the standard keyboard.
     */
    void switchInputMethod();

    /**
     * Show the input method picker dialog, like long-pressing the language key.
     */
    void showInputMethodPicker();
}
