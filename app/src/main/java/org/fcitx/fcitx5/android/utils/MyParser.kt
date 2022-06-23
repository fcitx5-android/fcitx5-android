package org.fcitx.fcitx5.android.utils

import arrow.core.Either

interface MyParser<I, O, E> {
    fun parse(raw: I): Either<E, O>
}