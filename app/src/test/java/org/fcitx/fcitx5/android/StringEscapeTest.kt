/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.core.FcitxUtils
import org.junit.Assert
import org.junit.Test

class StringEscapeTest {

    // https://github.com/fcitx/fcitx5/blob/5.1.8/test/teststringutils.cpp#L118
    private val data = listOf(
        "\"" to """"\""""",
        "\"\"\n" to """"\"\"\n"""",
        "abc" to """abc""",
        "ab\"c" to """"ab\"c"""",
        "a c" to """"a c""""
    )

    @Test
    fun testEscapeForValue() {
        data.forEach {
            Assert.assertEquals(FcitxUtils.escapeForValue(it.first), it.second)
        }
    }

    @Test
    fun testUnescapeForValue() {
        data.forEach {
            Assert.assertEquals(it.first, FcitxUtils.unescapeForValue(it.second))
        }
    }
}