/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2023 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.utils

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import org.fcitx.fcitx5.android.BuildConfig

inline fun <reified T : Parcelable> Intent.parcelable(key: String): T? {
    // https://issuetracker.google.com/issues/240585930#comment6
    @Suppress("DEPRECATION")
    return getParcelableExtra(key) as? T
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        getParcelableExtra(key, T::class.java)
//    } else {
//        @Suppress("DEPRECATION")
//        getParcelableExtra(key) as? T
//    }
}

inline fun <reified T : Parcelable> Intent.parcelableArray(key: String): Array<T>? {
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    return getParcelableArrayExtra(key) as? Array<T>
//    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//        getParcelableArrayExtra(key, T::class.java)
//    } else {
//        @Suppress("DEPRECATION", "UNCHECKED_CAST")
//        getParcelableArrayExtra(key) as? Array<T>
//    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun buildPrimaryStorageIntent(path: String = ""): Intent {
    val initialUri = appContext.storageManager.primaryStorageVolume.createOpenDocumentTreeIntent()
        .parcelable<Uri>(DocumentsContract.EXTRA_INITIAL_URI)!!
    val uri = Uri.Builder()
        .scheme(initialUri.scheme)
        .authority(initialUri.authority)
        .encodedPath(
            initialUri.path!!.replaceFirst("/root/", "/document/") +
                    Uri.encode(":Android/data/${BuildConfig.APPLICATION_ID}/files/$path")
        ).build()
    return Intent(Intent.ACTION_VIEW, uri)
}

fun buildDocumentsProviderIntent(): Intent {
    val uri = DocumentsContract.buildRootUri("${BuildConfig.APPLICATION_ID}.provider", "files")
    return Intent(Intent.ACTION_VIEW, uri)
}
