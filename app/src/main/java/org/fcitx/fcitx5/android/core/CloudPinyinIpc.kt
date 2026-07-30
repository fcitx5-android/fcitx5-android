/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.core

import android.os.RemoteException
import androidx.annotation.Keep
import org.fcitx.fcitx5.android.common.ipc.ICloudPinyinCallback
import org.fcitx.fcitx5.android.common.ipc.ICloudPinyinProvider
import timber.log.Timber

/**
 * The native addon lives in a plugin APK, while Binder endpoints must be owned
 * by the main process. This object is their narrow, request-only bridge.
 */
@Keep
object CloudPinyinIpc {

    @Volatile
    private var provider: ICloudPinyinProvider? = null

    fun register(provider: ICloudPinyinProvider) {
        this.provider = provider
    }

    fun unregister(provider: ICloudPinyinProvider) {
        if (this.provider?.asBinder() == provider.asBinder()) {
            this.provider = null
            nativeOnProviderUnavailable()
        }
    }

    fun loadNativeLibrary(nativeLibraryDir: String) {
        System.load("$nativeLibraryDir/libcloudpinyin.so")
    }

    @JvmStatic
    fun request(requestId: Long, pinyin: String, backend: String, proxy: String): Boolean {
        val currentProvider = provider ?: return false
        return try {
            currentProvider.request(requestId, pinyin, backend, proxy, callback)
            true
        } catch (e: RemoteException) {
            Timber.w(e, "Cloud pinyin provider request failed")
            if (provider?.asBinder() == currentProvider.asBinder()) {
                provider = null
            }
            false
        }
    }

    private val callback = object : ICloudPinyinCallback.Stub() {
        override fun onResponse(
            requestId: Long,
            httpStatus: Int,
            response: ByteArray?,
            error: String?
        ) {
            nativeOnResponse(requestId, httpStatus, response ?: ByteArray(0), error ?: "")
        }
    }

    @JvmStatic
    private external fun nativeOnResponse(
        requestId: Long,
        httpStatus: Int,
        response: ByteArray,
        error: String
    )

    @JvmStatic
    private external fun nativeOnProviderUnavailable()
}
