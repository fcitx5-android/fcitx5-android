package org.fcitx.fcitx5.android.utils

import cn.berberman.girls.utils.maybe.Maybe
import cn.berberman.girls.utils.maybe.then
import cn.berberman.girls.utils.maybe.toMaybe
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A transparent wrapper of serializer, which ignores failures on deserialization
 */
class MaybeSerializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<Maybe<T>> {
    override fun deserialize(decoder: Decoder): Maybe<T> =
        runCatching { serializer.deserialize(decoder) }.getOrNull().toMaybe()

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Maybe", serializer.descriptor)

    override fun serialize(encoder: Encoder, value: Maybe<T>) {
        value.then { serializer.serialize(encoder, it) }
    }
}