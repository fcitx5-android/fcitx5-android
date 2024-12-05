/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2024 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.plugin.clipboard_filter

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object RegexSerializer : KSerializer<Regex> {
    override val descriptor = PrimitiveSerialDescriptor("RegexSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Regex) = encoder.encodeString(value.toString())
    override fun deserialize(decoder: Decoder) = Regex(decoder.decodeString())
}
