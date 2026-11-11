/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.data.clipboard

import androidx.annotation.StringRes
import org.fcitx.fcitx5.android.R

enum class ClipboardCategory(@StringRes val titleRes: Int) {
    All(R.string.clipboard_category_all),
    Favorites(R.string.clipboard_category_favorites),
    Local(R.string.clipboard_category_local),
    Media(R.string.clipboard_category_media),
    Remote(R.string.clipboard_category_remote)
}
