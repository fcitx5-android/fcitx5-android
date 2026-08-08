package org.fcitx.fcitx5.android.common.ipc;

import org.fcitx.fcitx5.android.common.ipc.ICloudPinyinCallback;

interface ICloudPinyinProvider {
   void request(long requestId, String pinyin, String backend, String proxy,
                ICloudPinyinCallback callback);
}
