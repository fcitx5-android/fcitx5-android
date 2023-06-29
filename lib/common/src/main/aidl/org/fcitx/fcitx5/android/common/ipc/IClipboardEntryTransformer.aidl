package org.fcitx.fcitx5.android.common.ipc;

interface IClipboardEntryTransformer {
   int getPriority();
   String transform(String clipboardText);
   String getDescription();
}