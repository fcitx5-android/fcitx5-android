/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement

/**
 * Add information to the decoding result
 */
class NostalgicSerializer<T, U>(
    private val serializer: KSerializer<T>,
    private val f: (JsonElement) -> U
) : KSerializer<Pair<T, U>> {
    override fun deserialize(decoder: Decoder): Pair<T, U> {
        val json = (decoder as JsonDecoder).decodeJsonElement()
        return Json.decodeFromJsonElement(serializer, json) to f(json)
    }

    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun serialize(encoder: Encoder, value: Pair<T, U>) {
        serializer.serialize(encoder, value.first)
    }
}