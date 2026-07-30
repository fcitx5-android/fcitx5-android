package org.fcitx.fcitx5.android.common.ipc;

interface ICloudPinyinCallback {
   void onResponse(long requestId, int httpStatus, in byte[] response, String error);
}
