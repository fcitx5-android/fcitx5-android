package org.fcitx.fcitx5.android.common.ipc;

import org.fcitx.fcitx5.android.common.ipc.IFcitxPluginIpcCallback;

interface IFcitxPluginService {
    /**
     * Unique identifier for plugin service. Also used in Plugin IPC.
     */
    String getPluginId();

    /**
     * `transformClipboardEntry` callbacks will be chained and applied to clipboard entry, where higher priority one goes first.
     * Return `-1` if you don't have one.
     */
    int getClipboardEntryTransformerPriority();
    /**
     * The callback to transform clipboard text. Return `null` if you don't have one.
     */
    String transformClipboardEntry(String clipboardText);

    /**
     * Return `true` to enable Plugin IPC.
     */
    boolean getCanHandleIpc();
    /**
     * Plugin IPC "notify" methods which does not have any return value.
     */
    oneway void onIpcNotify(String method, in @nullable byte[] params);
    /**
     * Plugin IPC "request" methods which need to responsd in callback.
     */
    oneway void onIpcRequest(String method, in @nullable byte[] params, IFcitxPluginIpcCallback cb);
}
