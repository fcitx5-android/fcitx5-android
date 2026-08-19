package org.fcitx.fcitx5.android.common.ipc;

oneway interface IFcitxPluginIpcCallback {
    /**
     * Plugin IPC "request" methods which need to responsd in callback.
     */
    void respond(int status, String msg, in @nullable byte[] payload);
}
