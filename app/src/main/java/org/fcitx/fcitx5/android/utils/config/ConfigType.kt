package org.fcitx.fcitx5.android.utils.config

import android.os.Parcelable
import arrow.core.Either
import arrow.core.continuations.either
import kotlinx.parcelize.Parcelize

sealed class ConfigType<T> : Parcelable {
    @Parcelize
    object TyInt : ConfigType<TyInt>() {
        override fun toString(): String = javaClass.simpleName
    }

    @Parcelize
    object TyString : ConfigType<TyString>() {
        override fun toString(): String = javaClass.simpleName
    }

    @Parcelize
    object TyBool : ConfigType<TyBool>() {
        override fun toString(): String = javaClass.simpleName
    }

    @Parcelize
    object TyKey : ConfigType<TyKey>() {
        override fun toString(): String = javaClass.simpleName
    }

    @Parcelize
    object TyEnum : ConfigType<TyEnum>() {
        override fun toString(): String = javaClass.simpleName
    }

    @Parcelize
    object TyExternal : ConfigType<TyExternal>() {
        override fun toString(): String = javaClass.simpleName
    }

    @Parcelize
    data class TyCustom(val typeName: String) : ConfigType<TyCustom>()

    @Parcelize
    data class TyList(val subtype: ConfigType<*>) : ConfigType<TyList>()

    companion object : ConfigParser<String, ConfigType<*>, Companion.UnknownConfigTypeException> {

        data class UnknownConfigTypeException(val type: String) : Exception()

        override fun parse(raw: String): Either<UnknownConfigTypeException, ConfigType<*>> =
            either.eager {
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
                            else -> shift(UnknownConfigTypeException(raw))
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