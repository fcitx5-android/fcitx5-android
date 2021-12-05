package me.rocka.fcitx5test.settings.parsed

import cn.berberman.girls.utils.either.Either
import cn.berberman.girls.utils.either.runCatchingEither


sealed class ConfigType<T> {
    object TyInt : ConfigType<TyInt>() {
        override fun toString(): String = javaClass.simpleName
    }

    object TyString : ConfigType<TyString>() {
        override fun toString(): String = javaClass.simpleName
    }

    object TyBool : ConfigType<TyBool>() {
        override fun toString(): String = javaClass.simpleName
    }

    object TyKey : ConfigType<TyKey>() {
        override fun toString(): String = javaClass.simpleName
    }

    object TyEnum : ConfigType<TyEnum>() {
        override fun toString(): String = javaClass.simpleName
    }

    object TyExternal : ConfigType<TyExternal>() {
        override fun toString(): String = javaClass.simpleName
    }

    data class TyCustom(val typeName: String) : ConfigType<TyCustom>()
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