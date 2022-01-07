package me.rocka.fcitx5test.utils

import cn.berberman.girls.utils.either.Either

interface MyParser<I, O, E> {
    fun parse(raw: I): Either<E, O>
}