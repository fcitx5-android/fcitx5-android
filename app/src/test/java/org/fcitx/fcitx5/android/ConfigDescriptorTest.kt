/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2026 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android

import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigDescriptorTest {

    @Test
    fun parsesRegexStringConstrain() {
        val descriptor = ConfigDescriptor.parse(
            RawConfig(
                "Regex",
                arrayOf(
                    RawConfig("Type", "String"),
                    RawConfig("IsRegex", "True")
                )
            )
        ).getOrNull() as ConfigDescriptor.ConfigString

        assertTrue(descriptor.isRegex)
    }

    @Test
    fun parsesRegexListConstrain() {
        val descriptor = ConfigDescriptor.parse(
            RawConfig(
                "RegexList",
                arrayOf(
                    RawConfig("Type", "List|String"),
                    RawConfig(
                        "ListConstrain",
                        arrayOf(RawConfig("IsRegex", "True"))
                    )
                )
            )
        ).getOrNull() as ConfigDescriptor.ConfigList

        assertTrue(descriptor.isRegex)
    }

    @Test
    fun plainStringsAreNotRegex() {
        val stringDescriptor = ConfigDescriptor.parse(
            RawConfig("String", arrayOf(RawConfig("Type", "String")))
        ).getOrNull() as ConfigDescriptor.ConfigString
        val listDescriptor = ConfigDescriptor.parse(
            RawConfig("StringList", arrayOf(RawConfig("Type", "List|String")))
        ).getOrNull() as ConfigDescriptor.ConfigList

        assertFalse(stringDescriptor.isRegex)
        assertFalse(listDescriptor.isRegex)
    }
}
