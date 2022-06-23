package org.fcitx.fcitx5.android.utils

import arrow.core.Option
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A transparent wrapper of serializer, which ignores failures on deserialization
 */
class OptionSerializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<Option<T>> {
    override fun deserialize(decoder: Decoder): Option<T> =
        Option.catch { serializer.deserialize(decoder) }

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Option", serializer.descriptor)

    override fun serialize(encoder: Encoder, value: Option<T>) {
        value.fold({}) {
            serializer.serialize(encoder, it)
        }
    }
}