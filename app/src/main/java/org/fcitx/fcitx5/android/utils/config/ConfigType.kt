/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2023 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils.config

import android.os.Parcelable
import arrow.core.Either
import arrow.core.raise.either
import kotlinx.parcelize.Parcelize

sealed class ConfigType<T> : Parcelable {
    @Parcelize
    data object TyInt : ConfigType<TyInt>()

    @Parcelize
    data object TyString : ConfigType<TyString>()

    @Parcelize
    data object TyBool : ConfigType<TyBool>()

    @Parcelize
    data object TyKey : ConfigType<TyKey>()

    @Parcelize
    data object TyEnum : ConfigType<TyEnum>()

    @Parcelize
    data object TyExternal : ConfigType<TyExternal>()

    @Parcelize
    data class TyCustom(val typeName: String) : ConfigType<TyCustom>()

    @Parcelize
    data class TyList(val subtype: ConfigType<*>) : ConfigType<TyList>()

    companion object : ConfigParser<String, ConfigType<*>, Companion.UnknownConfigTypeException> {

        data class UnknownConfigTypeException(val type: String) : Exception()

        override fun parse(raw: String): Either<UnknownConfigTypeException, ConfigType<*>> =
            either {
                when (raw) {
                    "Integer" -> TyInt
                    "String" -> TyString
                    "Boolean" -> TyBool
                    "Key" -> TyKey
                    "Enum" -> TyEnum
                    "External" -> TyExternal
                    else -> {
                        when {
                            raw.startsWith("List|") -> parse(raw.drop(5)).map(::TyList).bind()
                            raw.contains("$") -> TyCustom(raw)
                            else -> raise(UnknownConfigTypeException(raw))
                        }
                    }
                }
            }

        fun pretty(raw: ConfigType<*>): String = when (raw) {
            TyBool -> "Boolean"
            is TyCustom -> raw.typeName
            TyEnum -> "Enum"
            TyInt -> "Integer"
            TyKey -> "Key"
            is TyList -> "List|${pretty(raw.subtype)}"
            TyString -> "String"
            TyExternal -> "External"
        }
    }
}