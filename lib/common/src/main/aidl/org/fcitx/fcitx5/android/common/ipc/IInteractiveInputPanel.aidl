/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.common.ipc;

import android.view.Surface;
import org.fcitx.fcitx5.android.common.ipc.IInteractiveInputPanelHost;

/**
 * Interface implemented by interactive input panel plugins.
 *
 * The plugin is responsible for rendering the panel UI (e.g. a handwriting canvas)
 * on the {@link Surface} provided by the host, while the host is responsible for
 * showing candidates and committing text on behalf of the plugin.
 *
 * The panel service must be declared with action
 * `${mainApplicationId}.plugin.INTERACTIVE_PANEL`.
 */
interface IInteractiveInputPanel {
    /**
     * Called when a new input session starts.
     *
     * @param sessionId monotonically increasing ID of the input session,
     *                  used by the plugin to discard stale asynchronous results
     * @param host      the host interface, valid until {@link #onDetach}
     */
    void onAttach(int sessionId, IInteractiveInputPanelHost host);

    /**
     * Called when the current input session ends.
     */
    void onDetach();

    /**
     * The host created a {@link Surface} for the panel. The plugin should
     * start drawing the panel UI here.
     *
     * @param surface the drawing surface, valid until {@link #onSurfaceDestroyed}
     * @param width   width of the surface in pixels
     * @param height  height of the surface in pixels
     */
    void onSurfaceCreated(in Surface surface, int width, int height);

    /**
     * The {@link Surface} is destroyed. The plugin must stop drawing.
     */
    void onSurfaceDestroyed();

    /**
     * The panel size changed.
     */
    void onSizeChanged(int width, int height);

    /**
     * A new stroke begins at {@code (x, y)}.
     */
    void onTouchDown(float x, float y);

    /**
     * Batched stroke points, sent between {@link #onTouchDown} and {@link #onTouchUp}.
     * {@code xs} and {@code ys} must have the same length.
     */
    void onTouchMove(in float[] xs, in float[] ys);

    /**
     * The current stroke ends at {@code (x, y)}.
     */
    void onTouchUp(float x, float y);
}
