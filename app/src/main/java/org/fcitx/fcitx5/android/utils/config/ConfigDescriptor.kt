/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.utils.config

import android.os.Parcelable
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.fcitx.fcitx5.android.core.RawConfig
import org.fcitx.fcitx5.android.utils.config.ConfigDescriptor.Companion.parse

sealed class ConfigDescriptor<T, U> : Parcelable {
    abstract val raw: RawConfig
    abstract val name: String
    abstract val type: ConfigType<T>
    abstract val description: String?
    abstract val defaultValue: U?
    abstract val tooltip: String?

    @Parcelize
    data class ConfigTopLevelDef(
        val name: String,
        val values: List<ConfigDescriptor<*, *>>,
        val customTypes: List<ConfigCustomTypeDef>
    ) : Parcelable

    @Parcelize
    data class ConfigCustomTypeDef(
        val name: String,
        val values: List<ConfigDescriptor<*, *>>
    ) : Parcelable

    @Parcelize
    data class ConfigInt(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val defaultValue: Int? = null,
        override val tooltip: String? = null,
        val intMax: Int?,
        val intMin: Int?,
    ) : ConfigDescriptor<ConfigType.TyInt, Int>() {
        override val type: ConfigType<ConfigType.TyInt>
            get() = ConfigType.TyInt
    }

    @Parcelize
    data class ConfigString(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val defaultValue: String? = null,
        override val tooltip: String? = null,
    ) : ConfigDescriptor<ConfigType.TyString, String>() {
        override val type: ConfigType<ConfigType.TyString>
            get() = ConfigType.TyString
    }

    @Parcelize
    data class ConfigBool(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val defaultValue: Boolean? = null,
        override val tooltip: String? = null,
    ) : ConfigDescriptor<ConfigType.TyBool, Boolean>() {
        override val type: ConfigType<ConfigType.TyBool>
            get() = ConfigType.TyBool

    }

    @Parcelize
    data class ConfigKey(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val defaultValue: String? = null,
        override val tooltip: String? = null,
    ) : ConfigDescriptor<ConfigType.TyKey, String>() {
        override val type: ConfigType<ConfigType.TyKey>
            get() = ConfigType.TyKey

    }

    @Parcelize
    data class ConfigEnum(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val defaultValue: String? = null,
        override val tooltip: String? = null,
        val entries: List<String>,
        val entriesI18n: List<String>?
    ) : ConfigDescriptor<ConfigType.TyEnum, String>() {
        override val type: ConfigType<ConfigType.TyEnum>
            get() = ConfigType.TyEnum

    }

    @Parcelize
    data class ConfigCustom(
        override val raw: RawConfig,
        override val name: String,
        override val type: ConfigType.TyCustom,
        override val description: String? = null,
        override val tooltip: String? = null,
        // will be filled in parseTopLevel
        var customTypeDef: ConfigCustomTypeDef? = null
    ) : ConfigDescriptor<ConfigType.TyCustom, Nothing>() {
        override val defaultValue: Nothing?
            get() = null
    }

    @Parcelize
    data class ConfigList(
        override val raw: RawConfig,
        override val name: String,
        override val type: ConfigType.TyList,
        override val description: String? = null,
        override val tooltip: String? = null,
        /**
         * [Any?] is used for a union type. See [parse] for details.
         */
        override val defaultValue: @RawValue List<Any?>? = null,
    ) : ConfigDescriptor<ConfigType.TyList, List<Any?>>()

    /**
     * Specialized [ConfigList] for enum
     */
    @Parcelize
    data class ConfigEnumList(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val defaultValue: List<String>? = null,
        override val tooltip: String? = null,
        val entries: List<String>,
        val entriesI18n: List<String>?
    ) :
        ConfigDescriptor<ConfigType.TyList, List<String>>() {
        override val type: ConfigType<ConfigType.TyList>
            get() = ConfigType.TyList(ConfigType.TyEnum)
    }

    @Parcelize
    data class ConfigExternal(
        override val raw: RawConfig,
        override val name: String,
        override val description: String? = null,
        override val tooltip: String? = null,
        val uri: String? = null,
        val knownType: ETy? = null
    ) : ConfigDescriptor<ConfigType.TyExternal, Nothing>() {
        enum class ETy {
            PinyinDict,
            Punctuation,
            QuickPhrase,
            Chttrans,
            TableGlobal,
            PinyinCustomPhrase,
            RimeUserDataDir,

            // manually added on Android side for TableManager
            AndroidTable
        }

        override val type: ConfigType<ConfigType.TyExternal>
            get() = ConfigType.TyExternal
        override val defaultValue: Nothing?
            get() = null
    }

