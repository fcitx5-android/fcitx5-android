package org.fcitx.fcitx5.android.common.ipc;

interface IClipboardEntryTransformer {
   /** Transformers will be chained an applied to clipboard entry, where higher priority one goes first */
   int getPriority();
   /** The callback */
   String transform(String clipboardText);
   /** Unique description of this transformer */
   String getDescription();
}