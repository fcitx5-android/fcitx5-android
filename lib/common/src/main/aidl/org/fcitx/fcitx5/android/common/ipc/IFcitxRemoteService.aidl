package org.fcitx.fcitx5.android.common.ipc;

import org.fcitx.fcitx5.android.common.ipc.IClipboardEntryTransformer;
import org.fcitx.fcitx5.android.common.ipc.ICloudPinyinProvider;

interface IFcitxRemoteService {
   /** Get the version name of fcitx app */
   String getVersionName();
   /** Get the process ID of fcitx app */
   int getPid();
   /** Get loaded plugins in fcitx app */
   Map<String, String> getLoadedPlugins();

   /** Request fcitx daemon to restart fcitx */
   void restartFcitx();

   /** Register a clipboard transformer to fcitx app */
   void registerClipboardEntryTransformer(IClipboardEntryTransformer transformer);
   /** Unregister a clipboard transformer to fcitx app */
   void unregisterClipboardEntryTransformer(IClipboardEntryTransformer transformer);

   /** Register the sole cloud pinyin network provider. */
   void registerCloudPinyinProvider(ICloudPinyinProvider provider);
   /** Unregister the cloud pinyin network provider. */
   void unregisterCloudPinyinProvider(ICloudPinyinProvider provider);

   /** Reload fcitx pinyin dictionary */
   void reloadPinyinDict();
   /** Reload fcitx quick phrase */
   void reloadQuickPhrase();
}