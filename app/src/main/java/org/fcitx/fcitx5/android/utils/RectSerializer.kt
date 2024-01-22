/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import android.graphics.Rect
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object RectSerializer : KSerializer<Rect> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Rect") {
        element<Int>("bottom")
        element<Int>("left")
        element<Int>("right")
        element<Int>("top")
    }

    override fun serialize(encoder: Encoder, value: Rect) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.bottom)
            encodeIntElement(descriptor, 1, value.left)
            encodeIntElement(descriptor, 2, value.right)
            encodeIntElement(descriptor, 3, value.top)
        }
    }

    override fun deserialize(decoder: Decoder): Rect = decoder.decodeStructure(descriptor) {
        var bottom = -1
        var left = -1
        var right = -1
        var top = -1
        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> bottom = decodeIntElement(descriptor, 0)
                1 -> left = decodeIntElement(descriptor, 1)
                2 -> right = decodeIntElement(descriptor, 2)
                3 -> top = decodeIntElement(descriptor, 3)
                CompositeDecoder.DECODE_DONE -> break
                else -> error("Unexpected index: $index")
            }
        }
        Rect(left, top, right, bottom)
    }

}