package org.fcitx.fcitx5.android.utils.config

import arrow.core.Either

interface ConfigParser<I, O, E> {
    fun parse(raw: I): Either<E, O>
}