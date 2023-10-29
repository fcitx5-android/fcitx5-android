/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.content.res.Configuration
import android.os.Build

object Locales {

    lateinit var fcitxLocale: String
        private set

    lateinit var language: String
        private set

    lateinit var languageWithCountry: String
        private set

    fun onLocaleChange(configuration: Configuration) {
        fcitxLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales.let {
                buildString {
                    for (i in 0 until it.size()) {
                        if (i != 0) append(":")
                        append(it[i].run { "${language}_${country}:$language" })
                        // since there is not an `en.mo` file, `en` must be the only locale
                        // in order to use default english translation
                        if (i == 0 && it[i].language == "en") break
                    }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            configuration.locale.run { "${language}_${country}:$language" }
        }
        languageWithCountry = fcitxLocale.split(':')[0]
        language = languageWithCountry.split('_')[0]
    }

}