    companion object :
        ConfigParser<RawConfig, ConfigDescriptor<*, *>, Companion.ParseException> {

        private val RawConfig.type: Either<ConfigType.Companion.UnknownConfigTypeException, ConfigType<*>>?
            get() {
                val type = findByName("Type")?.value
                if (type == "String" && findByName("IsEnum")?.value == "True") {
                    return Either.Right(ConfigType.TyEnum)
                }
                return type?.let { ConfigType.parse(it) }
            }
        private val RawConfig.description
            get() = findByName("Description")?.value
        private val RawConfig.defaultValue
            get() = findByName("DefaultValue")?.value
        private val RawConfig.enum
            get() = findByName("Enum")?.subItems?.map { it.value }
        private val RawConfig.enumI18n
            get() = findByName("EnumI18n")?.subItems?.map { it.value }
        private val RawConfig.intMin
            get() = findByName("IntMin")?.value?.toInt()
        private val RawConfig.intMax
            get() = findByName("IntMax")?.value?.toInt()
        private val RawConfig.tooltip
            get() = findByName("Tooltip")?.value

        sealed class ParseException : Exception() {
            data class NoTypeExist(val config: RawConfig) : ParseException()
            data class TypeNoParse(val sup: ConfigType.Companion.UnknownConfigTypeException) :
                ParseException()

            data class NoEnumFound(val config: RawConfig) : ParseException()
            data class BadFormList(val type: ConfigType<*>) : ParseException()
            data class BadFormDesc(val config: RawConfig) : ParseException()
        }

        override fun parse(raw: RawConfig): Either<ParseException, ConfigDescriptor<*, *>> =
            ((raw.type?.mapLeft { ParseException.TypeNoParse(it) })
                ?: (Either.Left(ParseException.NoTypeExist(raw)))).flatMap {
                either {
                    when (it) {
                        ConfigType.TyBool ->
                            ConfigBool(
                                raw,
                                raw.name,
                                raw.description,
                                raw.defaultValue?.toBoolean()
                            )
                        is ConfigType.TyCustom -> ConfigCustom(
                            raw,
                            raw.name,
                            it,
                            raw.description
                        )
                        ConfigType.TyEnum -> {
                            val entries = raw.enum ?: raise(ParseException.NoEnumFound(raw))
                            ConfigEnum(
                                raw,
                                raw.name,
                                raw.description,
                                raw.defaultValue,
                                raw.tooltip,
                                entries,
                                raw.enumI18n
                            )
                        }
                        ConfigType.TyInt -> ConfigInt(
                            raw,
                            raw.name,
                            raw.description,
                            raw.defaultValue?.toInt(),
                            raw.tooltip,
                            raw.intMax,
                            raw.intMin
                        )
                        ConfigType.TyKey -> ConfigKey(
                            raw,
                            raw.name,
                            raw.description,
                            raw.defaultValue
                        )
                        is ConfigType.TyList ->
                            if (it.subtype == ConfigType.TyEnum) {
                                val entries = raw.enum ?: raise(ParseException.NoEnumFound(raw))
                                ConfigEnumList(
                                    raw,
                                    raw.name,
                                    raw.description,
                                    raw.findByName("DefaultValue")?.subItems?.map { ele -> ele.value },
                                    raw.tooltip,
                                    entries,
                                    raw.enumI18n
                                )
                            } else
                                ConfigList(
                                    raw,
                                    raw.name,
                                    it,
                                    raw.description,
                                    raw.tooltip,
                                    raw.findByName("DefaultValue")?.subItems?.map { ele ->
                                        when (it.subtype) {
                                            ConfigType.TyBool -> ele.value.toBoolean()
                                            ConfigType.TyInt -> ele.value.toInt()
                                            ConfigType.TyKey -> ele.value
                                            ConfigType.TyString -> ele.value
                                            ConfigType.TyEnum -> error("Impossible!")
                                            else -> raise(ParseException.BadFormList(it))
                                        }
                                    }
                                )
                        ConfigType.TyString -> ConfigString(
                            raw,
                            raw.name,
                            raw.description,
                            raw.defaultValue
                        )
                        ConfigType.TyExternal -> ConfigExternal(
                            raw,
                            raw.name,
                            raw.description,
                            raw.tooltip,
                            raw.findByName("External")?.value,
                            when (raw.name) {
                                "DictManager" -> ConfigExternal.ETy.PinyinDict
                                "Punctuation" -> ConfigExternal.ETy.Punctuation
                                "QuickPhrase", "Editor" -> ConfigExternal.ETy.QuickPhrase
                                "Chttrans" -> ConfigExternal.ETy.Chttrans
                                "TableGlobal" -> ConfigExternal.ETy.TableGlobal
                                "CustomPhrase" -> ConfigExternal.ETy.PinyinCustomPhrase
                                "UserDataDir" -> ConfigExternal.ETy.RimeUserDataDir
                                "AndroidTable" -> ConfigExternal.ETy.AndroidTable
                                else -> null
                            }
                        )
                    }
                }

            }


        fun parseTopLevel(raw: RawConfig): Either<ParseException, ConfigTopLevelDef> =
            either {
                val topLevel = raw.subItems?.get(0) ?: raise(ParseException.BadFormDesc(raw))
                val customTypeDef = raw.subItems?.drop(1)?.mapNotNull {
                    it.subItems?.map { ele -> parse(ele).bind() }
                        ?.let { parsed -> ConfigCustomTypeDef(it.name, parsed) }
                } ?: listOf()
                val topDesc = topLevel.subItems?.map {
                    val parsed = parse(it).bind()
                    if (parsed is ConfigCustom)
                        parsed.customTypeDef = customTypeDef.find { cTy ->
                            cTy.name == parsed.type.typeName
                        }
                    parsed
                } ?: listOf()
                ConfigTopLevelDef(topLevel.name, topDesc, customTypeDef)
            }
    }

}