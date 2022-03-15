package org.fcitx.fcitx5.android.utils

import cn.berberman.girls.utils.either.Either

interface MyParser<I, O, E> {
    fun parse(raw: I): Either<E, O>
}