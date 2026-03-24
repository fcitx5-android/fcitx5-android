/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2025 Fcitx5 for Android Contributors
 */

package org.fcitx.fcitx5.android.data.quickphrase

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import java.io.File

object QuickPhraseSerializer : KSerializer<QuickPhrase> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("quickphrase") {
        element<String>("path")
        element<Boolean>("isBuiltin")
        element<String?>("override")
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(
        encoder: Encoder,
        value: QuickPhrase
    ) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.file.absolutePath)
        encodeBooleanElement(descriptor, 1, value is BuiltinQuickPhrase)
        encodeNullableSerializableElement(
            descriptor,
            2,
            String.serializer(),
            value.let { it as? BuiltinQuickPhrase }?.overrideFilePath
        )

    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): QuickPhrase =
        decoder.decodeStructure(descriptor) {
            var path: String? = null
            var isBuiltin = false
            var overridePath: String? = null

            while (true) {
                when (decodeElementIndex(descriptor)) {
                    0 -> path = decodeStringElement(descriptor, 0)
                    1 -> isBuiltin = decodeBooleanElement(descriptor, 1)
                    2 -> overridePath = decodeNullableSerializableElement(
                        descriptor, 2, String.serializer()
                    )
                    else -> break
                }
            }

            val file = File(path ?: throw IllegalStateException("Path cannot be null"))
            if (isBuiltin) {
                BuiltinQuickPhrase(
                    file,
                    File(
                        overridePath ?: throw IllegalStateException("Override path cannot be null")
                    )
                )
            } else {
                CustomQuickPhrase(file)
            }
        }
}
