package org.fcitx.fcitx5.android.utils.config

import android.os.Parcelable
import cn.berberman.girls.utils.either.Either
import cn.berberman.girls.utils.either.runCatchingEither
import kotlinx.parcelize.Parcelize
import org.fcitx.fcitx5.android.utils.MyParser

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

    companion object : MyParser<String, ConfigType<*>, Companion.UnknownConfigTypeException> {

        data class UnknownConfigTypeException(val type: String) : Exception()

        override fun parse(raw: String): Either<UnknownConfigTypeException, ConfigType<*>> =
            runCatchingEither {
                parseE(raw)
            }

        private fun parseE(raw: String): ConfigType<*> =
            when (raw) {
                "Integer" -> TyInt
                "String" -> TyString
                "Boolean" -> TyBool
                "Key" -> TyKey
                "Enum" -> TyEnum
                "External" -> TyExternal
                else -> {
                    when {
                        raw.startsWith("List|") -> TyList(parseE(raw.drop(5)))
                        raw.contains("$") -> TyCustom(raw)
                        else -> throw UnknownConfigTypeException(raw)
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