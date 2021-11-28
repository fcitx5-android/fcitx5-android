package me.rocka.fcitx5test.settings.parsed

import cn.berberman.girls.utils.either.Either
import cn.berberman.girls.utils.either.runCatchingEither


sealed class ConfigType<T> {
    object TyInt : ConfigType<TyInt>()
    object TyString : ConfigType<TyString>()
    object TyBool : ConfigType<TyBool>()
    object TyKey : ConfigType<TyKey>()
    object TyEnum : ConfigType<TyEnum>()
    data class TyCustom(val typeName: String) : ConfigType<TyCustom>()
    data class TyList(val subtype: ConfigType<*>) : ConfigType<TyList>()
    companion object : MyParser<String, ConfigType<*>, Companion.UnknownConfigTypeException> {

        class UnknownConfigTypeException(val type: String) : Exception()

        override fun parse(raw: String): Either<UnknownConfigTypeException, ConfigType<*>> =
            parseE(raw).runCatchingEither { this }

        private fun parseE(raw: String): ConfigType<*> =
            when (raw) {
                "Integer" -> TyInt
                "String" -> TyString
                "Boolean" -> TyBool
                "Key" -> TyKey
                "Enum" -> TyEnum
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
        }
    }
}